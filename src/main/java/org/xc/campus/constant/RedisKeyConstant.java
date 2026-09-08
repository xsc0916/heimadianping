package org.xc.campus.constant;

/**
 * Redis Key 规范，对应文档 4.3
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
    }

    /** 短信验证码 login:code:{phone} TTL 5min */
    public static final String LOGIN_CODE = "login:code:";
    /** 验证码 60s 发送间隔标记 */
    public static final String LOGIN_CODE_INTERVAL = "login:code:interval:";
    /** 登录会话 login:token:{userId} TTL 30min 滑动续期 */
    public static final String LOGIN_TOKEN = "login:token:";
    /** 商品详情逻辑过期缓存 cache:product:{id} */
    public static final String CACHE_PRODUCT = "cache:product:";
    /** 分类列表缓存 */
    public static final String CACHE_CATEGORY = "cache:category";
    /** 秒杀库存 seckill:stock:{seckillId} */
    public static final String SECKILL_STOCK = "seckill:stock:";
    /** 秒杀已购用户 seckill:buyers:{seckillId} Set */
    public static final String SECKILL_BUYERS = "seckill:buyers:";
    /** 秒杀随机码 seckill:code:{seckillId} */
    public static final String SECKILL_CODE = "seckill:code:";
    /** 秒杀结果 seckill:order:{orderNo} 0排队 1成功 2失败 */
    public static final String SECKILL_ORDER = "seckill:order:";
    /** AI 会话上下文 ai:session:{userId}:{sessionId} Hash */
    public static final String AI_SESSION = "ai:session:";
    /** 滑动窗口限流 rate:limit:{dimension}:{key} ZSet */
    public static final String RATE_LIMIT = "rate:limit:";
    /** 缓存重建锁 lock:product:{id} Redisson RLock */
    public static final String LOCK_PRODUCT = "lock:product:";
    /** RedisIdWorker 序列号前缀 */
    public static final String ID_WORKER = "icr:";
}
