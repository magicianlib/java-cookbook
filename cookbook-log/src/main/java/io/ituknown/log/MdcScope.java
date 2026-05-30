package io.ituknown.log;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MDC 作用域管理，支持 try-with-resources 自动清理。
 * <p>
 * close 时仅清理本 scope 设置的 key，不影响已有的 MDC 内容。
 *
 * <pre>{@code
 * try (MdcScope scope = MdcScope.of(MdcUtils.USER_ID, "123")) {
 *     // MDC 中有 userId=123
 *     log.info("处理中");
 * }
 * // 离开后自动清理 userId
 * }</pre>
 *
 * @author magicianlib@gmail.com
 * @see MdcUtils
 */
public class MdcScope implements AutoCloseable {

    private final Map<String, String> previousValues;

    private MdcScope(Map<String, String> entries) {
        this.previousValues = new LinkedHashMap<>(entries.size());
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            previousValues.put(entry.getKey(), MDC.get(entry.getKey()));
            MDC.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 创建包含单个字段的 MDC scope。
     *
     * @param key   MDC key
     * @param value 值
     * @return MdcScope 实例
     */
    public static MdcScope of(String key, String value) {
        Map<String, String> entries = new LinkedHashMap<>(2);
        entries.put(key, value);
        return new MdcScope(entries);
    }

    /**
     * 创建包含两个字段的 MDC scope。
     *
     * @param key1   第一个 MDC key
     * @param value1 第一个值
     * @param key2   第二个 MDC key
     * @param value2 第二个值
     * @return MdcScope 实例
     */
    public static MdcScope of(String key1, String value1, String key2, String value2) {
        Map<String, String> entries = new LinkedHashMap<>(4);
        entries.put(key1, value1);
        entries.put(key2, value2);
        return new MdcScope(entries);
    }

    /**
     * 创建包含多个字段的 MDC scope。
     *
     * @param entries MDC 键值对
     * @return MdcScope 实例
     */
    public static MdcScope of(Map<String, String> entries) {
        return new MdcScope(entries);
    }

    /**
     * 在 MDC scope 中执行操作，完成后自动清理。
     *
     * @param key     MDC key
     * @param value   值
     * @param runnable 要执行的操作
     */
    public static void wrap(String key, String value, Runnable runnable) {
        try (MdcScope scope = of(key, value)) {
            runnable.run();
        }
    }

    /**
     * 清理本 scope 设置的 MDC 字段，恢复为进入 scope 前的值。
     */
    @Override
    public void close() {
        for (Map.Entry<String, String> entry : previousValues.entrySet()) {
            String previous = entry.getValue();
            if (previous == null) {
                MDC.remove(entry.getKey());
            } else {
                MDC.put(entry.getKey(), previous);
            }
        }
    }
}
