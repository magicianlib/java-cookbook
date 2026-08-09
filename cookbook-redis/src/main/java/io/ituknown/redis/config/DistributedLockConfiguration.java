package io.ituknown.redis.config;

import io.ituknown.redis.DistributedLockManager;
import io.ituknown.redis.annotation.EnableDistributedLock;
import io.ituknown.redis.aspect.DistributedLockAspect;
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
 * 分布式锁装配：注册执行器与切面，并开启方法代理。
 * 由 {@link EnableDistributedLock} 导入；
 * {@link RedissonClient} 由应用提供，缺失时容器启动即失败，便于尽早暴露配置问题。
 *
 * <p>实现 {@link ImportAware}，从导入方上的 {@link EnableDistributedLock#order()} 读取切面顺序注入切面，
 * 把"开启分布式锁"与"切面排序"的配置统一收口到同一个注解。
 *
 * <p>{@link SpelKeyResolver} 不作为共享 bean 注册，而是在切面构造时内联创建，避免与限流
 * {@code @EnableRateLimit} 同时启用时出现两个同类型 bean 冲突；解析器仅持表达式缓存，多实例无害。
 */
@Configuration
@EnableAspectJAutoProxy
public class DistributedLockConfiguration implements ImportAware {

    /**
     * 分布式锁切面顺序，取自 {@link EnableDistributedLock#order()}，未取到时按 {@code HIGHEST_PRECEDENCE + 1}
     */
    private int order = Ordered.HIGHEST_PRECEDENCE + 1;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> attrs = importMetadata.getAnnotationAttributes(EnableDistributedLock.class.getName());
        if (attrs != null) {
            Object value = attrs.get("order");
            if (value != null) {
                this.order = (Integer) value;
            }
        }
    }

    @Bean
    public DistributedLockManager distributedLockManager(RedissonClient redissonClient) {
        return new DistributedLockManager(redissonClient);
    }

    @Bean
    public DistributedLockAspect distributedLockAspect(DistributedLockManager lockManager) {
        // 内联创建 SpelKeyResolver，规避与限流模块同时启用时的同类型 bean 冲突
        return new DistributedLockAspect(lockManager, new SpelKeyResolver(), order);
    }
}
