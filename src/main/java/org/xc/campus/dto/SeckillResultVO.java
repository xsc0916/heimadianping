package org.xc.campus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 轮询结果（存 Redis seckill:order:{orderNo}，查询纯走 Redis 不打 DB）：
 * status: 0 排队中(前端1s后再轮询) 1 成功(带orderId) 2 失败(msg说明原因)
 * userId 用于校验"结果只能本人查询"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResultVO {

    private String orderNo;
    private Long userId;
    private Integer status;
    private Long orderId;
    private String msg;
}
