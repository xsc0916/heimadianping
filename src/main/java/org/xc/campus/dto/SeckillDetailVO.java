package org.xc.campus.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀详情：code（随机码）是下单的"门票"，前端带着它调下单接口
 */
@Data
public class SeckillDetailVO {

    private Long seckillId;
    private Long productId;
    private String productName;
    private BigDecimal seckillPrice;
    private Integer stock;
    private String code;
}
