package org.xc.campus.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xc.campus.entity.Product;

import java.time.Duration;

/**
 * Caffeine 一级缓存（进程内存）。
 * 短物理 TTL(10秒)：本地缓存允许"最多旧10秒"，到期自动逐出，再从 Redis 拉新 ——
 * 两层缓存的新鲜度靠这 10 秒窗口兜底，同时把同机重复请求全挡在内存里。
 */
@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<Long, Product> productCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(10)) // 写入10秒后自动逐出
                .maximumSize(10_000)                      // 最多缓存1万个商品，防内存爆
                .build();
    }
}
