package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 冲突异常，表示资源已存在或状态冲突。
 */
public class BizConflictException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

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
