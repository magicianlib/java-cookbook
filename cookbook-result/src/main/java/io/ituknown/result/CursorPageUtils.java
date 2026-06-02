package io.ituknown.result;

import java.util.List;
import java.util.function.Function;

/**
 * 游标分页工具类
 *
 * @author magicianlib@gmail.com
 */
public class CursorPageUtils {

    private CursorPageUtils() {
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
