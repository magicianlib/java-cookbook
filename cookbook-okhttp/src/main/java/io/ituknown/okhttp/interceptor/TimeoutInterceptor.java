package io.ituknown.okhttp.interceptor;

import io.ituknown.okhttp.RequestConfig;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TimeoutInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        RequestConfig tag = request.tag(RequestConfig.class);
        if (tag != null) {
            return chain
                    .withConnectTimeout(tag.getConnectTimeout(), TimeUnit.MILLISECONDS)
                    .withReadTimeout(tag.getReadTimeout(), TimeUnit.MILLISECONDS)
                    .withWriteTimeout(tag.getWriteTimeout(), TimeUnit.MILLISECONDS)
                    .proceed(request);
        }
        return chain.proceed(request);
    }
}