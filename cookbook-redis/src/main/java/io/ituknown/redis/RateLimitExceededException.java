package io.ituknown.redis;

import lombok.Getter;

/**
 * 请求超过限流配额时抛出。携带限流上下文（配额上限、剩余、建议等待秒数），
 * 方便上层转成 HTTP 429 状态码和 Retry-After。
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