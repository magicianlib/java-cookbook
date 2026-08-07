package io.ituknown.redis;

import io.ituknown.redis.annotation.EnableRateLimit;
import io.ituknown.redis.annotation.RateLimit;

import java.util.UUID;

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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RateLimitLiveTest.AppConfig.class)
class RateLimitLiveTest {

    @Autowired
    LimitedService limitedService;
    @Autowired
    RedissonClient client;

    @BeforeEach
    void requireRedis() {
        Assumptions.assumeTrue(canReachRedis(), "本地 Dragonfly 不可用，跳过真机测试");
    }

    @Test
    void annotation_throttles_via_real_redis() {
        String id = UUID.randomUUID().toString();

        // 配额 5、突发上限 3 对应 limit=4，前 4 次放行
        for (int i = 0; i < 4; i++) {
            assertEquals("ok-" + id, limitedService.call(id));
        }
        // 第 5 次被限流
        assertThrows(RateLimitExceededException.class, () -> limitedService.call(id));
    }

    private boolean canReachRedis() {
        try {
            client.getBucket("ratelimit:probe").delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static class LimitedService {
        @RateLimit(key = "#id", count = 5, maxBurst = 3)
        public String call(String id) {
            return "ok-" + id;
        }
    }

    @Configuration
    @EnableRateLimit
    static class AppConfig {

        @Bean(destroyMethod = "shutdown")
        RedissonClient redissonClient() {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://127.0.0.1:6379");
            return Redisson.create(config);
        }

        @Bean
        LimitedService limitedService() {
            return new LimitedService();
        }
    }
}
