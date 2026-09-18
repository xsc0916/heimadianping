package org.xc.campus.aspect;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.xc.campus.annotation.RateLimit;
import org.xc.campus.constant.RedisKeyConstant;
import org.xc.campus.enums.LimitDimension;
import org.xc.campus.exception.BizException;
import org.xc.campus.utils.UserHolder;

import java.util.List;
import java.util.UUID;

/**
 * 滑动窗口限流切面（对应文档 6.5）。
 * @Around 环绕通知 = 在目标方法前后都插手：先判断放不放行，放行才 proceed() 执行业务。
 * 注意写法：@annotation(rateLimit) 会把方法上的 @RateLimit 注解对象直接注入参数 —— 不用反射自己取。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redis;

    /**
     * 滑动窗口 Lua：三步合一，Redis 单线程执行保证原子性。
     * KEYS[1] = 限流key   ARGV[1]=当前毫秒  ARGV[2]=窗口起点毫秒  ARGV[3]=上限
     * ARGV[4]=本次请求唯一member   ARGV[5]=key存活毫秒(窗口时长+1s缓冲，闲置后自动清理)
     * 返回 1=放行 0=拒绝
     */
    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>("""
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, tonumber(ARGV[2]))
            local count = redis.call('ZCARD', KEYS[1])
            if count >= tonumber(ARGV[3]) then
                return 0
            end
            redis.call('ZADD', KEYS[1], tonumber(ARGV[1]), ARGV[4])
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[5]))
            return 1
            """, Long.class);

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        // identity 自带类型前缀（user:1 / ip:x.x.x.x / api），key 里不再重复拼维度枚举名
        String identity = switch (rateLimit.dimension()) {
            case USER -> {
                Long userId = UserHolder.getUserId();
                // 公开接口游客没有 userId：退化为按 IP 限，避免所有游客共享一个"游客桶"被误伤
                yield userId != null ? "user:" + userId : "ip:" + currentIp();
            }
            case IP -> "ip:" + currentIp();
            case API -> "api";
        };
        // 接口名拼进 key：同一个用户调 A 接口和 B 接口分开计数
        String method = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        String zKey = RedisKeyConstant.RATE_LIMIT + identity + ":" + method;

        long now = System.currentTimeMillis();
        long winStart = now - rateLimit.windowSeconds() * 1000L;

        long expireMs = rateLimit.windowSeconds() * 1000L + 1000; // 多留1秒缓冲，窗口过后 key 自动消失
        Long allowed = redis.execute(SLIDING_WINDOW_SCRIPT, List.of(zKey),
                String.valueOf(now), String.valueOf(winStart),
                String.valueOf(rateLimit.limit()), UUID.randomUUID().toString(),
                String.valueOf(expireMs));

        if (allowed == null || allowed == 0) {
            throw new BizException(429, "请求过于频繁，请稍后再试");
        }
        return pjp.proceed(); // 放行 → 执行真正的业务方法
    }

    /** 拿客户端真实 IP：有 Nginx 等反代时 getRemoteAddr 只会拿到代理 IP，需优先取 X-Forwarded-For 头 */
    private String currentIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String xff = attrs.getRequest().getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(xff)) {
            return xff.split(",")[0].trim(); // 多级代理时第一个才是真实客户端 IP
        }
        return attrs.getRequest().getRemoteAddr();
    }
}
