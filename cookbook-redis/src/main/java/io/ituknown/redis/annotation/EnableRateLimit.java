package io.ituknown.redis.annotation;

import io.ituknown.redis.config.RateLimitConfiguration;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启声明式限流功能
 * <p>
 * 开启后就能在方法上使用 {@link RateLimit} 注解配置请求频率。</p>
 * <p>
 * 开启该功能的前提是在 Spring 中注册 Bean {@link RedissonClient}，如果没有注册将无法使用。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RateLimitConfiguration.class)
public @interface EnableRateLimit {
}
