package org.xc.campus.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀列表项：活动信息 + 商品摘要（前端秒杀专场页）
 */
@Data
public class SeckillListVO {

    private Long id;
    private Long productId;
    private String productName;
    private String cover;
    private BigDecimal seckillPrice;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    /** 0未开始 1进行中 */
    private Integer status;
}
