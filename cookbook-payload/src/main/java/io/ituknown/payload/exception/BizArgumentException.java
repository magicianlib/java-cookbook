package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 参数错误 / 请求不合法。
 * <p>用于请求参数缺失、格式错误、值越界、类型不匹配等入参校验失败的场景。
 *
 * @see ResultCodes#BAD_REQUEST
 */
public class BizArgumentException extends BizException {

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
