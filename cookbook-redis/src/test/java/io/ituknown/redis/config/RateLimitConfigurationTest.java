package io.ituknown.redis.config;

import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.annotation.EnableRateLimit;
import io.ituknown.redis.aspect.RateLimitAspect;
import io.ituknown.redis.support.SpelKeyResolver;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 限流装配的顺序透传单测：验证 {@link EnableRateLimit#order()} 经 ImportAware 注入到切面。
 *
 * <p>直接驱动 {@link RateLimitConfiguration}，不启动 Spring 容器、不连接 Redis，故可离线运行；
 * {@link EnableRateLimitTest} 走完整容器装配，需本地 Dragonfly 才能拉起 RedissonClient。
 */
class RateLimitConfigurationTest {

    @Test
    void default_order_is_lowest_precedence() {
        RateLimitConfiguration cfg = newConfig(DefaultMarker.class);
        RateLimitAspect aspect = cfg.rateLimitAspect(new RateLimiter(null), new SpelKeyResolver());
        assertEquals(Ordered.LOWEST_PRECEDENCE, aspect.getOrder());
    }

    @Test
    void configured_order_is_applied() {
        RateLimitConfiguration cfg = newConfig(Order42Marker.class);
        RateLimitAspect aspect = cfg.rateLimitAspect(new RateLimiter(null), new SpelKeyResolver());
        assertEquals(42, aspect.getOrder());
    }

    private static RateLimitConfiguration newConfig(Class<?> importingClass) {
        RateLimitConfiguration cfg = new RateLimitConfiguration();
        cfg.setImportMetadata(AnnotationMetadata.introspect(importingClass));
        return cfg;
    }

    @EnableRateLimit
    static class DefaultMarker {
    }

    @EnableRateLimit(order = 42)
    static class Order42Marker {
    }
}
