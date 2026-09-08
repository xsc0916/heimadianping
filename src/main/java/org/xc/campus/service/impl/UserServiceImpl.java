package org.xc.campus.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.xc.campus.constant.RedisKeyConstant;
import org.xc.campus.dto.LoginCodeDTO;
import org.xc.campus.dto.LoginVO;
import org.xc.campus.dto.UserDTO;
import org.xc.campus.entity.User;
import org.xc.campus.exception.BizException;
import org.xc.campus.mapper.UserMapper;
import org.xc.campus.service.UserService;
import org.xc.campus.utils.JwtUtil;
import org.xc.campus.utils.UserHolder;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor // Lombok 生成 final 字段的构造器，Spring 自动注入（比 @Autowired 更推荐）
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate redis;

    /** 是否 mock 短信：true 时不真正发短信，直接把验证码返回给前端方便联调 */
    @Value("${campus.sms.mock:true}")
    private boolean smsMock;

    @Value("${campus.jwt.expire}")
    private long jwtExpireSeconds;

    @Override
    public String sendCode(String phone) {
        // 1. 60 秒防重发：setIfAbsent = "不存在才写入"（原子操作），已有标记说明发过
        Boolean ok = redis.opsForValue().setIfAbsent(
                RedisKeyConstant.LOGIN_CODE_INTERVAL + phone, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(ok)) {
            throw new BizException(400, "发送过于频繁，请60秒后再试");
        }
        // 2. 生成6位纯数字验证码，存 Redis，5分钟自动过期（TTL = 天然的失效时间）
        String code = RandomUtil.randomNumbers(6);
        redis.opsForValue().set(RedisKeyConstant.LOGIN_CODE + phone, code, Duration.ofMinutes(5));
        log.info("发送验证码 phone={} code={}", phone, code);
        // 3. 生产环境这里对接短信服务商；mock 模式直接返回验证码联调
        return smsMock ? code : null;
    }

    @Override
    public LoginVO login(LoginCodeDTO dto) {
        // 1. 从 Redis 取验证码比对 —— 过期/不存在/填错都是同一个错误，避免暴露细节
        String cached = redis.opsForValue().get(RedisKeyConstant.LOGIN_CODE + dto.getPhone());
        if (cached == null || !cached.equals(dto.getCode())) {
            throw new BizException(1001, "验证码错误或已过期");
        }
        // 2. 验证成功立即删除，防止同一验证码反复使用（重放攻击）
        redis.delete(RedisKeyConstant.LOGIN_CODE + dto.getPhone());

        // 3. 按手机号查用户，不存在自动注册
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            user = new User();
            user.setPhone(dto.getPhone());
            user.setNickName("文创er" + RandomUtil.randomNumbers(4));
            user.setRole(0);
            save(user); // MyBatis-Plus 提供的插入
        }

        // 4. 签发 JWT（通行证），并把会话写 Redis：JWT 管"是谁"，Redis 管"能不能继续玩"
        String token = JwtUtil.createToken(user.getId());
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
        userDTO.setIcon(user.getIcon());
        userDTO.setRole(user.getRole());
        redis.opsForValue().set(RedisKeyConstant.LOGIN_TOKEN + user.getId(),
                JSONUtil.toJsonStr(userDTO), Duration.ofSeconds(jwtExpireSeconds));

        return LoginVO.builder().token(token).user(userDTO).build();
    }

    @Override
    public UserDTO me() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BizException(401, "未登录");
        }
        return user;
    }

    @Override
    public void updateProfile(String nickName, String icon) {
        Long userId = UserHolder.getUserId();
        lambdaUpdate() // 只更新传了值的字段，null 字段跳过
                .eq(User::getId, userId)
                .set(nickName != null, User::getNickName, nickName)
                .set(icon != null, User::getIcon, icon)
                .update();
        // 同步刷新 Redis 里的会话，让 me() 立刻看到新昵称
        User user = getById(userId);
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
        userDTO.setIcon(user.getIcon());
        userDTO.setRole(user.getRole());
        redis.opsForValue().set(RedisKeyConstant.LOGIN_TOKEN + userId,
                JSONUtil.toJsonStr(userDTO), Duration.ofSeconds(jwtExpireSeconds));
    }

    @Override
    public void logout() {
        // 删掉 Redis 会话 = 这个用户立刻被踢下线（这就是为什么不能只用纯 JWT）
        redis.delete(RedisKeyConstant.LOGIN_TOKEN + UserHolder.getUserId());
    }
}
