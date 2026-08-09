package io.ituknown.redis.config;

import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.annotation.EnableRateLimit;
import io.ituknown.redis.aspect.RateLimitAspect;
import io.ituknown.redis.support.SpelKeyResolver;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * 限流装配：注册执行器、维度解析器、切面，并开启方法代理。
 * 由 {@link EnableRateLimit} 导入；
 * {@link RedissonClient} 由应用提供，缺失时容器启动即失败，便于尽早暴露配置问题。
 *
 * <p>实现 {@link ImportAware}，从导入方上的 {@link EnableRateLimit#order()} 读取切面顺序注入切面，
 * 把"开启限流"与"切面排序"的配置统一收口到同一个注解。
 */
@Configuration
@EnableAspectJAutoProxy
public class RateLimitConfiguration implements ImportAware {

    /**
     * 限流切面顺序，取自 {@link EnableRateLimit#order()}，未取到时按最高优先级
     */
    private int order = Ordered.HIGHEST_PRECEDENCE;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> attrs = importMetadata.getAnnotationAttributes(EnableRateLimit.class.getName());
        if (attrs != null) {
            Object value = attrs.get("order");
            if (value != null) {
                this.order = (Integer) value;
            }
        }
    }

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
        return new RateLimitAspect(rateLimiter, keyResolver, order);
    }
}