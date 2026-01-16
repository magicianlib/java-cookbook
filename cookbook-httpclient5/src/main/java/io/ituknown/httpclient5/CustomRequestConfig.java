package io.ituknown.httpclient5;

import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;

import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class CustomRequestConfig {
    static final CustomRequestConfig DEFAULT = new CustomRequestConfig();
    /**
     * 是否允许请求重定向
     */
    private boolean redirects = true;
    /**
     * 从连接池获取连接超时时间（毫秒）
     * <p>
     * 如果池中的最大连接数已满，且所有连接都在被其他请求占用，那么当前的请求就必须进入队列等待。
     * <p>
     * 如果超过这个时间还没有连接可用，会抛出 {@link ConnectionRequestTimeoutException}
     */
    private long connectionRequestTimeout = 200L;
    /**
     * 等待目标服务器数据响应超时时间（毫秒）
     * <p>
     * 连接已经建立，请求也已经发给服务器了，现在 HttpClient 正在等待服务器吐出数据。
     * <p>
     * 根据业务接口响应速度来定。比如一个支付回调可能需要 10 秒，而一个心跳接口通常只需 500 毫秒。
     * <p>
     * 如果超过这个时间还未相应，会抛出 {@link SocketTimeoutException}
     */
    private long responseTimeout = 3_000L;
    /**
     * 请求代理服务器
     * <p>
     * 请求可能需要走特定的代理服务器才能正常访问，可以是 IP:PORT 也可以是具体域名。
     * <p>
     * 如果代理服务器无法正常解析，会抛出 {@link URISyntaxException}
     */
    private String proxy;
    /**
     * 请求认证
     * <p>
     *
     * @see BasicCredentialsProvider
     */
    private CredentialsProvider credentials;
    /**
     * 请求头
     */
    private final Map<String, Object> headers = new HashMap<>();

    public boolean isRedirects() {
        return redirects;
    }

    public void setRedirects(boolean redirects) {
        this.redirects = redirects;
    }

    public long getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(long connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public long getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(long responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public CredentialsProvider getCredentials() {
        return credentials;
    }

    public void setCredentials(CredentialsProvider credentials) {
        this.credentials = credentials;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void addHeader(String key, Object value) {
        headers.put(key, value);
    }
}