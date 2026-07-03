package io.ituknown.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC 工具类，提供链路追踪 ID 等常用上下文字段的设置与获取。
 * <p>
 * 预定义 key 常量见本类静态字段，业务模块可直接使用，
 * 也可通过 {@link MDC} 直接操作自定义 key。
 *
 * @author magicianlib@gmail.com
 * @see MdcScope
 */
public final class MdcUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MdcUtils.class);

    // ===== 预定义 key 常量 =====

    /**
     * 链路追踪 ID
     */
    public static final String TRACE_ID = "traceId";
    /**
     * 用户 ID
     */
    public static final String USER_ID = "userId";

    private MdcUtils() {
    }

    // ===== TraceId =====

    /**
     * 生成随机 traceId 并设置到 MDC。
     * <p>
     * 如果当前 MDC 中已有 traceId，则以 {@code |} 拼接，形成嵌套链路。
     */
    public static void withTrace() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        withTrace(traceId);
    }

    /**
     * 将指定 traceId 设置到 MDC。
     * <p>
     * 如果当前 MDC 中已有 traceId，则以 {@code |} 拼接，形成嵌套链路。
     *
     * @param traceId 链路追踪 ID，为 null 或空时不设置并记录警告日志
     */
    public static void withTrace(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            LOGGER.warn("traceId must not be null or empty");
            return;
        }

        String current = MDC.get(TRACE_ID);
        if (current == null || current.isEmpty()) {
            setMdc(traceId);
        } else {
            setMdc(current + "|" + traceId);
        }
    }

    public static String getTrace() {
        String mdc = getMdc();
        if (StringUtils.isBlank(mdc)) {
            withTrace();
            mdc = getMdc();
        }
        return StringUtils.substringAfterLast(mdc, "|");
    }

    /**
     * 获取当前 traceId。
     *
     * @return traceId，不存在则返回 null
     */
    public static String getMdc() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 设置 traceId，覆盖已有值。
     *
     * @param mdc 链路追踪 ID，为 null 或空时不设置并记录警告日志
     */
    public static void setMdc(String mdc) {
        if (mdc == null || mdc.isEmpty()) {
            LOGGER.warn("mdc must not be null or empty");
            return;
        }
        MDC.put(TRACE_ID, mdc);
    }

    // ===== UserId =====

    /**
     * 获取当前 userId。
     *
     * @return userId，不存在则返回 null
     */
    public static String getUser() {
        return MDC.get(USER_ID);
    }

    /**
     * 设置 userId。
     *
     * @param userId 用户 ID，为 null 或空时不设置并记录警告日志
     */
    public static void setUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            LOGGER.warn("userId must not be null or empty");
            return;
        }
        MDC.put(USER_ID, userId);
    }

    // ===== 通用操作 =====

    /**
     * 获取 MDC 中指定 key 的值。
     *
     * @param key MDC key
     * @return 对应值，不存在则返回 null
     */
    public static String get(String key) {
        return MDC.get(key);
    }

    /**
     * 设置 MDC 键值对。
     *
     * @param key   MDC key，为 null 或空时不设置并记录警告日志
     * @param value 值，为 null 或空时不设置并记录警告日志
     */
    public static void put(String key, String value) {
        if (key == null || key.isEmpty()) {
            LOGGER.warn("key must not be null or empty");
            return;
        }
        if (value == null || value.isEmpty()) {
            LOGGER.warn("value must not be null or empty, key={}", key);
            return;
        }
        MDC.put(key, value);
    }

    /**
     * 移除 MDC 中指定 key。
     *
     * @param key MDC key
     */
    public static void remove(String key) {
        MDC.remove(key);
    }

    /**
     * 清空所有 MDC 内容。
     */
    public static void clear() {
        MDC.clear();
    }
}
