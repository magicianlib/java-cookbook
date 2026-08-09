package io.ituknown.redis.annotation;

import io.ituknown.redis.config.RateLimitConfiguration;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启声明式限流功能
 * <p>
 * 开启后就能在方法上用 {@link RateLimit} 注解配置请求频率。</p>
 * <p>
 * 使用前要先在容器里注册 {@link RedissonClient} Bean；没有的话，启动会直接报错。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RateLimitConfiguration.class)
public @interface EnableRateLimit {

    /**
     * 配置切面执行顺序，数值越小优先级越高，默认使用 {@link Ordered#HIGHEST_PRECEDENCE}
     * <p>
     * 限流通常应尽量靠前，以便在打开事务、访问数据等耗资源操作之前就拒绝越界请求，可按需调整该值。</p>
     */
    int order() default Ordered.HIGHEST_PRECEDENCE;
}