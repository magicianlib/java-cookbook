package io.ituknown.okhttp;

import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * 异步请求响应处理
 */
public interface AsyncCallback {
    /**
     * 请求失败
     */
    void onFailure(@NotNull IOException e);

    /**
     * 请求执行成功
     */
    void onSuccess(@NotNull Response response);
}