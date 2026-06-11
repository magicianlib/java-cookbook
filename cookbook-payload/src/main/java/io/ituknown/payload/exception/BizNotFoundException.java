package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 资源未找到异常。
 */
public final class BizNotFoundException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public BizNotFoundException(String msg) {
        super(ResultCodes.NOT_FOUND, msg, (Throwable) null);
    }

    public BizNotFoundException(Throwable cause) {
        super(ResultCodes.NOT_FOUND, (String) null, cause);
    }

    public BizNotFoundException(String format, Object... args) {
        super(ResultCodes.NOT_FOUND, String.format(format, args), (Throwable) null);
    }

    public BizNotFoundException(Throwable cause, String format, Object... args) {
        super(ResultCodes.NOT_FOUND, String.format(format, args), cause);
    }
}
