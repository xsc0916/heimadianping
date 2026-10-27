package org.xc.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀下单入参：seckillId + 随机码。
 * 随机码的作用：详情接口才发放，直接构造 URL 疯狂下单的脚本拿不到它（第一道门槛）
 */
@Data
public class SeckillOrderDTO {

    @NotNull(message = "秒杀活动ID不能为空")
    private Long seckillId;

    @NotBlank(message = "秒杀随机码不能为空")
    private String code;
}
