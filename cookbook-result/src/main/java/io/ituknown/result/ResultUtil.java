package io.ituknown.result;

public final class ResultUtil {
    /**
     * 构建返回结果
     */
    private static <T> Result<T> build(ErrorCode errorCode, T data) {
        return build(errorCode, errorCode.message(), data);
    }

    private static <T> Result<T> build(ErrorCode errorCode, String message, T data) {
        Result<T> result = new Result<>();
        result.setSuccess(errorCode.success());
        result.setCode(errorCode.code());
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 成功结果
     */
    public static <T> Result<T> createSuccess() {
        return build(BasicErrorCode.SUCCESS, null);
    }

    public static <T> Result<T> createSuccess(T data) {
        return build(BasicErrorCode.SUCCESS, data);
    }

    /**
     * 失败结果
     */
    public static <T> Result<T> createFailure() {
        return build(BasicErrorCode.FAILURE, null);
    }

    public static <T> Result<T> createFailure(String message) {
        return build(BasicErrorCode.FAILURE, message, null);
    }

    public static <T> Result<T> createFailure(T data) {
        return build(BasicErrorCode.FAILURE, data);
    }

    public static <T> Result<T> createFailure(String message, T data) {
        return build(BasicErrorCode.FAILURE, message, data);
    }

    /**
     * 自定义结果
     */
    public static <T> Result<T> create(ErrorCode errorCode) {
        return build(errorCode, null);
    }

    public static <T> Result<T> create(ErrorCode errorCode, T data) {
        return build(errorCode, data);
    }

    public static <T> Result<T> create(ErrorCode errorCode, String message) {
        return build(errorCode, message, null);
    }

    public static <T> Result<T> create(ErrorCode errorCode, String message, T data) {
        return build(errorCode, message, data);
    }
}