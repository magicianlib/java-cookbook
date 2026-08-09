package io.ituknown.redis.config;

import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.annotation.EnableRateLimit;
import io.ituknown.redis.aspect.RateLimitAspect;
import io.ituknown.redis.support.SpelKeyResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EnableRateLimitTest.AppConfig.class)
class EnableRateLimitTest {

    @Autowired
    RateLimiter rateLimiter;
    @Autowired
    SpelKeyResolver spelKeyResolver;
    @Autowired
    RateLimitAspect rateLimitAspect;

    @Test
    void enable_registers_all_beans() {
        assertNotNull(rateLimiter);
        assertNotNull(spelKeyResolver);
        assertNotNull(rateLimitAspect);
    }

    @Test
    void enable_applies_configured_order() {
        // @EnableRateLimit(order = 42) 经 @Import + ImportAware 透传到切面
        assertEquals(42, rateLimitAspect.getOrder());
    }

    @Configuration
    @EnableRateLimit(order = 42)
    static class AppConfig {

        @Bean(destroyMethod = "shutdown")
        RedissonClient redissonClient() {
            // 惰性客户端，构造期不连接；测试不触发限流调用，故离线可跑
            return Redisson.create();
        }
    }
}
