package io.ituknown.redis.aspect;

import io.ituknown.redis.RateLimitExceededException;
import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.ThrottleResult;
import io.ituknown.redis.annotation.RateLimit;
import io.ituknown.redis.support.SpelKeyResolver;

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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RateLimitAspectTest.TestConfig.class)
class RateLimitAspectTest {

    @Autowired
    SampleService sampleService;

    @Test
    void allowed_request_proceeds() {
        assertEquals("ok-1", sampleService.allowed("1"));
    }

    @Test
    void denied_request_throws() {
        assertThrows(RateLimitExceededException.class, () -> sampleService.denied("x"));
    }

    @Test
    void redis_failure_fails_open() {
        assertEquals("ok-y", sampleService.broken("y"));
    }

    static class SampleService {
        @RateLimit(count = 5)
        public String allowed(String id) {
            return "ok-" + id;
        }

        @RateLimit(count = 5)
        public String denied(String id) {
            return "ok-" + id;
        }

        @RateLimit(count = 5)
        public String broken(String id) {
            return "ok-" + id;
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
        RateLimiter rateLimiter() {
            return new RateLimiter(null) {
                @Override
                public ThrottleResult tryAcquire(String key, int maxBurst, int count, int period, int quantity) {
                    if (key.endsWith("#broken")) {
                        throw new RuntimeException("redis down");
                    }
                    if (key.endsWith("#denied")) {
                        return new ThrottleResult(false, 5, 0, 2, 8);
                    }
                    return new ThrottleResult(true, 5, 4, -1, 0);
                }
            };
        }

        @Bean
        RateLimitAspect rateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver) {
            return new RateLimitAspect(rateLimiter, keyResolver);
        }

        @Bean
        SampleService sampleService() {
            return new SampleService();
        }
    }
}
