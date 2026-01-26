package io.ituknown.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * 消息结果
 *
 * @author magicianlib@gmail.com
 */
@Getter
@Setter
public class PageResult<T> extends Result<Collection<T>> {
    /**
     * 页码
     */
    private int page;
    /**
     * 每页数据量
     */
    private int pageSize;
    /**
     * 总数量
     */
    private int totalCount;
    /**
     * 总页数
     */
    @Setter(AccessLevel.NONE)
    private int totalPage;

    public int getTotalPage() {
        if (totalPage != 0) {
            totalPage = totalCount / pageSize;
            if (totalCount % pageSize != 0) {
                ++totalPage;
            }
        }
        return totalPage;
    }

    public void add(T data) {
        initializeCollectionIfNeed(ArrayList::new).add(data);
    }

    public void add(T data, Supplier<Collection<T>> initCollection) {
        initializeCollectionIfNeed(initCollection).add(data);
    }

    public void addAll(Collection<T> data) {
        initializeCollectionIfNeed(ArrayList::new).addAll(data);
    }

    public void addAll(Collection<T> data, Supplier<Collection<T>> initCollection) {
        initializeCollectionIfNeed(initCollection).addAll(data);
    }

    public Collection<T> initializeCollectionIfNeed(Supplier<Collection<T>> initCollection) {
        {
            Collection<T> collection = this.getData();
            if (collection == null) {
                setData(initCollection.get());
            }
        }
        return this.getData();
    }
}