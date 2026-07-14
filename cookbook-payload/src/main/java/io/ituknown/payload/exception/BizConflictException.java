package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 资源冲突。
 * <p>用于唯一约束冲突、并发修改版本不匹配、重复创建等场景。
 *
 * @see ResultCodes#CONFLICT
 */
public class BizConflictException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public BizConflictException() {
        this(ResultCodes.CONFLICT.message());
    }

    public BizConflictException(String msg) {
        super(ResultCodes.CONFLICT, msg, (Throwable) null);
    }

    public BizConflictException(Throwable cause) {
        super(ResultCodes.CONFLICT, (String) null, cause);
    }

    public BizConflictException(String format, Object... args) {
        super(ResultCodes.CONFLICT, String.format(format, args), (Throwable) null);
    }

    public BizConflictException(Throwable cause, String format, Object... args) {
        super(ResultCodes.CONFLICT, String.format(format, args), cause);
    }
}
