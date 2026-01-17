package io.ituknown.result;

import java.util.ArrayList;
import java.util.Collection;

public class ListResult<T> extends Result<Collection<T>> {
    public ListResult() {
        super(new ArrayList<>());
    }

    public ListResult(Collection<T> data) {
        super(data);
    }

    private boolean hasMore;

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public void add(T data) {
        getData().add(data);
    }

    public void addAll(Collection<T> data) {
        getData().addAll(data);
    }
}