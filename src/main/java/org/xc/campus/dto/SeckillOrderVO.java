package org.xc.campus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 下单接口响应：status=0 排队中，前端拿 orderNo 去轮询 /api/seckill/result/{orderNo}
 */
@Data
@AllArgsConstructor
public class SeckillOrderVO {

    private String orderNo;
    private Integer status;
}
