package org.xc.campus.utils;

import lombok.RequiredArgsConstructor;
import org.xc.campus.constant.RedisKeyConstant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 分布式唯一 ID（订单号）：符号位 1bit + 时间戳 31bit(秒) + 序列号 32bit(Redis 自增)。
 * 相比雪花算法无时钟回拨问题；序列号按天自增，同秒内靠 INCR 原子递增区分。
 */
@Component
@RequiredArgsConstructor
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP = 1640995200L; // 基准时间 2022-01-01 00:00:00
    private static final int COUNT_BITS = 32;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy:MM:dd");

    private final StringRedisTemplate redis;

    public long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        // 1. 时间戳：相对基准时间的秒数，左移 32 位落在 bit32~bit62，bit63 自动为符号位 0
        long timestamp = now.toEpochSecond(ZoneOffset.of("+8")) - BEGIN_TIMESTAMP;
        // 2. 序列号：Redis 按天自增，原子、唯一、趋势递增
        String date = now.format(DATE_FMT);
        long count = redis.opsForValue().increment(RedisKeyConstant.ID_WORKER + keyPrefix + ":" + date);
        // 3. 拼接
        return timestamp << COUNT_BITS | count;
    }
}
