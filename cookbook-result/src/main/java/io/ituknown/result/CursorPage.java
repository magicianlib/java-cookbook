package io.ituknown.result;

import java.util.List;
import java.util.Objects;

/**
 * 游标分页数据封装
 * <p>
 * JSON 结构：
 * <pre>{@code
 * {
 *   "list": [],
 *   "pagination": {
 *     "hasMore": true,
 *     "nextCursor": "eyJpZCI6MTB9",
 *     "pageSize": 20
 *   }
 * }
 * }</pre>
 * <p>
 * {@code list} 为 null 时自动转为空集合；{@code pagination} 不允许为 null。
 *
 * @param <T> 数据元素类型
 * @param <C> 游标类型（如 {@code String}、{@code Long} 等）
 * @author magicianlib@gmail.com
 */
public record CursorPage<T, C>(List<T> list, CursorPagination<C> pagination) {

    /**
     * 便捷构造器，内部创建 {@link CursorPagination}
     *
     * @param list       当前页数据，为 null 时自动转为空集合
     * @param nextCursor 下一页游标，无更多数据时为 null
     * @param hasMore    是否有更多数据
     * @param pageSize   每页数量
     */
    public CursorPage(List<T> list, C nextCursor, boolean hasMore, int pageSize) {
        this(list != null ? list : List.of(), new CursorPagination<>(hasMore, nextCursor, pageSize));
    }

    /**
     * 原始构造器
     *
     * @param list       当前页数据，为 null 时自动转为空集合
     * @param pagination 分页元数据，不允许为 null
     * @throws NullPointerException pagination 为 null 时
     */
    public CursorPage(List<T> list, CursorPagination<C> pagination) {
        this.list = list != null ? list : List.of();
        this.pagination = Objects.requireNonNull(pagination);
    }
}