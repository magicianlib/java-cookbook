package io.ituknown.result;

/**
 * 基础错误码
 *
 * @author magicianlib@gmail.com
 */
public enum BasicErrorCode implements ErrorCode {
    SUCCESS(0, "Success", true),
    FAILURE(1, "Failure"),
    ;

    private final int code;
    private final String message;
    private final boolean success; // 默认为失败错误码

    BasicErrorCode(int code, String message) {
        this(code, message, false);
    }

    BasicErrorCode(int code, String message, boolean success) {
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