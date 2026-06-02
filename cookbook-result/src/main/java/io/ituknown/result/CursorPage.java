package io.ituknown.result;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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

    /**
     * 将数据库多查出的（N+1）条数据，转换为标准游标分页结果。
     * <p>
     * 不会修改传入的原始列表。
     *
     * @param rawList         数据库返回的原始列表（包含可能多出的第 N+1 条）
     * @param pageSize        前端请求的数量 N
     * @param cursorExtractor 从数据对象中提取游标的函数（例如：Article::getId）
     * @param <T>             数据类型
     * @param <C>             游标类型
     * @return 封装好的分页结果
     */
    public static <T, C> CursorPage<T, C> of(
            List<T> rawList, int pageSize, Function<T, C> cursorExtractor
    ) {
        if (rawList == null || rawList.isEmpty()) {
            return new CursorPage<>(List.of(), null, false, pageSize);
        }

        boolean hasMore = rawList.size() > pageSize;
        List<T> data = hasMore ? rawList.subList(0, pageSize) : rawList;

        C nextCursor = hasMore ? cursorExtractor.apply(data.get(data.size() - 1)) : null;
        return new CursorPage<>(data, nextCursor, hasMore, pageSize);
    }
}