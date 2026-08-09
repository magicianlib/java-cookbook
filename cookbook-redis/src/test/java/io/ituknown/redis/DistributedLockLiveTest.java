package io.ituknown.redis;

import io.ituknown.redis.annotation.DistributedLock;
import io.ituknown.redis.annotation.EnableDistributedLock;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 切面真机测试：连本地 Dragonfly/Redis 验证 {@code @DistributedLock} 端到端流程，不可达时自动跳过。
 *
 * <p>互斥验证用 {@link CountDownLatch} 做时序控制：一个线程进入注解方法后持锁等待，
 * 另一个线程并发调用同 key 应被拒（{@code waitTime=0}），确定性无随机时序。
 *
 * <p>注：Redisson 锁按线程重入，故"占住锁再同线程调用"无法触发拒绝；必须跨线程，见
 * {@code concurrent_call_on_held_key_is_rejected}。
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DistributedLockLiveTest.AppConfig.class)
class DistributedLockLiveTest {

    @Autowired
    LockedService lockedService;
    @Autowired
    RedissonClient client;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(canReachRedis(), "本地 Dragonfly 不可用，跳过真机测试");
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void acquire_runs_and_releases() throws Exception {
        // 抢锁 -> 执行 -> 释放，端到端验证注解驱动流程；首次释放后同线程二次调用同样放行
        String id = UUID.randomUUID().toString();
        assertEquals("done-" + id, lockedService.doWork(id, null, null));
        assertEquals("done-" + id, lockedService.doWork(id, null, null));
    }

    @Test
    void concurrent_call_on_held_key_is_rejected() throws Exception {
        String id = UUID.randomUUID().toString();
        CountDownLatch inCriticalSection = new CountDownLatch(1);
        CountDownLatch allowFirstToFinish = new CountDownLatch(1);

        // 线程 A 进入注解方法后持锁等待，模拟临界区占用
        Future<String> first = executor.submit(() -> lockedService.doWork(id, inCriticalSection, allowFirstToFinish));
        assertTrue(inCriticalSection.await(10, TimeUnit.SECONDS), "A 应进入方法并持锁");

        // 测试线程并发调用同 key（waitTime=0），应被拒并抛 LockNotAcquiredException
        assertThrows(LockNotAcquiredException.class, () -> lockedService.doWork(id, null, null));

        // 放行 A，验证它正常完成（锁随后在 finally 释放）
        allowFirstToFinish.countDown();
        assertEquals("done-" + id, first.get(10, TimeUnit.SECONDS));
    }

    private boolean canReachRedis() {
        try {
            client.getBucket("lock:probe").delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static class LockedService {
        /**
         * 带协调 latch 的注解方法：signal 非空时在方法体内持锁等待，用于真机测试构造临界区占用。
         * 锁键仅由 {@code #id} 决定，额外入参不影响键解析。
         */
        @DistributedLock(key = "#id")
        public String doWork(String id, CountDownLatch signal, CountDownLatch wait) throws InterruptedException {
            if (signal != null) {
                signal.countDown();
                assertTrue(wait.await(10, TimeUnit.SECONDS), "应被测试线程放行");
            }
            return "done-" + id;
        }
    }

    @Configuration
    @EnableDistributedLock
    static class AppConfig {

        @Bean(destroyMethod = "shutdown")
        RedissonClient redissonClient() {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://127.0.0.1:6379");
            return Redisson.create(config);
        }

        @Bean
        LockedService lockedService() {
            return new LockedService();
        }
    }
}
