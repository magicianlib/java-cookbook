package io.ituknown.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.ituknown.redis.config.RateLimitConfiguration;

import org.springframework.context.annotation.Import;

/**
 * 开启声明式限流：导入限流装配，注册切面并启用方法代理。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RateLimitConfiguration.class)
public @interface EnableRateLimit {
}
