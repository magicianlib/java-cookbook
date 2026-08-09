package io.ituknown.redis.aspect;

import io.ituknown.redis.DistributedLockManager;
import io.ituknown.redis.LockNotAcquiredException;
import io.ituknown.redis.annotation.DistributedLock;
import io.ituknown.redis.support.SpelKeyResolver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DistributedLockAspectTest.TestConfig.class)
class DistributedLockAspectTest {

    @Autowired
    SampleService sampleService;
    @Autowired
    RecordingManager manager;

    @BeforeEach
    void reset() {
        // 切面与执行器是同一 Spring 上下文里的单例，释放记录跨用例累积，每条用例前清空
        manager.released.clear();
    }

    @Test
    void acquired_request_proceeds_and_releases() {
        assertEquals("ok-1", sampleService.acquire("1"));
        assertEquals(List.of("SampleService#acquire#1"), manager.released);
    }

    @Test
    void not_acquired_throws_without_release() {
        assertThrows(LockNotAcquiredException.class, () -> sampleService.deny("x"));
        assertTrue(manager.released.isEmpty(), "未抢到锁时不应触发释放");
    }

    @Test
    void business_exception_still_releases() {
        assertThrows(RuntimeException.class, () -> sampleService.boom("y"));
        assertEquals(List.of("SampleService#boom#y"), manager.released);
    }

    static class SampleService {
        @DistributedLock(key = "#id")
        public String acquire(String id) {
            return "ok-" + id;
        }

        @DistributedLock(key = "#id")
        public String deny(String id) {
            return "ok-" + id;
        }

        @DistributedLock(key = "#id")
        public String boom(String id) {
            throw new RuntimeException("boom-" + id);
        }
    }

    /**
     * 记录释放调用的执行器桩：按锁键片段决定抢到与否，并把释放的键记下来供断言。
     */
    static class RecordingManager extends DistributedLockManager {
        final List<String> released = new CopyOnWriteArrayList<>();

        RecordingManager() {
            super(null);
        }

        @Override
        public boolean tryAcquire(String key, long waitTime, long leaseTime, TimeUnit unit) {
            // deny 方法对应的键形如 SampleService#deny#x，抢不到；其余抢到
            return !key.contains("#deny#");
        }

        @Override
        public void release(String key) {
            released.add(key);
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        SpelKeyResolver spelKeyResolver() {
            return new SpelKeyResolver();
        }

        @Bean
        RecordingManager distributedLockManager() {
            return new RecordingManager();
        }

        @Bean
        DistributedLockAspect distributedLockAspect(RecordingManager manager, SpelKeyResolver keyResolver) {
            // 默认顺序即 HIGHEST_PRECEDENCE + 1（见 DistributedLockAspect 类注释），此处沿用默认
            return new DistributedLockAspect(manager, keyResolver);
        }

        @Bean
        SampleService sampleService() {
            return new SampleService();
        }
    }
}
