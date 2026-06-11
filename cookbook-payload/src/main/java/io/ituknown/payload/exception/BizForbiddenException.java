package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 授权异常，表示权限不足或访问被拒绝。
 */
public final class BizForbiddenException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public BizForbiddenException(String msg) {
        super(ResultCodes.FORBIDDEN, msg, (Throwable) null);
    }

    public BizForbiddenException(Throwable cause) {
        super(ResultCodes.FORBIDDEN, (String) null, cause);
    }

    public BizForbiddenException(String format, Object... args) {
        super(ResultCodes.FORBIDDEN, String.format(format, args), (Throwable) null);
    }

    public BizForbiddenException(Throwable cause, String format, Object... args) {
        super(ResultCodes.FORBIDDEN, String.format(format, args), cause);
    }
}
