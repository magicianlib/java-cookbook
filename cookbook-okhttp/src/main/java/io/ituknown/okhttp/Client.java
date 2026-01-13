package io.ituknown.okhttp;


import io.ituknown.okhttp.interceptor.RetryInterceptor;
import io.ituknown.okhttp.interceptor.TimeoutInterceptor;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Function;

class Client {
    private static final RequestConfig DEFAULT_CONFIG = new RequestConfig();
    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient.Builder()
            .addInterceptor(new TimeoutInterceptor())
            .addInterceptor(new RetryInterceptor())
            .build();


    public static OkHttpClient newClient(RequestConfig config) {
        OkHttpClient.Builder client = DEFAULT_CLIENT.newBuilder();
        // 代理
        client.proxy(config.getProxy());
        // 拦截器
        config.getInterceptors().forEach(client::addInterceptor);
        // 自定义 Dns
        if (config.getDbs() != null) {
            client.dns(config.getDbs());
        }
        return client.build();
    }

    public static Call execute(RequestConfig config, Request request) throws HttpException {
        return newClient(config).newCall(request);
    }

    public static <R> R execute(Request request, Function<ResponseBody, R> function) throws HttpException {
        return execute(DEFAULT_CONFIG, request, function);
    }

    public static <R> R execute(RequestConfig config, Request request, Function<ResponseBody, R> function) throws HttpException {
        try (Response response = newClient(config).newCall(request).execute()) {
            if (response.isSuccessful()) { // 200~299
                ResponseBody body = response.body();
                return function.apply(body);
            }
            throw new IOException(response.toString());
        } catch (IOException e) {
            throw new HttpException(e);
        }
    }

    public static void asyncExecute(OkHttpClient client, Request request, AsyncCallback callback) {
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                callback.onSuccess(response);
            }
        });
    }
}