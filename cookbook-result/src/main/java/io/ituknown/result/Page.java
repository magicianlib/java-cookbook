package io.ituknown.result;

import java.util.List;
import java.util.Objects;

/**
 * 偏移量分页数据封装
 * <p>
 * JSON 结构：
 * <pre>{@code
 * {
 *   "list": [],
 *   "pagination": {
 *     "total": 100,
 *     "pageSize": 10,
 *     "current": 1,
 *     "pages": 10
 *   }
 * }
 * }</pre>
 * <p>
 * {@code list} 为 null 时自动转为空集合；{@code pagination} 不允许为 null。
 *
 * @param <T> 数据元素类型
 * @author magicianlib@gmail.com
 */
public record Page<T>(List<T> list, Pagination pagination) {

    /**
     * 便捷构造器，内部创建 {@link Pagination}
     *
     * @param list     当前页数据，为 null 时自动转为空集合
     * @param current  当前页码（从 1 开始）
     * @param pageSize 每页数量
     * @param total    总条数
     */
    public Page(List<T> list, int current, int pageSize, long total) {
        this(list != null ? list : List.of(), new Pagination(total, pageSize, current));
    }

    /**
     * 原始构造器
     *
     * @param list       当前页数据，为 null 时自动转为空集合
     * @param pagination 分页元数据，不允许为 null
     * @throws NullPointerException pagination 为 null 时
     */
    public Page(List<T> list, Pagination pagination) {
        this.list = list != null ? list : List.of();
        this.pagination = Objects.requireNonNull(pagination);
    }
}