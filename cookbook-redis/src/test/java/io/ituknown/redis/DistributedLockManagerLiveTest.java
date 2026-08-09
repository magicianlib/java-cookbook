package io.ituknown.redis;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分布式锁执行器的真机测试：连本地 Dragonfly/Redis 验证互斥与释放，不可达时自动跳过。
 *
 * <p>用两个线程加 {@link CountDownLatch} 做时序控制，确定性验证：主线程持锁时另一线程
 * {@code waitTime=0} 抢锁失败；主线程释放后另一线程能重新抢到。
 */
class DistributedLockManagerLiveTest {

    private static final String ADDRESS = "redis://127.0.0.1:6379";

    private static DistributedLockManager manager;
    private static RedissonClient client;

    @BeforeAll
    static void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress(ADDRESS);
        try {
            client = Redisson.create(config);
            client.getBucket("lock:probe").delete(); // 探活，连不上会抛异常
        } catch (Exception e) {
            client = null;
        }
        Assumptions.assumeTrue(client != null, "本地 Dragonfly 不可用，跳过真机测试");
        manager = new DistributedLockManager(client);
    }

    @AfterAll
    static void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void held_lock_blocks_second_acquire_then_releases() throws Exception {
        String key = "live:lock:" + UUID.randomUUID();

        // 主线程先抢到锁
        assertTrue(manager.tryAcquire(key, 0, 30, TimeUnit.SECONDS));

        CountDownLatch firstAcquireDone = new CountDownLatch(1);
        CountDownLatch releaseSignal = new CountDownLatch(1);
        AtomicReference<Boolean> blockedResult = new AtomicReference<>();
        AtomicReference<Boolean> reacquireResult = new AtomicReference<>();

        Thread other = new Thread(() -> {
            // 主线程持锁期间抢，应失败（waitTime=0）
            blockedResult.set(manager.tryAcquire(key, 0, 30, TimeUnit.SECONDS));
            firstAcquireDone.countDown();
            // 等主线程释放后再抢，应成功
            try {
                releaseSignal.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            reacquireResult.set(manager.tryAcquire(key, 0, 30, TimeUnit.SECONDS));
        });
        other.start();

        // 等另一线程完成第一次抢锁
        firstAcquireDone.await();
        assertFalse(blockedResult.get(), "锁被持有时，waitTime=0 抢锁应失败");

        // 主线程释放
        manager.release(key);

        // 通知另一线程继续第三次抢锁
        releaseSignal.countDown();
        other.join();
        assertTrue(reacquireResult.get(), "锁释放后应能重新抢到");
    }
}
