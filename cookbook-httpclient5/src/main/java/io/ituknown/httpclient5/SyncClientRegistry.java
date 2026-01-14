package io.ituknown.httpclient5;

import io.ituknown.httpclient5.response.StringHttpClientResponseHandler;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

class SyncClientRegistry {
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

    // 全局唯一的同步客户端
    static final CloseableHttpClient SYNC_CLIENT = HttpClients.custom()
            // 启动一个后台线程, 每隔 10 秒扫描一次连接池. 强制关闭超过 ConnectionConfig#setIdleTimeout 定义的闲置连接
            .evictIdleConnections(TimeValue.ofSeconds(10L))
            .setConnectionManager(CONN_MANAGER)
            .build();

    {
        CredentialsProvider provider = new BasicCredentialsProvider();
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(provider); // 认证凭证

        HttpHost proxy = null; // 代理
        try {
            proxy = HttpHost.create("");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        RequestConfig config = RequestConfig.custom().setProxy(proxy).setRedirectsEnabled(true).build();

        HttpGet request = new HttpGet("https://target-site.com");
        request.setConfig(config);

        // 使用 HttpClientResponseHandler 会自动处理连接的释放
        try {
            StringResponse response = SYNC_CLIENT.execute(request, context, new StringHttpClientResponseHandler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}