package org.xc.campus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，对应 orders 表。秒杀单与普通单共用，seckill_flag 区分。
 * version 是乐观锁字段，第 6 课"支付 vs 超时关单"的竞争靠它。
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long productId;

    /** 1秒杀单 0普通单 */
    private Integer seckillFlag;

    private Long seckillId;

    private Integer count;

    private BigDecimal amount;

    private Integer payType;

    private LocalDateTime payTime;

    /** 0待支付 1已支付 2已发货 3已完成 4已取消 5已关闭 */
    private Integer status;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
