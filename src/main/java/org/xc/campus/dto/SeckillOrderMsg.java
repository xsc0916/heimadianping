package org.xc.campus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 消息体：下单接口受理成功后发给消费者。
 * 只带最小必要字段（productId/价格由消费者查库为准，避免消息里的快照过期）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMsg {

    private String orderNo;
    private Long userId;
    private Long seckillId;
}
