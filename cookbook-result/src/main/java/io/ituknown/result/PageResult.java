package io.ituknown.result;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class PageResult<T> extends Result<Collection<T>> {
    @Serial
    private static final long serialVersionUID = 9027753304537825727L;

    public PageResult() {
        super(new ArrayList<>());
    }

    public PageResult(Collection<T> data) {
        super(Optional.ofNullable(data).orElse(Collections.emptyList()));
    }

    /**
     * 页码
     */
    private int page;
    /**
     * 每页数量
     */
    private int pageSize;
    /**
     * 总记录数
     */
    private int total;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void add(T data) {
        getData().add(data);
    }

    public void addAll(Collection<T> data) {
        getData().addAll(data);
    }

    public static <T> PageResult<T> create(Status status, String message, Collection<T> data) {
        PageResult<T> result = new PageResult<>(data);
        result.setCode(status.code());
        result.setMessage(message != null && !message.isBlank() ? message : status.message());
        return result;
    }

    public static <T> PageResult<T> success() {
        return create(BasicStatus.SUCCESS, null, null);
    }

    public static <T> PageResult<T> success(Collection<T> data) {
        return create(BasicStatus.SUCCESS, null, data);
    }

    public static <T> PageResult<T> failure() {
        return create(BasicStatus.FAILURE, null, null);
    }

    public static <T> PageResult<T> failure(Status status) {
        return create(status, null, null);
    }

    public static <T> PageResult<T> failure(Status status, String message) {
        return create(status, message, null);
    }
}