package io.ituknown.redis.config;

import io.ituknown.redis.DistributedLockManager;
import io.ituknown.redis.annotation.EnableDistributedLock;
import io.ituknown.redis.aspect.DistributedLockAspect;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 分布式锁装配的顺序透传单测：验证 {@link EnableDistributedLock#order()} 经 ImportAware 注入到切面。
 *
 * <p>直接驱动 {@link DistributedLockConfiguration}，不启动 Spring 容器、不连接 Redis，故可离线运行；
 * {@link EnableDistributedLockTest} 走完整容器装配。
 */
class DistributedLockConfigurationTest {

    @Test
    void default_order_is_highest_precedence_plus_one() {
        DistributedLockConfiguration cfg = newConfig(DefaultMarker.class);
        DistributedLockAspect aspect = cfg.distributedLockAspect(new DistributedLockManager(null));
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 1, aspect.getOrder());
    }

    @Test
    void configured_order_is_applied() {
        DistributedLockConfiguration cfg = newConfig(Order42Marker.class);
        DistributedLockAspect aspect = cfg.distributedLockAspect(new DistributedLockManager(null));
        assertEquals(42, aspect.getOrder());
    }

    private static DistributedLockConfiguration newConfig(Class<?> importingClass) {
        DistributedLockConfiguration cfg = new DistributedLockConfiguration();
        cfg.setImportMetadata(AnnotationMetadata.introspect(importingClass));
        return cfg;
    }

    @EnableDistributedLock
    static class DefaultMarker {
    }

    @EnableDistributedLock(order = 42)
    static class Order42Marker {
    }
}
