package io.ituknown.httpclient5;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.core5.http.HttpHost;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 单次 HTTP 请求的可选配置聚合体：重定向、超时、代理、认证、请求头。
 *
 * <p>命名上刻意区别于 Apache HttpClient 5 自带的 {@code RequestConfig} —— 本类是对其
 * （外加 headers / credentials）的一层高层封装。
 *
 * <p>{@link #DEFAULT} 是全局共享的不可变实例：构造后即被 {@linkplain #freeze() 冻结}，
 * 任何修改都会抛出 {@link UnsupportedOperationException}。定制时应 {@link #copy()} 出一份
 * 可变副本后再修改。
 */
@Getter
public class RequestOptions {
    static final RequestOptions DEFAULT;

    static {
        DEFAULT = new RequestOptions();
        DEFAULT.freeze();
    }

    /**
     * 是否跟随重定向
     */
    private boolean followRedirects = true;
    /**
     * 从连接池获取连接超时时间（毫秒）
     */
    private long connectionRequestTimeout = 200L;
    /**
     * 等待目标服务器数据响应超时时间（毫秒）
     */
    private long responseTimeout = 3_000L;
    /**
     * 请求代理服务器（已解析的 HttpHost，{@code null} 表示不使用代理）
     */
    private HttpHost proxy;
    /**
     * 请求认证
     */
    private CredentialsProvider credentials;
    /**
     * 请求头
     */
    @Getter(AccessLevel.NONE)
    private final Map<String, String> headers = new HashMap<>();

    /**
     * 实例是否已冻结（不可变）。仅 {@link #DEFAULT} 会冻结；{@link #copy()} 产出的实例保持可变。
     */
    @Getter(AccessLevel.NONE)
    private boolean frozen;

    public void setFollowRedirects(boolean followRedirects) {
        checkMutable();
        this.followRedirects = followRedirects;
    }

    public void setConnectionRequestTimeout(long connectionRequestTimeout) {
        checkMutable();
        if (connectionRequestTimeout < 0) {
            throw new IllegalArgumentException("connectionRequestTimeout must be >= 0");
        }
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public void setResponseTimeout(long responseTimeout) {
        checkMutable();
        if (responseTimeout < 0) {
            throw new IllegalArgumentException("responseTimeout must be >= 0");
        }
        this.responseTimeout = responseTimeout;
    }

    /**
     * 设置代理。接受 {@code host:port} 或 {@code scheme://host:port} 形式，非法格式在调用时
     * 即抛出 {@link IllegalArgumentException}（早失败），避免推迟到请求发起时才暴露。
     * 传入 {@code null} 或空白字符串则清除代理。
     */
    public void setProxy(String proxy) {
        checkMutable();
        if (proxy == null || proxy.isBlank()) {
            this.proxy = null;
            return;
        }
        try {
            this.proxy = HttpHost.create(proxy);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid proxy '" + proxy + "': " + e.getMessage(), e);
        }
    }

    /**
     * 直接以已解析的 {@link HttpHost} 设置代理。
     */
    public void setProxy(HttpHost proxy) {
        checkMutable();
        this.proxy = proxy;
    }

    public void setCredentials(CredentialsProvider credentials) {
        checkMutable();
        this.credentials = credentials;
    }

    public void addHeader(String key, String value) {
        checkMutable();
        headers.put(key, value);
    }

    /**
     * 返回请求头的不可变视图，外部无法绕过 {@link #addHeader} 修改内部状态。
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * 返回一份可变的独立副本。{@link #DEFAULT} 不可变，定制时应 copy 后再修改。
     * 注意：{@code proxy} 与 {@code credentials} 为不可变/只读语义，按引用共享，不做深拷贝。
     */
    public RequestOptions copy() {
        RequestOptions c = new RequestOptions();
        c.followRedirects = this.followRedirects;
        c.connectionRequestTimeout = this.connectionRequestTimeout;
        c.responseTimeout = this.responseTimeout;
        c.proxy = this.proxy;
        c.credentials = this.credentials;
        c.headers.putAll(this.headers);
        return c;
    }

    private void checkMutable() {
        if (frozen) {
            throw new UnsupportedOperationException(
                    "This RequestOptions is immutable (frozen); call copy() to obtain a mutable instance");
        }
    }

    private void freeze() {
        this.frozen = true;
    }
}
