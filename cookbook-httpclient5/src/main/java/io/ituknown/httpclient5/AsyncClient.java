package io.ituknown.httpclient5;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;

class AsyncClient {
    static final CloseableHttpAsyncClient INSTANCE = HttpAsyncClients.custom()
            .setIOReactorConfig(IOReactorConfig.custom()
                    .setSoTimeout(Timeout.ofSeconds(5))
                    .build())
            .build();

    static {
        // 异步客户端必须手动启动一次
        INSTANCE.start();
    }
}