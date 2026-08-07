package io.ituknown.redis;

import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterLiveTest {

    private static final String ADDRESS = "redis://127.0.0.1:6379";

    private static RateLimiter rateLimiter;
    private static RedissonClient client;

    @BeforeAll
    static void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress(ADDRESS);
        try {
            client = Redisson.create(config);
            client.getBucket("ratelimit:probe").delete(); // 探活，连不上会抛异常
        } catch (Exception e) {
            client = null;
        }
        Assumptions.assumeTrue(client != null, "本地 Dragonfly 不可用，跳过真机测试");
        rateLimiter = new RateLimiter(client);
    }

    @Test
    void allows_burst_then_denies_with_retryAfter() {
        String key = "live:" + UUID.randomUUID();

        // maxBurst=3 对应 limit=4，前 4 次放行
        for (int i = 0; i < 4; i++) {
            ThrottleResult allowed = rateLimiter.tryAcquire(key, 3, 5, 10, 1);
            assertTrue(allowed.allowed(), "第 " + (i + 1) + " 次应放行");
        }

        // 第 5 次被限流
        ThrottleResult denied = rateLimiter.tryAcquire(key, 3, 5, 10, 1);
        assertFalse(denied.allowed());
        assertTrue(denied.retryAfter() >= 0, "被限流时重试等待秒数应非负");
    }
}
