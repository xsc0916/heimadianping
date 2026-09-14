package org.xc.campus.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.xc.campus.constant.RedisKeyConstant;
import org.xc.campus.dto.UserDTO;
import org.xc.campus.utils.JwtUtil;
import org.xc.campus.utils.UserHolder;

import java.time.Duration;

/**
 * 拦截器①：所有请求的"第一站"。
 * 只负责认识你（解析token→查会话→续期→装进口袋），不负责拦人 —— 一律 return true。
 * 注意：不能用构造器注入（会被 WebMvcConfig 手动 new），所以用 @Component + 字段注入。
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;

    @Value("${campus.jwt.expire}")
    private long jwtExpireSeconds;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头取 token，格式：Authorization: Bearer eyJhbGci...
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            return true; // 没带 token：可能是游客，放行，由②号拦截器决定拦不拦
        }
        // 2. 验票：解析出 userId（伪造/过期的 token 返回 null）
        Long userId = JwtUtil.parseToken(token.replace("Bearer ", ""));
        if (userId == null) {
            return true; // 票是假的：同样放行，②号会因为没有用户而拦下敏感请求
        }
        // 3. JWT 合法 ≠ 还能玩：真正的会话在 Redis，被踢下线/登出后这个 key 就没了
        String key = RedisKeyConstant.LOGIN_TOKEN + userId;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return true; // 会话不存在（已登出/过期）：放行，②号兜底
        }
        // 4. 一切正常：把用户装进口袋 + 滑动续期（你一直活跃就一直不掉线）
        UserHolder.saveUser(JSONUtil.toBean(json, UserDTO.class));
        redis.expire(key, Duration.ofSeconds(jwtExpireSeconds));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 5. 请求结束必须清口袋！Tomcat 的线程是复用的，
        //    不清的话下一个请求可能读到上一个人的身份（数据串号，事故级别）
        UserHolder.remove();
    }
}
