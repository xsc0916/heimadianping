package org.xc.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.xc.campus.dto.LoginCodeDTO;
import org.xc.campus.dto.LoginVO;
import org.xc.campus.dto.UserDTO;
import org.xc.campus.entity.User;

public interface UserService extends IService<User> {

    /** 发送验证码，mock 模式下返回验证码本身便于联调 */
    String sendCode(String phone);

    /** 验证码登录，新用户自动注册 */
    LoginVO login(LoginCodeDTO dto);

    /** 当前登录用户 */
    UserDTO me();

    /** 修改昵称/头像（传 null 的字段不更新） */
    void updateProfile(String nickName, String icon);

    /** 登出：删除 Redis 会话，Token 立即失效 */
    void logout();
}
