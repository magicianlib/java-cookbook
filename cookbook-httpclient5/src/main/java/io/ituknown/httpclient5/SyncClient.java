package io.ituknown.httpclient5;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class SyncClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncClient.class);
    /**
     * 默认连接配置
     */
    private static final ConnectionConfig DEFAULT_CONNECTION_CONFIG = ConnectionConfig.custom()
            // 连接超时
            .setConnectTimeout(5L, TimeUnit.SECONDS)
            // 读取超时
            .setSocketTimeout(10, TimeUnit.SECONDS)
            // 连接复用有效时间
            .setValidateAfterInactivity(3L, TimeUnit.SECONDS)
            // 连接最大空闲时间, 释放长期不用的资源(超过此时间会被标记为失效并由后台清理线程移除, 可以避免僵尸连接)
            // 需要配合 HttpClientBuilder#evictIdleConnections 使用
            .setIdleTimeout(30L, TimeUnit.SECONDS)
            // 设置连接总生存时间, 在动态 DNS 或集群环境下定期重新解析 IP
            .setTimeToLive(30L, TimeUnit.MINUTES)
            .build();

    /**
     * TLS 安全配置 - L7 层
     */
    private static final TlsConfig TLS_CONFIG = TlsConfig.custom()
            .setSupportedProtocols(TLS.V_1_2, TLS.V_1_3) // 限制使用的 TLS 版本, 可用于禁用不安全的版本
            .setHandshakeTimeout(Timeout.ofSeconds(5)) // TLS 握手的超时时间
            .build();

    /**
     * 套接字配置 - L4 层
     */
    private static final SocketConfig SOCKET_CONFIG = SocketConfig.custom()
            .setSoTimeout(Timeout.ofSeconds(5))
            .setTcpNoDelay(true)
            .build();

    /**
     * 连接池配置
     */
    private static final PoolingHttpClientConnectionManager CONN_MANAGER = new PoolingHttpClientConnectionManager();

    static {
        CONN_MANAGER.setMaxTotal(200); // 连接池允许最大总链接数
        CONN_MANAGER.setDefaultMaxPerRoute(20); // 每个目标机器并发限制
        CONN_MANAGER.setDefaultConnectionConfig(DEFAULT_CONNECTION_CONFIG); // 默认连接配置
        //CONN_MANAGER.setDefaultTlsConfig(TLS_CONFIG); // TLS 安全配置
        //CONN_MANAGER.setDefaultSocketConfig(SOCKET_CONFIG); // 套接字配置
    }

    /**
     * 全局唯一的同步客户端
     */
    private static final CloseableHttpClient INSTANCE = HttpClients.custom()
            // 启动一个后台线程, 每隔 10 秒扫描一次连接池. 强制关闭超过 ConnectionConfig#setIdleTimeout 定义的闲置连接
            .evictIdleConnections(TimeValue.ofSeconds(10L))
            .setConnectionManager(CONN_MANAGER)
            .build();

    static <T> T execute(CustomRequestConfig customConfig, HttpUriRequestBase requestBase, HttpClientResponseHandler<T> responseHandler) {
        URI url = null;

        try {
            url = requestBase.getUri();

            RequestConfig.Builder builder = RequestConfig.custom();

            // 代理
            if (customConfig.getProxy() != null && !customConfig.getProxy().isBlank()) {
                builder.setProxy(HttpHost.create(customConfig.getProxy()));
            }

            // 重定向
            builder.setRedirectsEnabled(customConfig.isRedirects());
            // 获取连接超时时间
            builder.setConnectionRequestTimeout(customConfig.getConnectionRequestTimeout(), TimeUnit.MILLISECONDS);
            // 服务器响应超时时间
            builder.setResponseTimeout(customConfig.getResponseTimeout(), TimeUnit.MILLISECONDS);

            requestBase.setConfig(builder.build());

            // 认证凭证
            HttpClientContext context = HttpClientContext.create();
            if (customConfig.getCredentials() != null) {
                context.setCredentialsProvider(customConfig.getCredentials());
            }

            // 请求头
            for (Map.Entry<String, Object> entry : customConfig.getHeaders().entrySet()) {
                requestBase.setHeader(entry.getKey(), entry.getValue());
            }

            if (customConfig.isPrintLog()) {
                LOGGER.info("http request: {} {}, header: {}", requestBase.getMethod(), url, customConfig.getHeaders());
            }

            return INSTANCE.execute(requestBase, context, responseHandler);
        } catch (Exception e) {
            LOGGER.error("Http request exception[{} {}]: {}", requestBase.getMethod(), url, e.getMessage(), e);
            throw new HttpException(e);
        }
    }
}