package org.xc.campus.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态机，对应文档 6.6：
 * 0 待支付 → 1 已支付 → 2 已发货 → 3 已完成；0 → 4 已取消(用户) / 5 已关闭(超时)
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    UNPAID(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELED(4, "已取消"),
    CLOSED(5, "已关闭");

    private final int code;
    private final String desc;
}
