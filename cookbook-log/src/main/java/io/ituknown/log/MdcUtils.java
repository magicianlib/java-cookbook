package io.ituknown.log;

import org.apache.commons.lang3.StringUtils;
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

    /**
     * 在新的链路作用域中执行操作，自动生成随机 traceId。
     * <p>
     * 操作结束后恢复进入作用域前的链路标识，不影响外层链路。
     *
     * @param runnable 要执行的操作
     */
    public static void withTraceScope(Runnable runnable) {
        withTraceScope(UUID.randomUUID().toString().replace("-", ""), runnable);
    }

    /**
     * 在新的链路作用域中执行操作。
     * <p>
     * 若当前已有链路标识，则拼接形成嵌套链路；操作结束后恢复进入作用域前的状态。
     *
     * @param traceId  链路追踪 ID，为空则不设置并记录警告日志
     * @param runnable 要执行的操作
     */
    public static void withTraceScope(String traceId, Runnable runnable) {
        String mdc = getMdc();
        try {
            withTrace(traceId);
            runnable.run();
        } finally {
            if (mdc == null) {
                MDC.remove(TRACE_ID);
            } else {
                MDC.put(TRACE_ID, mdc);
            }
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