package io.ituknown.payload;

/**
 * 游标分页元数据
 * <p>
 * <b>跨进程序列化注意：</b>本类为 Java 记录类型。通过 Dubbo 远程调用传输
 * （作为方法参数或返回值）时，提供端与消费端均需使用 Apache Dubbo 3.3.0 及以上版本；
 * 自该版本起内置的 Hessian2 序列化才支持记录类型的序列化往返，更低版本会在
 * 消费端反序列化失败（提示无法实例化）。
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