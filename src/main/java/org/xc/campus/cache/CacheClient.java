package org.xc.campus.cache;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.xc.campus.constant.RedisKeyConstant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存工具：封装"逻辑过期查询"与"逻辑过期写入"。
 * dbFallback 是一个 Function<Long,T>——把"怎么查数据库"作为参数传进来（Java8 函数式思想），
 * 工具类就不用耦合任何具体业务，商品/预约/未来任何表都能复用。
 *
 * 逻辑过期重建采用【异步重建】：命中过期数据后把"抢锁→查库→写回→解锁"整个甩给线程池，
 * 当前请求立即返回旧数据 —— 所有请求零阻塞。锁的加/解都发生在工作线程内部。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheClient {

    private final StringRedisTemplate redis;
    private final RedissonClient redisson;

    /**
     * 缓存重建专用线程池。
     * 为什么手动 new 而不用 Executors：阿里 Java 规范禁止 Executors —— 有界队列防内存溢出、线程命名便于排查。
     * 参数怎么定的：重建是低频 IO 任务（每次逻辑过期才触发一次），核心 2 / 上限 4 足够，
     * 队列 100 缓冲同一时刻多个 key 同时过期的突发。
     * CallerRunsPolicy 的妙用：池彻底满时由"提交任务的主线程"自己执行 —— 天然降级回同步重建，不丢任务。
     */
    private static final ThreadPoolExecutor REBUILD_POOL = new ThreadPoolExecutor(
            2, 4, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            ThreadFactoryBuilder.create().setNamePrefix("cache-rebuild-").build(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /** 应用关闭时优雅停机：不再接新任务，已提交的任务跑完，避免非守护线程卡住 JVM 退出 */
    @PreDestroy
    public void shutdownPool() {
        REBUILD_POOL.shutdown();
    }

    /**
     * 逻辑过期写入：key 不设 TTL，过期信息写在 value 里
     */
    public void writeWithLogicalExpire(String key, Object data, Duration logicalExpire) {
        RedisData rd = new RedisData();
        rd.setData(data);
        rd.setExpireTime(LocalDateTime.now().plus(logicalExpire));
        redis.opsForValue().set(key, JSONUtil.toJsonStr(rd));
    }

    /**
     * 逻辑过期查询主流程（异步重建版，防击穿核心）：
     * 返回 null = Redis 里没有这个 key（冷数据），由调用方决定查库回填。
     */
    public <T> T queryWithLogicalExpire(String keyPrefix, Long id, Class<T> type,
                                        Function<Long, T> dbFallback, Duration logicalExpire) {
        String key = keyPrefix + id;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return null; // ① Redis 没有 → 冷数据，交给调用方
        }
        // ② 反序列化：外壳 RedisData + 内部业务对象
        RedisData shell = JSONUtil.toBean(json, RedisData.class);
        T data = JSONUtil.toBean((JSONObject) shell.getData(), type);
        if (shell.getExpireTime().isAfter(LocalDateTime.now())) {
            return data; // ③ 还没逻辑过期 → 新鲜数据，直接返回（99.9% 的请求走这里）
        }

        // ④ 已逻辑过期 → 整个"抢锁+查库+写回+解锁"提交给线程池，本请求【立即】返回旧数据。
        //    为什么锁不在主线程抢：Redisson RLock 要求加锁/解锁在同一个线程（按线程记录持有者），
        //    主线程 tryLock、工作线程 unlock 会抛 IllegalMonitorStateException —— 必须整体下沉。
        REBUILD_POOL.submit(() -> {
            RLock lock = redisson.getLock(RedisKeyConstant.LOCK_PRODUCT + id);
            boolean locked = lock.tryLock(); // 不等待：抢不到说明已有重建任务在跑，放弃即可
            if (!locked) {
                return;
            }
            try {
                T fresh = dbFallback.apply(id); // 查库重建（在工作线程执行，主请求早已返回）
                if (fresh == null) {
                    redis.delete(key); // 商品已下架/不存在：删 key，避免永远返回旧数据
                    return;
                }
                writeWithLogicalExpire(key, fresh, logicalExpire); // 写回新数据+新过期时间
            } catch (Exception e) {
                // 异步线程的异常没人接住会无声无息消失，必须自己兜住记日志
                log.error("缓存重建失败 key={}", key, e);
            } finally {
                lock.unlock(); // 加锁/解锁在同一线程（工作线程），满足 Redisson 的要求
            }
        });
        // ⑤ 请求方拿到的是"上一版"数据 —— 用短暂不新鲜换永久不阻塞，逻辑过期方案的核心取舍
        return data;
    }
}
