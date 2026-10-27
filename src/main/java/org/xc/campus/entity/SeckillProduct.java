package org.xc.campus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体，对应 seckill_product 表。
 * 一个商品可参加多期活动：product 1—N seckill_product
 */
@Data
@TableName("seckill_product")
public class SeckillProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private BigDecimal seckillPrice;

    /** 秒杀库存（与商品普通库存分开） */
    private Integer stock;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    /** 0未开始 1进行中 2已结束 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
