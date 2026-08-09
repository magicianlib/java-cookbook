package io.ituknown.redis;

import lombok.Getter;

/**
 * 在等待时间内未能获取分布式锁时抛出。携带锁键，方便上层定位竞争资源并决定降级策略。
 *
 * <p>注意：与限流 {@link RateLimitExceededException} 不同，分布式锁遇到 Redis 不可用等获取异常时
 * 不会包装成本异常，而是原样上抛底层异常（fail-closed），本异常仅在「锁被他人持有、本次未抢到」时抛出。
 */
@Getter
public class LockNotAcquiredException extends RuntimeException {

    private final String key;

    public LockNotAcquiredException(String key) {
        super("分布式锁获取失败; key=" + key);
        this.key = key;
    }
}