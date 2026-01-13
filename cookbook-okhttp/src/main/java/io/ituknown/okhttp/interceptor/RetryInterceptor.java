package io.ituknown.okhttp.interceptor;

import io.ituknown.okhttp.RequestConfig;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class RetryInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        RequestConfig tag = request.tag(RequestConfig.class);
        if (tag != null) {
            int retry = tag.getRetry();
            while (--retry >= 0) {
                try {
                    return chain.proceed(request);
                } catch (IOException e) {
                    // ignore exception
                }
            }
        }
        return chain.proceed(request);
    }
}