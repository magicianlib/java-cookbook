package io.ituknown.result;

/**
 * 通用响应码，提供开箱即用的成功 / 失败响应码。
 * <p>
 * 业务模块如需自定义响应码，请实现 {@link ResultCode} 接口。
 *
 * @author magicianlib@gmail.com
 * @see ResultCode
 */
public enum ResultCodes implements ResultCode {
    SUCCESS(0, "success", true),
    FAILURE(1, "failure"),
    ;

    private final int code;
    private final String message;
    private final boolean success;

    ResultCodes(int code, String message) {
        this(code, message, false);
    }

    ResultCodes(int code, String message, boolean success) {
        this.code = code;
        this.message = message;
        this.success = success;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public boolean success() {
        return success;
    }
}
