package io.ituknown.result;

/**
 * 响应结果工厂工具类
 *
 * @author magicianlib@gmail.com
 */
public final class ResultUtils {

    private ResultUtils() {}

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
     * 构建成功结果（无数据）
     */
    public static <T> Result<T> createSuccess() {
        return build(BasicErrorCode.SUCCESS, null);
    }

    /**
     * 构建成功结果
     *
     * @param data 响应数据
     */
    public static <T> Result<T> createSuccess(T data) {
        return build(BasicErrorCode.SUCCESS, data);
    }

    /**
     * 构建失败结果（无数据、默认错误信息）
     */
    public static <T> Result<T> createFailure() {
        return build(BasicErrorCode.FAILURE, null);
    }

    /**
     * 构建失败结果（自定义错误信息）
     *
     * @param message 错误信息
     */
    public static <T> Result<T> createFailure(String message) {
        return build(BasicErrorCode.FAILURE, message, null);
    }

    /**
     * 构建失败结果（携带数据）
     *
     * @param data 响应数据
     */
    public static <T> Result<T> createFailure(T data) {
        return build(BasicErrorCode.FAILURE, data);
    }

    /**
     * 构建失败结果（自定义错误信息和数据）
     *
     * @param message 错误信息
     * @param data    响应数据
     */
    public static <T> Result<T> createFailure(String message, T data) {
        return build(BasicErrorCode.FAILURE, message, data);
    }

    /**
     * 构建自定义错误码结果（无数据）
     *
     * @param errorCode 错误码
     */
    public static <T> Result<T> create(ErrorCode errorCode) {
        return build(errorCode, null);
    }

    /**
     * 构建自定义错误码结果
     *
     * @param errorCode 错误码
     * @param data      响应数据
     */
    public static <T> Result<T> create(ErrorCode errorCode, T data) {
        return build(errorCode, data);
    }

    /**
     * 构建自定义错误码结果（自定义错误信息）
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public static <T> Result<T> create(ErrorCode errorCode, String message) {
        return build(errorCode, message, null);
    }

    /**
     * 构建自定义错误码结果（自定义错误信息和数据）
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param data      响应数据
     */
    public static <T> Result<T> create(ErrorCode errorCode, String message, T data) {
        return build(errorCode, message, data);
    }
}
