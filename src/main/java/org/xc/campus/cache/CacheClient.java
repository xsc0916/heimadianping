package org.xc.campus.cache;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.xc.campus.constant.RedisKeyConstant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * 缓存工具：封装"逻辑过期查询"与"逻辑过期写入"。
 * dbFallback 是一个 Function<Long,T>——把"怎么查数据库"作为参数传进来（Java8 函数式思想），
 * 工具类就不用耦合任何具体业务，商品/预约/未来任何表都能复用。
 */
@Component
@RequiredArgsConstructor
public class CacheClient {

    private final StringRedisTemplate redis;
    private final RedissonClient redisson;

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
     * 逻辑过期查询主流程（防击穿核心，对照讲解的读路径图看）：
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

        // ④ 已逻辑过期 → 尝试拿 Redisson 锁：tryLock() 不等待，抢不到立刻走人
        RLock lock = redisson.getLock(RedisKeyConstant.LOCK_PRODUCT + id);
        boolean locked = lock.tryLock();
        try {
            if (!locked) {
                return data; // 没抢到锁 → 别的线程正在重建，我返回旧数据，不等待不穿透
            }
            // 拿到锁 → 查库重建
            T fresh = dbFallback.apply(id);
            if (fresh == null) {
                redis.delete(key); // 商品已下架/不存在：删 key，避免永远返回旧数据
                return null;
            }
            writeWithLogicalExpire(key, fresh, logicalExpire); // 写回新数据+新过期时间
            return fresh;
        } finally {
            if (locked) {
                lock.unlock(); // Redisson 锁必须手动释放（tryLock 成功才 unlock）
            }
        }
        /*public void writeWithLogicalExpire(String key, Object data, Duration logicalExpire) {
            RedisData rd = new RedisData();
            rd.setData(data);
            rd.setExpireTime(LocalDateTime.now().plus(logicalExpire));
            redis.opsForValue().set(key, JSONUtil.toJsonStr(rd));
        }*/

    }
}
