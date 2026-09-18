package org.xc.campus.annotation;

import org.xc.campus.enums.LimitDimension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解：哪个接口要限流、限多少、按什么维度限，加在 Controller 方法上即可。
 * 限流逻辑在 RateLimitAspect 里统一实现 —— 业务代码零侵入。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内最大请求数 */
    int limit() default 10;

    /** 窗口时长（秒） */
    int windowSeconds() default 60;

    /** 限流维度：按用户 / 按IP / 按接口 */
    LimitDimension dimension() default LimitDimension.USER;
}
