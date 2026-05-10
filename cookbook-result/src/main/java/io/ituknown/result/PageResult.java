package io.ituknown.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * 分页响应结果封装
 *
 * @param <T> 分页数据元素类型
 * @author magicianlib@gmail.com
 */
@Getter
@Setter
public class PageResult<T> extends Result<Collection<T>> {
    /**
     * 当前页码（从 1 开始）
     */
    private int page;
    /**
     * 每页数据量（最小值为 1）
     */
    private int pageSize;
    /**
     * 数据总条数
     */
    private int totalCount;
    /**
     * 总页数（由 totalCount 和 pageSize 计算得出）
     */
    @Setter(AccessLevel.NONE)
    private int totalPage;

    /**
     * 计算并返回总页数
     *
     * @return 总页数，当 pageSize <= 0 时返回 0
     */
    public int getTotalPage() {
        if (pageSize <= 0) {
            return 0;
        }
        totalPage = totalCount / pageSize;
        if (totalCount % pageSize != 0) {
            totalPage++;
        }
        return totalPage;
    }

    /**
     * 添加单条数据（内部使用 ArrayList 初始化集合）
     *
     * @param data 要添加的数据
     */
    public void add(T data) {
        initializeCollectionIfNeeded(ArrayList::new).add(data);
    }

    /**
     * 添加单条数据（使用自定义集合初始化）
     *
     * @param data           要添加的数据
     * @param initCollection 集合初始化函数
     */
    public void add(T data, Supplier<Collection<T>> initCollection) {
        initializeCollectionIfNeeded(initCollection).add(data);
    }

    /**
     * 批量添加数据（内部使用 ArrayList 初始化集合）
     *
     * @param data 要添加的数据集合
     */
    public void addAll(Collection<T> data) {
        initializeCollectionIfNeeded(ArrayList::new).addAll(data);
    }

    /**
     * 批量添加数据（使用自定义集合初始化）
     *
     * @param data           要添加的数据集合
     * @param initCollection 集合初始化函数
     */
    public void addAll(Collection<T> data, Supplier<Collection<T>> initCollection) {
        initializeCollectionIfNeeded(initCollection).addAll(data);
    }

    private Collection<T> initializeCollectionIfNeeded(Supplier<Collection<T>> initCollection) {
        Collection<T> collection = this.getData();
        if (collection == null) {
            setData(initCollection.get());
        }
        return this.getData();
    }
}
