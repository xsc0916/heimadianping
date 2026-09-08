package org.xc.campus.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户轻量对象：存 Redis 会话、写 ThreadLocal，不携带手机号明文之外的大字段
 */
@Data
public class UserDTO implements Serializable {

    private Long id;
    private String nickName;
    private String icon;
    private Integer role;
}
