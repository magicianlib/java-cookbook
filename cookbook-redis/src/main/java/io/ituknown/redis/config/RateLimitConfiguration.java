package io.ituknown.redis.config;

import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.aspect.RateLimitAspect;
import io.ituknown.redis.support.SpelKeyResolver;

import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 限流装配：注册执行器、维度解析器、切面，并开启方法代理。
 * 由 @EnableRateLimit 导入；RedissonClient 由应用提供。
 */
@Configuration
@EnableAspectJAutoProxy
public class RateLimitConfiguration {

    @Bean
    public RateLimiter rateLimiter(RedissonClient redissonClient) {
        return new RateLimiter(redissonClient);
    }

    @Bean
    public SpelKeyResolver spelKeyResolver() {
        return new SpelKeyResolver();
    }

    @Bean
    public RateLimitAspect rateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver) {
        return new RateLimitAspect(rateLimiter, keyResolver);
    }
}
