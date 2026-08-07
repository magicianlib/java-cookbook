package io.ituknown.redis;

import lombok.Getter;

/**
 * 限流异常类
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final String key;
    private final long limit;
    private final long remaining;
    private final long retryAfter;
    private final long resetAfter;

    public RateLimitExceededException(String key, ThrottleStatus result) {
        super("请求被限流; key=" + key + ", retryAfter=" + result.retryAfter() + "s");
        this.key = key;
        this.limit = result.limit();
        this.remaining = result.remaining();
        this.retryAfter = result.retryAfter();
        this.resetAfter = result.resetAfter();
    }
}
