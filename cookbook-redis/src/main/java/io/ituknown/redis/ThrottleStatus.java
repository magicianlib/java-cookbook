package io.ituknown.redis;

import java.util.List;

/**
 * 一次限流判定的结果，对应 CL.THROTTLE 返回的五元组。
 *
 * @param allowed    是否放行（false 表示被限流）
 * @param limit      桶容量，即最多放过的请求数（最大突发量 + 1）
 * @param remaining  剩余配额
 * @param retryAfter 被限流时建议等待的秒数；放行时为 -1
 * @param resetAfter 配额完全恢复所需的秒数
 */
public record ThrottleStatus(
        boolean allowed, long limit,
        long remaining, long retryAfter, long resetAfter) {

    public static ThrottleStatus from(List<?> raw) {
        return new ThrottleStatus(
                asLong(raw.get(0)) == 0,
                asLong(raw.get(1)),
                asLong(raw.get(2)),
                asLong(raw.get(3)),
                asLong(raw.get(4))
        );
    }

    private static long asLong(Object element) {
        if (element instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(element));
    }
}
