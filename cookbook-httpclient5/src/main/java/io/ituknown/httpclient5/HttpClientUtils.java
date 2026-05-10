package io.ituknown.httpclient5;

import io.ituknown.httpclient5.response.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
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
import java.util.List;
import java.util.function.Consumer;

public class HttpClientUtils {

    private static ContentType guessContentType(File file) {
        String type = URLConnection.guessContentTypeFromName(file.getName());
        return ContentType.create(type != null ? type : "application/octet-stream");
    }

    /**
     * Get 请求
     */
    public static StringEntityResponse get(String url) {
        return get(url, HttpRequestConfig.DEFAULT);
    }

    public static StringEntityResponse get(String url, HttpRequestConfig config) {
        HttpGet request = new HttpGet(url);
        return SyncClient.execute(config, request, new StringResponseHandler());
    }

    public static Headers getStream(String url, Consumer<InputStream> streamConsumer) {
        return getStream(url, HttpRequestConfig.DEFAULT, streamConsumer);
    }

    public static Headers getStream(String url, HttpRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpGet request = new HttpGet(url);
        return SyncClient.execute(config, request, new StreamResponseHandler(streamConsumer));
    }

    public static FileEntityResponse download(String url, String filePath) {
        return download(url, HttpRequestConfig.DEFAULT, filePath);
    }

    public static FileEntityResponse download(String url, HttpRequestConfig config, String filePath) {
        HttpGet request = new HttpGet(url);
        return SyncClient.execute(config, request, new FileDownloadResponseHandler(filePath));
    }

    public static FileEntityResponse downloadUseRemoteName(String url, Path dir) {
        return downloadUseRemoteName(url, HttpRequestConfig.DEFAULT, dir);
    }

    public static FileEntityResponse downloadUseRemoteName(String url, HttpRequestConfig config, Path dir) {
        HttpGet request = new HttpGet(url);
        return SyncClient.execute(config, request, new RemoteNameFileDownloadResponseHandler(dir, url));
    }

    /**
     * Post JSON 请求
     */
    public static StringEntityResponse postJson(String url, String jsonContent) {
        return postJson(url, jsonContent, HttpRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postJson(String url, String jsonContent, HttpRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(jsonContent, ContentType.APPLICATION_JSON));
        return SyncClient.execute(config, request, new StringResponseHandler());
    }

    public static Headers postJson(String url, String jsonContent, Consumer<InputStream> streamConsumer) {
        return postJson(url, jsonContent, HttpRequestConfig.DEFAULT, streamConsumer);
    }

    public static Headers postJson(String url, String jsonContent, HttpRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(jsonContent, ContentType.APPLICATION_JSON));
        return SyncClient.execute(config, request, new StreamResponseHandler(streamConsumer));
    }

    public static StringEntityResponse postJson(String url, byte[] jsonContent) {
        return postJson(url, jsonContent, HttpRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postJson(String url, byte[] jsonContent, HttpRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(jsonContent, ContentType.APPLICATION_JSON));
        return SyncClient.execute(config, request, new StringResponseHandler());
    }

    public static Headers postJson(String url, byte[] jsonContent, Consumer<InputStream> streamConsumer) {
        return postJson(url, jsonContent, HttpRequestConfig.DEFAULT, streamConsumer);
    }

    public static Headers postJson(String url, byte[] jsonContent, HttpRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(jsonContent, ContentType.APPLICATION_JSON));
        return SyncClient.execute(config, request, new StreamResponseHandler(streamConsumer));
    }

    /**
     * Post 请求
     */
    public static StringEntityResponse post(String url, String content, ContentType contentType) {
        return post(url, content, contentType, HttpRequestConfig.DEFAULT);
    }

    public static StringEntityResponse post(String url, String content, ContentType contentType, HttpRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(content, contentType));
        return SyncClient.execute(config, request, new StringResponseHandler());
    }

    public static Headers post(String url, String content, ContentType contentType, Consumer<InputStream> streamConsumer) {
        return post(url, content, contentType, HttpRequestConfig.DEFAULT, streamConsumer);
    }

    public static Headers post(String url, String content, ContentType contentType, HttpRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(content, contentType));
        return SyncClient.execute(config, request, new StreamResponseHandler(streamConsumer));
    }

    public static StringEntityResponse post(String url, byte[] content, ContentType contentType) {
        return post(url, content, contentType, HttpRequestConfig.DEFAULT, StandardCharsets.UTF_8);
    }

    public static StringEntityResponse post(String url, byte[] content, ContentType contentType, HttpRequestConfig config) {
        return post(url, content, contentType, config, StandardCharsets.UTF_8);
    }

    public static StringEntityResponse post(String url, byte[] content, ContentType contentType, HttpRequestConfig config, Charset charset) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(content, contentType));
        return SyncClient.execute(config, request, new StringResponseHandler());
    }

    public static Headers post(String url, byte[] content, ContentType contentType, Consumer<InputStream> streamConsumer) {
        return post(url, content, contentType, HttpRequestConfig.DEFAULT, streamConsumer);
    }

    public static Headers post(String url, byte[] content, ContentType contentType, HttpRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(content, contentType));
        return SyncClient.execute(config, request, new StreamResponseHandler(streamConsumer));
    }

    /**
     * Post Form 请求
     */
    public static <T extends NameValuePair> StringEntityResponse postForm(String url, final List<T> parameters) {
        return postForm(url, parameters, HttpRequestConfig.DEFAULT, StandardCharsets.UTF_8);
    }

    public static <T extends NameValuePair> StringEntityResponse postForm(String url, final List<T> parameters, HttpRequestConfig config) {
        return postForm(url, parameters, config, StandardCharsets.UTF_8);
    }

    public static <T extends NameValuePair> StringEntityResponse postForm(String url, final List<T> parameters, HttpRequestConfig config, Charset charset) {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, charset);
        HttpPost request = new HttpPost(url);
        request.setEntity(entity);
        return SyncClient.execute(config, request, new StringResponseHandler());
    }

    public static <T extends NameValuePair> Headers postForm(String url, final List<T> parameters, Consumer<InputStream> streamConsumer) {
        return postForm(url, parameters, HttpRequestConfig.DEFAULT, StandardCharsets.UTF_8, streamConsumer);
    }

    public static <T extends NameValuePair> Headers postForm(String url, final List<T> parameters, HttpRequestConfig config, Consumer<InputStream> streamConsumer) {
        return postForm(url, parameters, config, StandardCharsets.UTF_8, streamConsumer);
    }

    public static <T extends NameValuePair> Headers postForm(String url, final List<T> parameters, HttpRequestConfig config, Charset charset, Consumer<InputStream> streamConsumer) {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, charset);
        HttpPost request = new HttpPost(url);
        request.setEntity(entity);
        return SyncClient.execute(config, request, new StreamResponseHandler(streamConsumer));
    }

    /**
     * Post Multipart 请求
     */
    public static StringEntityResponse postMultipart(String url, MultipartEntityBuilder entity) {
        return postMultipart(url, entity, HttpRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postMultipart(String url, MultipartEntityBuilder entity, HttpRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(entity.build());
        return SyncClient.execute(config, request, new StringResponseHandler());
    }

    public static Headers postMultipart(String url, MultipartEntityBuilder entity, Consumer<InputStream> streamConsumer) {
        return postMultipart(url, entity, HttpRequestConfig.DEFAULT, streamConsumer);
    }

    public static Headers postMultipart(String url, MultipartEntityBuilder entity, HttpRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(entity.build());
        return SyncClient.execute(config, request, new StreamResponseHandler(streamConsumer));
    }

    /**
     * Post File 请求
     */
    public static StringEntityResponse postFile(String url, File file) {
        return postFile(url, file, guessContentType(file), HttpRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postFile(String url, File file, ContentType contentType) {
        return postFile(url, file, contentType, HttpRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postFile(String url, File file, ContentType contentType, HttpRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new FileEntity(file, contentType));
        return SyncClient.execute(config, request, new StringResponseHandler());
    }

    public static Headers postFile(String url, File file, Consumer<InputStream> streamConsumer) {
        return postFile(url, file, guessContentType(file), HttpRequestConfig.DEFAULT, streamConsumer);
    }

    public static Headers postFile(String url, File file, ContentType contentType, Consumer<InputStream> streamConsumer) {
        return postFile(url, file, contentType, HttpRequestConfig.DEFAULT, streamConsumer);
    }

    public static Headers postFile(String url, File file, ContentType contentType, HttpRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new FileEntity(file, contentType));
        return SyncClient.execute(config, request, new StreamResponseHandler(streamConsumer));
    }
}
