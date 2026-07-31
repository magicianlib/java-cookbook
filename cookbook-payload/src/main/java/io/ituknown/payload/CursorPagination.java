package io.ituknown.payload;

/**
 * 游标分页元数据
 *
 * @param <C>        游标类型（如 {@code String}、{@code Long} 等）
 * @param prevCursor 上一页游标，无前一页时为 null
 * @param nextCursor 下一页游标，无下一页时为 null
 * @param hasPrev    是否有前一页
 * @param hasNext    是否有下一页
 * @param pageSize   每页数量
 * @author magicianlib@gmail.com
 */
public record CursorPagination<C>(C prevCursor, C nextCursor, boolean hasPrev, boolean hasNext, int pageSize) {

    /**
     * @param prevCursor 上一页游标，无前一页时为 null
     * @param nextCursor 下一页游标，无下一页时为 null
     * @param hasPrev    是否有前一页
     * @param hasNext    是否有下一页
     * @param pageSize   每页数量
     */
    public CursorPagination {
    }
}