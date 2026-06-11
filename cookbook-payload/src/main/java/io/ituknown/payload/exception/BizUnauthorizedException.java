package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 认证异常，表示未登录或凭证无效。
 */
public class BizUnauthorizedException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public BizUnauthorizedException(String msg) {
        super(ResultCodes.UNAUTHORIZED, msg, (Throwable) null);
    }

    public BizUnauthorizedException(Throwable cause) {
        super(ResultCodes.UNAUTHORIZED, (String) null, cause);
    }

    public BizUnauthorizedException(String format, Object... args) {
        super(ResultCodes.UNAUTHORIZED, String.format(format, args), (Throwable) null);
    }

    public BizUnauthorizedException(Throwable cause, String format, Object... args) {
        super(ResultCodes.UNAUTHORIZED, String.format(format, args), cause);
    }
}
