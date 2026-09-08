package org.xc.campus.constant;

/**
 * Kafka Topic 约定，对应文档 7.3
 */
public final class MqTopic {

    private MqTopic() {
    }

    /** 秒杀下单 */
    public static final String SECKILL_ORDER = "seckill.order";
    /** 缓存删除失败重试 */
    public static final String CACHE_DELETE_RETRY = "cache.delete.retry";
}
