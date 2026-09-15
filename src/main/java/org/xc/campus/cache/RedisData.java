package org.xc.campus.cache;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 逻辑过期包装：Redis 里存的是 { data: 业务数据, expireTime: 逻辑过期时间 }。
 * key 本身不设 TTL（物理永不过期），是否"过期"由读的一方比较 expireTime 自行判断 —— 防击穿的核心。
 */
@Data
public class RedisData {

    private Object data;

    private LocalDateTime expireTime;
}
