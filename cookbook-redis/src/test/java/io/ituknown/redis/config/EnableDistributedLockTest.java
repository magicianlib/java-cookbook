package io.ituknown.redis.config;

import io.ituknown.redis.DistributedLockManager;
import io.ituknown.redis.annotation.EnableDistributedLock;
import io.ituknown.redis.aspect.DistributedLockAspect;

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
@ContextConfiguration(classes = EnableDistributedLockTest.AppConfig.class)
class EnableDistributedLockTest {

    @Autowired
    DistributedLockManager distributedLockManager;
    @Autowired
    DistributedLockAspect distributedLockAspect;

    @Test
    void enable_registers_all_beans() {
        assertNotNull(distributedLockManager);
        assertNotNull(distributedLockAspect);
    }

    @Test
    void enable_applies_configured_order() {
        // @EnableDistributedLock(order = 42) 经 @Import + ImportAware 透传到切面
        assertEquals(42, distributedLockAspect.getOrder());
    }

    @Configuration
    @EnableDistributedLock(order = 42)
    static class AppConfig {

        @Bean(destroyMethod = "shutdown")
        RedissonClient redissonClient() {
            // 惰性客户端，构造期不连接；测试不触发锁调用，故离线可跑
            return Redisson.create();
        }
    }
}
