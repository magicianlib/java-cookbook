package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCode;
import io.ituknown.payload.ResultCodes;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 业务异常基类，所有 {@code Biz*} 异常类均继承此类。
 *
 * <p>子类只需提供各自的默认 {@link ResultCode}，无需重复声明所有构造方法。
 * 核心构造方法为 {@link #BizException(ResultCode, String, Throwable)}，
 * 子类直接调用即可完成扩展。
 */
@Getter
public class BizException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final ResultCode code;

    // ==================== Core constructor ====================

    /**
     * 核心构造方法，所有其他构造方法最终委托于此。
     *
     * @param code    响应码
     * @param message 详细信息，可为 {@code null}
     * @param cause   原始异常，可为 {@code null}
     */
    protected BizException(ResultCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // ==================== Default-code constructors ====================

    public BizException(String msg) {
        this(ResultCodes.FAILURE, msg, (Throwable) null);
    }

    public BizException(Throwable cause) {
        this(ResultCodes.FAILURE, (String) null, cause);
    }

    // ==================== Explicit-code constructors ====================

    public BizException(ResultCode code, String msg) {
        this(code, msg, (Throwable) null);
    }

    public BizException(ResultCode code, Throwable cause, String msg) {
        this(code, msg, cause);
    }

    public BizException(ResultCode code, String format, Object... args) {
        this(code, String.format(format, args), (Throwable) null);
    }

    public BizException(ResultCode code, Throwable cause, String format, Object... args) {
        this(code, String.format(format, args), cause);
    }

    // ==================== Getter ====================

    // ==================== Static helpers ====================

    /**
     * 当表达式为 {@code true} 时抛出异常。
     */
    public static void checkCondition(boolean expression, String format, Object... args) {
        if (expression) {
            String message = String.format(format, args);
            throw new BizException(message);
        }
    }

    /**
     * 当对象为 {@code null} 时抛出异常。
     */
    public static <T> void requireNonNull(T obj, String message, Object... args) {
        checkCondition(Objects.isNull(obj), message, args);
    }

    /**
     * 当集合为 {@code null} 或为空时抛出异常。
     */
    public static <T> void requireNonEmpty(Collection<T> collection, String message, Object... args) {
        checkCondition(Objects.isNull(collection) || collection.isEmpty(), message, args);
    }

    /**
     * 当 Map 为 {@code null} 或为空时抛出异常。
     */
    public static <K, V> void requireNonEmpty(Map<K, V> map, String message, Object... args) {
        checkCondition(Objects.isNull(map) || map.isEmpty(), message, args);
    }
}