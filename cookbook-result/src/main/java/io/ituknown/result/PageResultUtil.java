package io.ituknown.result;

import java.util.Collection;

public final class PageResultUtil {
    /**
     * 构建返回结果
     */
    private static <T> PageResult<T> build(ErrorCode errorCode, Collection<T> data) {
        return build(errorCode, errorCode.message(), data);
    }

    private static <T> PageResult<T> build(ErrorCode errorCode, String message, Collection<T> data) {
        PageResult<T> result = new PageResult<>();
        result.setSuccess(errorCode.success());
        result.setCode(errorCode.code());
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 成功结果
     */
    public static <T> PageResult<T> createSuccess() {
        return build(BasicErrorCode.SUCCESS, null);
    }

    public static <T> PageResult<T> createSuccess(Collection<T> data) {
        return build(BasicErrorCode.SUCCESS, data);
    }

    /**
     * 失败结果
     */
    public static <T> PageResult<T> createFailure() {
        return build(BasicErrorCode.FAILURE, null);
    }

    public static <T> PageResult<T> createFailure(String message) {
        return build(BasicErrorCode.FAILURE, message, null);
    }

    public static <T> PageResult<T> createFailure(Collection<T> data) {
        return build(BasicErrorCode.FAILURE, data);
    }

    public static <T> PageResult<T> createFailure(String message, Collection<T> data) {
        return build(BasicErrorCode.FAILURE, message, data);
    }

    /**
     * 自定义结果
     */
    public static <T> PageResult<T> create(ErrorCode errorCode) {
        return build(errorCode, null);
    }

    public static <T> PageResult<T> create(ErrorCode errorCode, Collection<T> data) {
        return build(errorCode, data);
    }

    public static <T> PageResult<T> create(ErrorCode errorCode, String message) {
        return build(errorCode, message, null);
    }

    public static <T> PageResult<T> create(ErrorCode errorCode, String message, Collection<T> data) {
        return build(errorCode, message, data);
    }
}