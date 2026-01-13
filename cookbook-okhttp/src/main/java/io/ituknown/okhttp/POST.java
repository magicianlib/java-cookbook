package io.ituknown.okhttp;

import io.ituknown.okhttp.parameter.KeyValueParameter;
import io.ituknown.okhttp.parameter.MultipartParameter;
import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

enum POST {
    ;
    private static final Logger LOGGER = LoggerFactory.getLogger(POST.class);

    public static String postText(String url, String text, RequestConfig config) {
        RequestBody body = RequestBody.create(ByteString.encodeString(text, StandardCharsets.UTF_8), MediaType.parse("text/plain"));
        return doPost(url, body, config, request -> LOGGER.info("Http {} Body [{}]", request, text));
    }

    public static String postJson(String url, String json, RequestConfig config) {
        return doPost(url, RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")), config, request -> LOGGER.info("Http {} Body [{}]", request, json));
    }

    public static String postForm(String url, KeyValueParameter parameter, RequestConfig config) {
        return doPost(url, parameter.toForm(), config, request -> LOGGER.info("Http {} Query [{}]", request, parameter));
    }

    public static String postMultipart(String url, MultipartParameter parameter, RequestConfig config) {
        return doPost(url, parameter.toMultipart(), config, request -> LOGGER.info("Http {} Body [Multipart]", request));
    }

    public static String postFile(String url, File file, RequestConfig config) {
        String fileName = file.getName();
        String type = URLConnection.guessContentTypeFromName(fileName);
        if (type == null) {
            type = "application/octet-stream"; // 默认二进制流
        }

        return doPost(url, RequestBody.create(file, MediaType.parse(type)), config, request -> LOGGER.info("Http {} Body [File={}]", request, file.getPath()));
    }

    public static String postFile(String url, String fileName, byte[] file, RequestConfig config) {
        String type = URLConnection.guessContentTypeFromName(fileName);
        if (type == null) {
            type = "application/octet-stream"; // 默认二进制流
        }

        return doPost(url, RequestBody.create(file, MediaType.parse(type)), config, request -> LOGGER.info("Http {} Body [FileName={}, Data=bytes]", request, fileName));
    }

    public static String doPost(String url, RequestBody body, RequestConfig config, Consumer<Request> printLog) {
        Request request = new Request.Builder()
                .headers(config.toHeaders())
                .url(url)
                .tag(RequestConfig.class, config)
                .post(body)
                .build();

        if (config.isNeedLog()) {
            printLog.accept(request);
        }

        Call call = Client.execute(config, request);
        try (Response response = call.execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                return responseBody.string();

            }

            if (config.isNeedLog()) {
                LOGGER.info("Request fail or no-content[code={}]", response.code());
            }
        } catch (Exception e) {
            throw new HttpException(e);
        }

        return null;
    }
}