package org.xc.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 请求参数 DTO。@NotBlank/@Pattern 是校验注解，
 * Controller 参数上加 @Valid 后，不符合会自动被全局异常处理器拦下返回 400。
 */
@Data
public class LoginCodeDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    /** 登录时必填；发验证码时不需要（发验证码只有 phone） */
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    private String code;
}
