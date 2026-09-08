package org.xc.campus.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功返回：token 给前端存着，之后每个请求带 Authorization: Bearer {token}
 */
@Data
@Builder
public class LoginVO {

    private String token;
    private UserDTO user;
}
