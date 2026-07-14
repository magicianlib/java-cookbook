package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 未认证（未登录）。
 * <p>用于未携带凭证、Token 过期、签名无效等身份校验失败的场景。
 *
 * @see ResultCodes#UNAUTHORIZED
 */
public class BizUnauthorizedException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public BizUnauthorizedException() {
        this(ResultCodes.UNAUTHORIZED.message());
    }

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
