package org.xc.campus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文创商品实体，对应 product 表。
 * 金额用 BigDecimal —— double 有精度丢失（0.1+0.2!=0.3），钱的事绝不能用 double
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String subTitle;

    private String cover;

    /** 轮播图，逗号分隔 */
    private String images;

    /** 详情富文本 */
    private String detail;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 普通库存 */
    private Integer stock;

    private Integer sales;

    /** 1上架 0下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
