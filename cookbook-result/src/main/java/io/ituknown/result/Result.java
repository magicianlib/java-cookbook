package io.ituknown.result;

import io.ituknown.jackson.JacksonUtils;

import java.io.Serial;
import java.io.Serializable;

public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 8249601870035703225L;

    public Result(T data) {
        this.data = data;
    }

    /**
     * 状态码
     */
    private int code;
    /**
     * 信息
     */
    private String message;
    /**
     * 数据
     */
    private final T data;

    @Override
    public String toString() {
        return JacksonUtils.toJson(this);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public static <T> Result<T> create(Status status, String message, T data) {
        Result<T> result = new Result<>(data);
        result.setCode(status.code());
        result.setMessage(message != null && !message.isBlank() ? message : status.message());
        return result;
    }

    public static <T> Result<T> createSuccess() {
        return create(BasicStatus.SUCCESS, null, null);
    }

    public static <T> Result<T> createSuccess(T data) {
        return create(BasicStatus.SUCCESS, null, data);
    }

    public static <T> Result<T> createFailure() {
        return create(BasicStatus.FAILURE, null, null);
    }

    public static <T> Result<T> createFailure(Status status) {
        return create(status, null, null);
    }

    public static <T> Result<T> createFailure(Status status, String message) {
        return create(status, message, null);
    }
}