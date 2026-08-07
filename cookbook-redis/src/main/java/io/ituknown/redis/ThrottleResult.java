package io.ituknown.redis;

import java.util.List;

/**
 * 限流判定结果。第 1 位为 0 表示放行，1 表示被限流；后续依次为桶容量、剩余令牌、
 * 重试等待秒数（-1 表示未被限流）、恢复满桶等待秒数。
 */
public record ThrottleResult(boolean allowed, long limit, long remaining,
                             long retryAfter, long resetAfter) {

    public static ThrottleResult from(List<?> raw) {
        return new ThrottleResult(
                asLong(raw.get(0)) == 0,
                asLong(raw.get(1)),
                asLong(raw.get(2)),
                asLong(raw.get(3)),
                asLong(raw.get(4)));
    }

    private static long asLong(Object element) {
        return ((Number) element).longValue();
    }
}
