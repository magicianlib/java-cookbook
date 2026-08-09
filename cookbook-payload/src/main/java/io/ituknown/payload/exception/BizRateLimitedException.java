package io.ituknown.payload.exception;

import io.ituknown.payload.ResultCodes;

/**
 * 请求被限流。
 * <p>用于调用频率超出配额、触发限流的场景。
 *
 * @see ResultCodes#TOO_MANY_REQUESTS
 */
public class BizRateLimitedException extends BizException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public BizRateLimitedException() {
        this(ResultCodes.TOO_MANY_REQUESTS.message());
    }

    public BizRateLimitedException(String msg) {
        super(ResultCodes.TOO_MANY_REQUESTS, msg, (Throwable) null);
    }

    public BizRateLimitedException(Throwable cause) {
        super(ResultCodes.TOO_MANY_REQUESTS, null, cause);
    }

    public BizRateLimitedException(String format, Object... args) {
        super(ResultCodes.TOO_MANY_REQUESTS, String.format(format, args), (Throwable) null);
    }

    public BizRateLimitedException(Throwable cause, String format, Object... args) {
        super(ResultCodes.TOO_MANY_REQUESTS, String.format(format, args), cause);
    }
}