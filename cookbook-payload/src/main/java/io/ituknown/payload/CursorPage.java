package io.ituknown.payload;

import java.util.ArrayList;
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
 *     "prevCursor": "eyJpZCI6NX0=",
 *     "nextCursor": "eyJpZCI6MTB9",
 *     "hasPrev": true,
 *     "hasNext": true,
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
     * @param prevCursor 上一页游标，无前一页时为 null
     * @param nextCursor 下一页游标，无下一页时为 null
     * @param hasPrev    是否有前一页
     * @param hasNext    是否有下一页
     * @param pageSize   每页数量
     */
    public CursorPage(List<T> list, C prevCursor, C nextCursor, boolean hasPrev, boolean hasNext, int pageSize) {
        this(list != null ? list : List.of(), new CursorPagination<>(prevCursor, nextCursor, hasPrev, hasNext, pageSize));
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
     * 将数据库多查出的（N+1）条数据，转换为标准游标分页结果（向后翻页）。
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
        return of(rawList, pageSize, cursorExtractor, CursorDirection.FORWARD, null);
    }

    /**
     * 将数据库多查出的（N+1）条数据，转换为标准游标分页结果（支持双向）。
     * <p>
     * 不会修改传入的原始列表。
     *
     * @param rawList         数据库返回的原始列表（包含可能多出的第 N+1 条）
     * @param pageSize        前端请求的数量 N
     * @param cursorExtractor 从数据对象中提取游标的函数（例如：Article::getId）
     * @param direction       翻页方向
     * @param cursor          请求游标（请求参数中的 cursor）
     * @param <T>             数据类型
     * @param <C>             游标类型
     * @return 封装好的分页结果
     */
    public static <T, C> CursorPage<T, C> of(
            List<T> rawList, int pageSize, Function<T, C> cursorExtractor,
            CursorDirection direction, C cursor
    ) {
        if (rawList == null || rawList.isEmpty()) {
            return new CursorPage<>(List.of(), null, null, false, false, pageSize);
        }

        boolean hasMore = rawList.size() > pageSize;
        List<T> data;
        C nextCursor;
        C prevCursor;

        if (direction == CursorDirection.FORWARD) {
            // 下一页查询：WHERE id > :cursor ORDER BY id ASC LIMIT (pageSize+1)
            data = hasMore ? rawList.subList(0, pageSize) : rawList;

            // 游标语义：prevCursor 用于查询上一页，nextCursor 用于查询下一页
            prevCursor = cursor;  // 请求的游标，可用于查询上一页
            nextCursor = hasMore ? cursorExtractor.apply(data.getLast()) : null;
        } else {
            // 上一页查询：WHERE id < :cursor ORDER BY id DESC LIMIT (pageSize+1)
            // 如果有 N+1 条，说明还有前一页
            int fromIndex = hasMore ? 1 : 0;
            data = rawList.subList(fromIndex, rawList.size());
            // 反向数据需要反转，保证顺序正确
            List<T> reversedData = new ArrayList<>(data);
            java.util.Collections.reverse(reversedData);
            data = java.util.Collections.unmodifiableList(reversedData);

            // 游标语义：prevCursor 用于查询上一页，nextCursor 用于查询下一页
            nextCursor = cursor;  // 请求的游标，可用于查询下一页
            prevCursor = hasMore ? cursorExtractor.apply(data.getFirst()) : null;
        }

        // 判断是否有前后页：基于游标是否为 null
        boolean hasPrev = prevCursor != null;
        boolean hasNext = nextCursor != null;

        return new CursorPage<>(data, prevCursor, nextCursor, hasPrev, hasNext, pageSize);
    }
}