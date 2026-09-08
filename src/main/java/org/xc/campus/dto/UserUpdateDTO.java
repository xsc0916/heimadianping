package org.xc.campus.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人信息入参：两个字段都可只传一个
 */
@Data
public class UserUpdateDTO {

    @Size(max = 64, message = "昵称最长64字符")
    private String nickName;

    @Size(max = 255, message = "头像URL过长")
    private String icon;
}
