package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 参数异常，表示请求参数不合法。
 */
public final class BizArgumentException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public BizArgumentException(String msg) {
        super(ResultCodes.BAD_REQUEST, msg, (Throwable) null);
    }

    public BizArgumentException(Throwable cause) {
        super(ResultCodes.BAD_REQUEST, (String) null, cause);
    }

    public BizArgumentException(String format, Object... args) {
        super(ResultCodes.BAD_REQUEST, String.format(format, args), (Throwable) null);
    }

    public BizArgumentException(Throwable cause, String format, Object... args) {
        super(ResultCodes.BAD_REQUEST, String.format(format, args), cause);
    }
}
