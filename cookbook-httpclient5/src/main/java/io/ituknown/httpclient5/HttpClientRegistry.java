package io.ituknown.httpclient5;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.util.concurrent.TimeUnit;

public class HttpClientRegistry {







    }


    // 全局唯一的异步客户端
    public static final CloseableHttpAsyncClient ASYNC_CLIENT = HttpAsyncClients.custom()
            .setIOReactorConfig(IOReactorConfig.custom()
                    .setSoTimeout(Timeout.ofSeconds(5))
                    .build())
            .build();

    static {
        // 异步客户端必须手动启动一次
        ASYNC_CLIENT.start();
    }
}