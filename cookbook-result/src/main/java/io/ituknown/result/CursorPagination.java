package io.ituknown.result;

import lombok.Getter;

/**
 * 游标分页元数据
 *
 * @param <C> 游标类型（如 {@code String}、{@code Long} 等）
 * @author magicianlib@gmail.com
 */
@Getter
public class CursorPagination<C> {

    /** 是否有更多数据 */
    private final boolean hasMore;
    /** 下一页游标，无更多数据时为 null */
    private final C nextCursor;
    /** 每页数量 */
    private final int pageSize;

    /**
     * @param hasMore    是否有更多数据
     * @param nextCursor 下一页游标，无更多数据时为 null
     * @param pageSize   每页数量
     */
    public CursorPagination(boolean hasMore, C nextCursor, int pageSize) {
        this.hasMore = hasMore;
        this.nextCursor = nextCursor;
        this.pageSize = pageSize;
    }
}
