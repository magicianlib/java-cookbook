package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 无权限（已认证但权限不足）。
 * <p>用于已登录用户访问超出其角色 / 权限范围的资源的场景。
 *
 * @see ResultCodes#FORBIDDEN
 */
public class BizForbiddenException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public BizForbiddenException() {
        this(ResultCodes.FORBIDDEN.message());
    }

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
