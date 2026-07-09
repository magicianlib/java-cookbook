package io.ituknown.httpclient5;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.auth.CredentialsProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class HttpRequestConfig {
    static final HttpRequestConfig DEFAULT;

    static {
        DEFAULT = new HttpRequestConfig();
        DEFAULT.headers = Collections.unmodifiableMap(DEFAULT.headers);
    }

    /**
     * 是否允许请求重定向
     */
    private boolean redirects = true;
    /**
     * 从连接池获取连接超时时间（毫秒）
     */
    private long connectionRequestTimeout = 200L;
    /**
     * 等待目标服务器数据响应超时时间（毫秒）
     */
    private long responseTimeout = 3_000L;
    /**
     * 请求代理服务器
     */
    private String proxy;
    /**
     * 请求认证
     */
    private CredentialsProvider credentials;
    /**
     * 请求头
     */
    @Setter(AccessLevel.NONE)
    private Map<String, String> headers = new HashMap<>();

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * 返回一份可变的独立副本。DEFAULT 不可变，定制时应 copy 后再修改。
     */
    public HttpRequestConfig copy() {
        HttpRequestConfig c = new HttpRequestConfig();
        c.redirects = this.redirects;
        c.connectionRequestTimeout = this.connectionRequestTimeout;
        c.responseTimeout = this.responseTimeout;
        c.proxy = this.proxy;
        c.credentials = this.credentials;
        c.headers.putAll(this.headers);
        return c;
    }
}
