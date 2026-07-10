package io.ituknown.httpclient5;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
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
     * 连接池配置
     */
    private static final PoolingHttpClientConnectionManager CONN_MANAGER = new PoolingHttpClientConnectionManager();

    static {
        CONN_MANAGER.setMaxTotal(200); // 连接池允许最大总链接数
        CONN_MANAGER.setDefaultMaxPerRoute(20); // 每个目标机器并发限制
        CONN_MANAGER.setDefaultConnectionConfig(DEFAULT_CONNECTION_CONFIG); // 默认连接配置
    }

    /**
     * 全局唯一的同步客户端
     */
    private static final CloseableHttpClient INSTANCE = HttpClients.custom()
            // 启动一个后台线程, 每隔 10 秒扫描一次连接池. 强制关闭超过 ConnectionConfig#setIdleTimeout 定义的闲置连接
            .evictIdleConnections(TimeValue.ofSeconds(10L))
            .setConnectionManager(CONN_MANAGER)
            .build();

    static <T> T execute(HttpRequestConfig config, HttpUriRequestBase requestBase, HttpClientResponseHandler<T> responseHandler) {
        URI url = null;

        try {
            url = requestBase.getUri();

            RequestConfig.Builder builder = RequestConfig.custom();

            // 代理
            if (config.getProxy() != null && !config.getProxy().isBlank()) {
                builder.setProxy(HttpHost.create(config.getProxy()));
            }

            // 重定向
            builder.setRedirectsEnabled(config.isRedirects());
            // 获取连接超时时间
            builder.setConnectionRequestTimeout(config.getConnectionRequestTimeout(), TimeUnit.MILLISECONDS);
            // 服务器响应超时时间
            builder.setResponseTimeout(config.getResponseTimeout(), TimeUnit.MILLISECONDS);

            requestBase.setConfig(builder.build());

            // 认证凭证
            HttpClientContext context = HttpClientContext.create();
            if (config.getCredentials() != null) {
                context.setCredentialsProvider(config.getCredentials());
            }

            // 请求头
            for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
                requestBase.setHeader(entry.getKey(), entry.getValue());
            }

            if (LOGGER.isInfoEnabled()) {
                StringJoiner headerJoiner = new StringJoiner(", ");
                for (Header header : requestBase.getHeaders()) {
                    headerJoiner.add(header.getName() + ": " + header.getValue());
                }

                String payload = resolvePayloadForLog(requestBase.getEntity());

                String proxy = config.getProxy();
                if (proxy == null) {
                    proxy = "NONE";
                }

                LOGGER.info("HTTP Request: [{} {}] | Payload: {} | Timeout: {}ms | Proxy: {} | Headers: [{}]",
                        requestBase.getMethod(),
                        url,
                        payload,
                        config.getResponseTimeout(),
                        proxy,
                        headerJoiner
                );
            }

            return INSTANCE.execute(requestBase, context, responseHandler);
        } catch (Exception e) {
            String failedUrl;
            if (url != null) {
                failedUrl = url.toString();
            } else {
                failedUrl = "Unknown URI";
            }

            LOGGER.error("HTTP Execution Error [{} {}]: {}",
                    requestBase.getMethod(),
                    failedUrl,
                    e.getMessage(),
                    e);

            throw new HttpException(e);
        }
    }

    /**
     * 为日志生成请求体摘要：最多读取 1000 字节，避免大文件全量加载进内存。
     * 非 repeatable 或非文本 content-type 不读取，直接返回占位符。
     */
    static String resolvePayloadForLog(HttpEntity entity) {
        if (entity == null || !entity.isRepeatable()) {
            return "Binary/Large Content";
        }
        if (!isTextLike(entity)) {
            return "Binary/Large Content";
        }
        try (InputStream in = entity.getContent()) {
            byte[] sample = in.readNBytes(1000);
            if (sample.length == 0) {
                return "";
            }
            String content = new String(sample, StandardCharsets.UTF_8);
            long total = entity.getContentLength();
            if (total > 1000 || (total < 0 && sample.length == 1000)) {
                String totalLabel;
                if (total > 0) {
                    totalLabel = Long.toString(total);
                } else {
                    totalLabel = "unknown";
                }
                return content + "... [truncated, total: " + totalLabel + "]";
            }
            return content;
        } catch (Exception e) {
            return "[Error reading payload: " + e.getMessage() + "]";
        }
    }

    private static boolean isTextLike(HttpEntity entity) {
        String ct = entity.getContentType();
        if (ct == null) {
            return false;
        }
        String lower = ct.toLowerCase(Locale.ROOT);
        return lower.startsWith("text/")
                || lower.contains("application/json")
                || lower.contains("application/xml")
                || lower.contains("+json")
                || lower.contains("+xml")
                || lower.contains("application/x-www-form-urlencoded");
    }
}
