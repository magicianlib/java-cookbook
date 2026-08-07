package io.ituknown.redis;

/**
 * 请求超出限流配额时抛出，携带限流上下文，便于上层映射为 429 与重试等待秒数。
 */
public class RateLimitExceededException extends RuntimeException {

    private final String key;
    private final long limit;
    private final long remaining;
    private final long retryAfter;
    private final long resetAfter;

    public RateLimitExceededException(String key, ThrottleResult result) {
        super("请求被限流; key=" + key + ", retryAfter=" + result.retryAfter() + "s");
        this.key = key;
        this.limit = result.limit();
        this.remaining = result.remaining();
        this.retryAfter = result.retryAfter();
        this.resetAfter = result.resetAfter();
    }

    public String getKey() {
        return key;
    }

    public long getLimit() {
        return limit;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getRetryAfter() {
        return retryAfter;
    }

    public long getResetAfter() {
        return resetAfter;
    }
}
