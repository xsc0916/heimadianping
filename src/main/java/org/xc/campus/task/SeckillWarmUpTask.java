package org.xc.campus.task;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.xc.campus.constant.RedisKeyConstant;
import org.xc.campus.entity.SeckillProduct;
import org.xc.campus.mapper.SeckillProductMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀预热任务（文档 6.1"Redis 库存预热"）：
 * 活动开始前把 DB 库存写进 Redis seckill:stock:{id}，并生成随机码 seckill:code:{id}。
 * 没有 Redis 库存，Lua 第一行 exists 就不通过，所有下单都会返回"活动不存在"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillWarmUpTask {

    private final SeckillProductMapper seckillProductMapper;
    private final StringRedisTemplate redis;

    /** 应用启动立即预热一次（开发/演示友好，不用等定时点） */
    @PostConstruct
    public void init() {
        warmUp();
    }

    /** 每分钟扫一遍：新增活动自动被预热 */
    @Scheduled(cron = "0 * * * * ?")
    public void scheduledWarmUp() {
        warmUp();
    }

    public void warmUp() {
        List<SeckillProduct> list = seckillProductMapper.selectList(
                new LambdaQueryWrapper<SeckillProduct>()
                        .gt(SeckillProduct::getEndTime, LocalDateTime.now())); // 未结束的活动都要有库存key
        for (SeckillProduct sp : list) {
            // ⚠️ 必须用 setIfAbsent（不存在才写），绝不能用 set 覆盖！
            // 反例：Redis 已被 Lua 扣到 98，DB 还是 100，覆盖一次库存"回血"2 件 → 超卖！
            redis.opsForValue().setIfAbsent(RedisKeyConstant.SECKILL_STOCK + sp.getId(),
                    String.valueOf(sp.getStock()), Duration.ofHours(24));
            // 随机码已发放的不能换（否则拿到旧码的用户全部"非法请求"），只补缺
            if (Boolean.FALSE.equals(redis.hasKey(RedisKeyConstant.SECKILL_CODE + sp.getId()))) {
                redis.opsForValue().set(RedisKeyConstant.SECKILL_CODE + sp.getId(),
                        RandomUtil.randomString(6), Duration.ofHours(24));
            }
        }
        log.debug("秒杀预热完成，覆盖活动数={}", list.size());
    }
}
