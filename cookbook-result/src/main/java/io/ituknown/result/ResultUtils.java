package io.ituknown.result;

import java.util.List;

/**
 * 响应结果工厂工具类，支持三种响应模式：
 * <ol>
 *   <li>普通对象 / 集合 — {@link #success(Object)}</li>
 *   <li>偏移量分页 — {@link #successPage}，data 包装为 {@link Page}</li>
 *   <li>游标分页 — {@link #successCursor}，data 包装为 {@link CursorPage}</li>
 * </ol>
 *
 * @author magicianlib@gmail.com
 */
public final class ResultUtils {

    private ResultUtils() {}

    private static <T> Result<T> build(ResultCode resultCode, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.code());
        result.setMsg(msg != null ? msg : resultCode.message());
        result.setData(data);
        return result;
    }

    // ========== Success ==========

    /**
     * 构建成功结果（无数据）
     */
    public static <T> Result<T> success() {
        return build(ResultCodes.SUCCESS, null, null);
    }

    /**
     * 构建成功结果
     *
     * @param data 响应数据
     */
    public static <T> Result<T> success(T data) {
        return build(ResultCodes.SUCCESS, null, data);
    }

    /**
     * 构建成功结果（偏移量分页）
     *
     * @param list     当前页数据
     * @param current  当前页码（从 1 开始）
     * @param pageSize 每页数量
     * @param total    总条数
     */
    public static <T> Result<Page<T>> successPage(List<T> list, int current, int pageSize, long total) {
        return build(ResultCodes.SUCCESS, null, new Page<>(list, current, pageSize, total));
    }

    /**
     * 构建成功结果（游标分页）
     *
     * @param list       当前页数据
     * @param nextCursor 下一页游标，无更多数据时为 null
     * @param hasMore    是否有更多数据
     * @param pageSize   每页数量
     * @param <C>        游标类型
     */
    public static <T, C> Result<CursorPage<T, C>> successCursor(List<T> list, C nextCursor, boolean hasMore, int pageSize) {
        return build(ResultCodes.SUCCESS, null, new CursorPage<>(list, nextCursor, hasMore, pageSize));
    }

    // ========== Failure ==========

    /**
     * 构建失败结果（默认错误信息）
     */
    public static <T> Result<T> failure() {
        return build(ResultCodes.FAILURE, null, null);
    }

    /**
     * 构建失败结果（自定义错误信息）
     *
     * @param msg 错误信息
     */
    public static <T> Result<T> failure(String msg) {
        return build(ResultCodes.FAILURE, msg, null);
    }

    // ========== Custom ResultCode ==========

    /**
     * 构建自定义响应码结果（无数据）
     *
     * @param resultCode 响应码
     */
    public static <T> Result<T> create(ResultCode resultCode) {
        return build(resultCode, null, null);
    }

    /**
     * 构建自定义响应码结果
     *
     * @param resultCode 响应码
     * @param data       响应数据
     */
    public static <T> Result<T> create(ResultCode resultCode, T data) {
        return build(resultCode, null, data);
    }

    /**
     * 构建自定义响应码结果（自定义信息）
     *
     * @param resultCode 响应码
     * @param msg        响应信息
     */
    public static <T> Result<T> create(ResultCode resultCode, String msg) {
        return build(resultCode, msg, null);
    }

    /**
     * 构建自定义响应码结果（自定义信息和数据）
     *
     * @param resultCode 响应码
     * @param msg        响应信息
     * @param data       响应数据
     */
    public static <T> Result<T> create(ResultCode resultCode, String msg, T data) {
        return build(resultCode, msg, data);
    }
}
