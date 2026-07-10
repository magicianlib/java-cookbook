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
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
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

    private static ContentType guessContentType(File file) {
        String type = URLConnection.guessContentTypeFromName(file.getName());
        if (type == null) {
            type = "application/octet-stream";
        }
        return ContentType.create(type);
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

        // ---- body setter (仅 POST) ----

        public RequestBuilder json(String json) {
            return setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        }

        public RequestBuilder json(byte[] json) {
            return setEntity(new ByteArrayEntity(json, ContentType.APPLICATION_JSON));
        }

        public RequestBuilder form(List<? extends NameValuePair> params) {
            return form(params, StandardCharsets.UTF_8);
        }

        public RequestBuilder form(List<? extends NameValuePair> params, Charset charset) {
            return setEntity(new UrlEncodedFormEntity(params, charset));
        }

        public RequestBuilder multipart(MultipartEntityBuilder builder) {
            return setEntity(builder.build());
        }

        public RequestBuilder file(File file) {
            return file(file, guessContentType(file));
        }

        public RequestBuilder file(File file, ContentType contentType) {
            return setEntity(new FileEntity(file, contentType));
        }

        public RequestBuilder body(String content, ContentType contentType) {
            return setEntity(new StringEntity(content, contentType));
        }

        public RequestBuilder body(byte[] content, ContentType contentType) {
            return setEntity(new ByteArrayEntity(content, contentType));
        }

        private RequestBuilder setEntity(HttpEntity entity) {
            if (!"POST".equals(method)) {
                throw new IllegalStateException("GET request does not support a request body");
            }
            this.entity = entity;
            return this;
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
            HttpRequestConfig base;
            if (config != null) {
                base = config;
            } else {
                base = HttpRequestConfig.DEFAULT;
            }
            HttpRequestConfig effective = base.copy();
            headers.forEach(effective::addHeader);
            return effective;
        }
    }
}
