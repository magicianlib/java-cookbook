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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class HttpClientUtils extends SyncClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientUtils.class);

    /**
     * Get 请求
     */
    public static StringEntityResponse get(String url) {
        return get(url, CustomRequestConfig.DEFAULT);
    }

    public static StringEntityResponse get(String url, CustomRequestConfig config) {
        HttpGet request = new HttpGet(url);
        return execute(config, request, new StringHttpClientResponseHandler());
    }

    public static Header getStream(String url, Consumer<InputStream> streamConsumer) {
        return getStream(url, CustomRequestConfig.DEFAULT, streamConsumer);
    }

    public static Header getStream(String url, CustomRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpGet request = new HttpGet(url);
        return execute(config, request, new StreamHttpClientResponseHandler(streamConsumer));
    }

    public static FileEntityResponse download(String url, String filename) {
        return download(url, CustomRequestConfig.DEFAULT, filename);
    }

    public static FileEntityResponse download(String url, CustomRequestConfig config, String filename) {
        HttpGet request = new HttpGet(url);
        return execute(config, request, new FileDownloadHttpClientResponseHandler(filename));
    }

    public static FileEntityResponse downloadUseRemoteName(String url, Path dir) {
        return downloadUseRemoteName(url, CustomRequestConfig.DEFAULT, dir);
    }

    public static FileEntityResponse downloadUseRemoteName(String url, CustomRequestConfig config, Path dir) {
        HttpGet request = new HttpGet(url);
        return execute(config, request, new FileDownloadUseRemoteNameHttpClientResponseHandler(dir));
    }

    /**
     * Post JSON 请求
     */
    public static StringEntityResponse postJson(String url, String jsonContent) {
        return postJson(url, jsonContent, CustomRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postJson(String url, String jsonContent, CustomRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(jsonContent, ContentType.APPLICATION_JSON));
        return execute(config, request, new StringHttpClientResponseHandler());
    }

    public static Header postJson(String url, String jsonContent, Consumer<InputStream> streamConsumer) {
        return postJson(url, jsonContent, CustomRequestConfig.DEFAULT, streamConsumer);
    }

    public static Header postJson(String url, String jsonContent, CustomRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(jsonContent, ContentType.APPLICATION_JSON));
        return execute(config, request, new StreamHttpClientResponseHandler(streamConsumer));
    }

    public static StringEntityResponse postJson(String url, byte[] jsonContent) {
        return postJson(url, jsonContent, CustomRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postJson(String url, byte[] jsonContent, CustomRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(jsonContent, ContentType.APPLICATION_JSON));
        return execute(config, request, new StringHttpClientResponseHandler());
    }

    public static Header postJson(String url, byte[] jsonContent, Consumer<InputStream> streamConsumer) {
        return postJson(url, jsonContent, CustomRequestConfig.DEFAULT, streamConsumer);
    }

    public static Header postJson(String url, byte[] jsonContent, CustomRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(jsonContent, ContentType.APPLICATION_JSON));
        return execute(config, request, new StreamHttpClientResponseHandler(streamConsumer));
    }

    /**
     * Post 请求
     */
    public static StringEntityResponse post(String url, String content, ContentType contentType) {
        return post(url, content, contentType, CustomRequestConfig.DEFAULT);
    }

    public static StringEntityResponse post(String url, String content, ContentType contentType, CustomRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(content, contentType));
        return execute(config, request, new StringHttpClientResponseHandler());
    }

    public static Header post(String url, String content, ContentType contentType, Consumer<InputStream> streamConsumer) {
        return post(url, content, contentType, CustomRequestConfig.DEFAULT, streamConsumer);
    }

    public static Header post(String url, String content, ContentType contentType, CustomRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(content, contentType));
        return execute(config, request, new StreamHttpClientResponseHandler(streamConsumer));
    }

    public static StringEntityResponse post(String url, byte[] content, ContentType contentType) {
        return post(url, content, contentType, CustomRequestConfig.DEFAULT, StandardCharsets.UTF_8);
    }

    public static StringEntityResponse post(String url, byte[] content, ContentType contentType, CustomRequestConfig config) {
        return post(url, content, contentType, config, StandardCharsets.UTF_8);
    }

    public static StringEntityResponse post(String url, byte[] content, ContentType contentType, CustomRequestConfig config, Charset charset) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(content, contentType));
        return execute(config, request, new StringHttpClientResponseHandler());
    }

    public static Header post(String url, byte[] content, ContentType contentType, Consumer<InputStream> streamConsumer) {
        return post(url, content, contentType, CustomRequestConfig.DEFAULT, streamConsumer);
    }

    public static Header post(String url, byte[] content, ContentType contentType, CustomRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(content, contentType));
        return execute(config, request, new StreamHttpClientResponseHandler(streamConsumer));
    }

    /**
     * Post Form 请求
     */
    public static <T extends NameValuePair> StringEntityResponse postForm(String url, final List<T> parameters) {
        return postForm(url, parameters, CustomRequestConfig.DEFAULT, StandardCharsets.UTF_8);
    }

    public static <T extends NameValuePair> StringEntityResponse postForm(String url, final List<T> parameters, CustomRequestConfig config) {
        return postForm(url, parameters, config, StandardCharsets.UTF_8);
    }

    public static <T extends NameValuePair> StringEntityResponse postForm(String url, final List<T> parameters, CustomRequestConfig config, Charset charset) {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, charset);
        HttpPost request = new HttpPost(url);
        request.setEntity(entity);
        return execute(config, request, new StringHttpClientResponseHandler());
    }

    public static <T extends NameValuePair> Header postForm(String url, final List<T> parameters, Consumer<InputStream> streamConsumer) {
        return postForm(url, parameters, CustomRequestConfig.DEFAULT, StandardCharsets.UTF_8, streamConsumer);
    }

    public static <T extends NameValuePair> Header postForm(String url, final List<T> parameters, CustomRequestConfig config, Consumer<InputStream> streamConsumer) {
        return postForm(url, parameters, config, StandardCharsets.UTF_8, streamConsumer);
    }

    public static <T extends NameValuePair> Header postForm(String url, final List<T> parameters, CustomRequestConfig config, Charset charset, Consumer<InputStream> streamConsumer) {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, charset);
        HttpPost request = new HttpPost(url);
        request.setEntity(entity);
        return execute(config, request, new StreamHttpClientResponseHandler(streamConsumer));
    }

    /**
     * Post Multipart 请求
     */
    public static StringEntityResponse postMultipart(String url, MultipartEntityBuilder entity) {
        return postMultipart(url, entity, CustomRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postMultipart(String url, MultipartEntityBuilder entity, CustomRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(entity.build());
        return execute(config, request, new StringHttpClientResponseHandler());
    }

    public static Header postMultipart(String url, MultipartEntityBuilder entity, Consumer<InputStream> streamConsumer) {
        return postMultipart(url, entity, CustomRequestConfig.DEFAULT, streamConsumer);
    }

    public static Header postMultipart(String url, MultipartEntityBuilder entity, CustomRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(entity.build());
        return execute(config, request, new StreamHttpClientResponseHandler(streamConsumer));
    }

    /**
     * Post File 请求
     */
    public static StringEntityResponse postFile(String url, File file) {
        String type = URLConnection.guessContentTypeFromName(file.getName());
        if (type == null) {
            type = "application/octet-stream"; // 默认二进制流
        }

        return postFile(url, file, ContentType.create(type), CustomRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postFile(String url, File file, ContentType contentType) {
        return postFile(url, file, contentType, CustomRequestConfig.DEFAULT);
    }

    public static StringEntityResponse postFile(String url, File file, ContentType contentType, CustomRequestConfig config) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new FileEntity(file, contentType));
        return execute(config, request, new StringHttpClientResponseHandler());
    }

    public static Header postFile(String url, File file, Consumer<InputStream> streamConsumer) {
        String type = URLConnection.guessContentTypeFromName(file.getName());
        if (type == null) {
            type = "application/octet-stream"; // 默认二进制流
        }

        return postFile(url, file, ContentType.create(type), CustomRequestConfig.DEFAULT, streamConsumer);
    }

    public static Header postFile(String url, File file, ContentType contentType, Consumer<InputStream> streamConsumer) {
        return postFile(url, file, contentType, CustomRequestConfig.DEFAULT, streamConsumer);
    }

    public static Header postFile(String url, File file, ContentType contentType, CustomRequestConfig config, Consumer<InputStream> streamConsumer) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new FileEntity(file, contentType));
        return execute(config, request, new StreamHttpClientResponseHandler(streamConsumer));
    }
}