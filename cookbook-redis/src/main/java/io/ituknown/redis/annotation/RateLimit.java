package io.ituknown.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级限流。配额按周期计算，突发上限默认取配额数。
 * 表达式非空时按方法入参区分限流维度；为空时仅按方法签名限流。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key() default "";

    int count();

    int period() default 1;

    int maxBurst() default -1;

    int quantity() default 1;
}
