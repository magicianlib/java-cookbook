package io.ituknown.redis;

import java.util.List;

/**
 * 限流判定结果，对应限流命令返回的五元组。
 *
 * <p>各分量含义按声明顺序：
 * <ul>
 *   <li>是否放行：本次申请是否被允许通过</li>
 *   <li>桶总容量：等于最大突发量加一，即桶可容纳的最大请求数</li>
 *   <li>剩余配额：当前桶内还可消费的请求数</li>
 *   <li>重试等待秒数：被限流时建议等待多久再重试，放行时为 -1</li>
 *   <li>恢复满桶等待秒数：桶配额完全恢复所需时间</li>
 * </ul>
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
        if (element instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(element));
    }
}
