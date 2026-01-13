package io.ituknown.okhttp;

import okhttp3.Headers;
import okhttp3.Interceptor;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestConfig {
    /**
     * 是否打印日志
     */
    private boolean needLog;
    /**
     * 失败重试次数
     */
    private int retry;
    /**
     * 连接超时
     */
    private int connectTimeout = 3_000;
    /**
     * 从服务器读取数据的超时时间
     */
    private int readTimeout = 10_000;
    /**
     * 向服务器写入数据的超时时间
     */
    private int writeTimeout = 10_000;
    /**
     * 自定义代理
     * <pre>
     * {@code new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080)}
     * </pre>
     */
    private Proxy proxy;
    /**
     * 请求头
     */
    private final Map<String, String> header = new HashMap<>();
    /**
     * 拦截器
     */
    private final List<Interceptor> interceptors = new ArrayList<>();
    /**
     * 自定义 DNS
     */
    private CustomDns dbs;

    public Map<String, String> getHeader() {
        return header;
    }

    public void addHeader(String name, String value) {
        header.put(name, value);
    }

    public void removeHeader(String name) {
        header.remove(name);
    }

    public Headers toHeaders() {
        return Headers.of(header);
    }

    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public void addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
    }

    public boolean isNeedLog() {
        return needLog;
    }

    public void setNeedLog(boolean needLog) {
        this.needLog = needLog;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public CustomDns getDbs() {
        return dbs;
    }

    public void setDbs(CustomDns dbs) {
        this.dbs = dbs;
    }
}