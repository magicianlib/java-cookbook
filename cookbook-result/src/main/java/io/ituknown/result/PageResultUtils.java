package io.ituknown.result;

import java.util.Collection;

/**
 * 分页响应结果工厂工具类
 *
 * @author magicianlib@gmail.com
 */
public final class PageResultUtils {

    private PageResultUtils() {}

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
     * 构建成功分页结果（无数据）
     */
    public static <T> PageResult<T> createSuccess() {
        return build(BasicErrorCode.SUCCESS, null);
    }

    /**
     * 构建成功分页结果
     *
     * @param data 分页数据
     */
    public static <T> PageResult<T> createSuccess(Collection<T> data) {
        return build(BasicErrorCode.SUCCESS, data);
    }

    /**
     * 构建成功分页结果（含分页元数据）
     *
     * @param data       分页数据
     * @param page       当前页码
     * @param pageSize   每页数量
     * @param totalCount 总条数
     */
    public static <T> PageResult<T> createSuccess(Collection<T> data, int page, int pageSize, int totalCount) {
        PageResult<T> result = build(BasicErrorCode.SUCCESS, data);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotalCount(totalCount);
        return result;
    }

    /**
     * 构建失败分页结果（无数据、默认错误信息）
     */
    public static <T> PageResult<T> createFailure() {
        return build(BasicErrorCode.FAILURE, null);
    }

    /**
     * 构建失败分页结果（自定义错误信息）
     *
     * @param message 错误信息
     */
    public static <T> PageResult<T> createFailure(String message) {
        return build(BasicErrorCode.FAILURE, message, null);
    }

    /**
     * 构建失败分页结果（携带数据）
     *
     * @param data 分页数据
     */
    public static <T> PageResult<T> createFailure(Collection<T> data) {
        return build(BasicErrorCode.FAILURE, data);
    }

    /**
     * 构建失败分页结果（自定义错误信息和数据）
     *
     * @param message 错误信息
     * @param data    分页数据
     */
    public static <T> PageResult<T> createFailure(String message, Collection<T> data) {
        return build(BasicErrorCode.FAILURE, message, data);
    }

    /**
     * 构建自定义错误码分页结果（无数据）
     *
     * @param errorCode 错误码
     */
    public static <T> PageResult<T> create(ErrorCode errorCode) {
        return build(errorCode, null);
    }

    /**
     * 构建自定义错误码分页结果
     *
     * @param errorCode 错误码
     * @param data      分页数据
     */
    public static <T> PageResult<T> create(ErrorCode errorCode, Collection<T> data) {
        return build(errorCode, data);
    }

    /**
     * 构建自定义错误码分页结果（自定义错误信息）
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public static <T> PageResult<T> create(ErrorCode errorCode, String message) {
        return build(errorCode, message, null);
    }

    /**
     * 构建自定义错误码分页结果（自定义错误信息和数据）
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param data      分页数据
     */
    public static <T> PageResult<T> create(ErrorCode errorCode, String message, Collection<T> data) {
        return build(errorCode, message, data);
    }
}
