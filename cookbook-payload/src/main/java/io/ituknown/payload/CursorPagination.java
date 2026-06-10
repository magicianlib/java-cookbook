package io.ituknown.payload;

/**
 * 游标分页元数据
 *
 * @param <C>        游标类型（如 {@code String}、{@code Long} 等）
 * @param hasMore    是否有更多数据
 * @param nextCursor 下一页游标，无更多数据时为 null
 * @param pageSize   每页数量
 * @author magicianlib@gmail.com
 */
public record CursorPagination<C>(boolean hasMore, C nextCursor, int pageSize) {

    /**
     * @param hasMore    是否有更多数据
     * @param nextCursor 下一页游标，无更多数据时为 null
     * @param pageSize   每页数量
     */
    public CursorPagination {
    }
}