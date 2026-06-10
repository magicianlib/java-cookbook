package io.ituknown.payload;

import lombok.Getter;

/**
 * 偏移量分页元数据
 * <p>
 * {@code pages}（总页数）由 {@code total} 和 {@code pageSize} 自动计算得出，不需要调用方传入。
 *
 * @author magicianlib@gmail.com
 */
@Getter
public class Pagination {

    /** 总条数 */
    private final long total;
    /** 每页数量 */
    private final int pageSize;
    /** 当前页码（从 1 开始） */
    private final int current;
    /** 总页数（自动计算） */
    private final int pages;

    /**
     * @param total    总条数
     * @param pageSize 每页数量
     * @param current  当前页码（从 1 开始）
     */
    public Pagination(long total, int pageSize, int current) {
        this.total = total;
        this.pageSize = pageSize;
        this.current = current;
        this.pages = pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
    }
}
