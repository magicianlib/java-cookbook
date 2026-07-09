package io.ituknown.httpclient5;

import io.ituknown.httpclient5.response.FileDownloadResponseHandler;
import io.ituknown.httpclient5.response.FileEntityResponse;
import io.ituknown.httpclient5.response.Headers;
import io.ituknown.httpclient5.response.RemoteNameFileDownloadResponseHandler;
import io.ituknown.httpclient5.response.StreamResponseHandler;
import io.ituknown.httpclient5.response.StringEntityResponse;
import io.ituknown.httpclient5.response.StringResponseHandler;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.HttpEntity;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class HttpClientUtils {

    private HttpClientUtils() {
    }

    public static RequestBuilder get(String url) {
        return new RequestBuilder("GET", url);
    }

    public static RequestBuilder post(String url) {
        return new RequestBuilder("POST", url);
    }

    public static final class RequestBuilder {

        private final String method;
        private final String url;
        private HttpEntity entity;
        private HttpRequestConfig config;
        private final Map<String, String> headers = new LinkedHashMap<>();

        RequestBuilder(String method, String url) {
            this.method = method;
            this.url = url;
        }

        public RequestBuilder config(HttpRequestConfig config) {
            this.config = config;
            return this;
        }

        public RequestBuilder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public StringEntityResponse asString() {
            return SyncClient.execute(effectiveConfig(), buildRequest(), new StringResponseHandler());
        }

        public Headers stream(Consumer<InputStream> consumer) {
            return SyncClient.execute(effectiveConfig(), buildRequest(), new StreamResponseHandler(consumer));
        }

        public FileEntityResponse downloadTo(Path path) {
            return SyncClient.execute(effectiveConfig(), buildRequest(), new FileDownloadResponseHandler(path));
        }

        public FileEntityResponse downloadTo(String path) {
            return downloadTo(Path.of(path));
        }

        public FileEntityResponse downloadToRemoteName(Path dir) {
            return SyncClient.execute(effectiveConfig(), buildRequest(), new RemoteNameFileDownloadResponseHandler(dir, url));
        }

        private HttpUriRequestBase buildRequest() {
            if ("GET".equals(method)) {
                return new HttpGet(url);
            }
            HttpPost post = new HttpPost(url);
            if (entity != null) {
                post.setEntity(entity);
            }
            return post;
        }

        private HttpRequestConfig effectiveConfig() {
            HttpRequestConfig base = (config != null) ? config : HttpRequestConfig.DEFAULT;
            HttpRequestConfig effective = base.copy();
            headers.forEach(effective::addHeader);
            return effective;
        }
    }
}
