package io.ituknown.okhttp;

import io.ituknown.okhttp.parameter.KeyValueParameter;
import io.ituknown.okhttp.parameter.MultipartParameter;

import java.io.File;

public final class HttpClientUtils {
    private static final KeyValueParameter DEFAULT_QUERY = new KeyValueParameter();
    private static final RequestConfig DEFAULT_CONFIG = new RequestConfig();

    public static String get(String url) {
        return get(url, DEFAULT_QUERY);
    }

    public static String get(String url, KeyValueParameter parameter) {
        return get(url, parameter, DEFAULT_CONFIG);
    }

    public static String get(String url, KeyValueParameter parameter, RequestConfig config) {
        return GET.get(url, parameter, config);
    }

    /**
     * get file with byte[]
     */
    public static byte[] getFile(String url) {
        return getFile(url, DEFAULT_QUERY);
    }

    public static byte[] getFile(String url, KeyValueParameter parameter) {
        return getFile(url, parameter, DEFAULT_CONFIG);
    }

    public static byte[] getFile(String url, KeyValueParameter parameter, RequestConfig config) {
        return GET.getFile(url, parameter, config);
    }

    /**
     * @return download file path
     */
    public static String download(String url) {
        return download(url, DEFAULT_QUERY);
    }

    public static String download(String url, KeyValueParameter parameter) {
        return download(url, parameter, DEFAULT_CONFIG);
    }

    public static String download(String url, KeyValueParameter parameter, RequestConfig config) {
        return GET.download(url, parameter, config);
    }

    public static String postText(String url, String text) {
        return postText(url, text, DEFAULT_CONFIG);
    }

    public static String postText(String url, String text, RequestConfig config) {
        return POST.postText(url, text, config);
    }

    public static String postJson(String url, String json) {
        return postJson(url, json, DEFAULT_CONFIG);
    }

    public static String postJson(String url, String json, RequestConfig config) {
        return POST.postJson(url, json, config);
    }

    public static String postForm(String url, KeyValueParameter parameter) {
        return postForm(url, parameter, DEFAULT_CONFIG);
    }

    public static String postForm(String url, KeyValueParameter parameter, RequestConfig config) {
        return POST.postForm(url, parameter, config);
    }

    public static String postMultipart(String url, MultipartParameter parameter) {
        return postMultipart(url, parameter, DEFAULT_CONFIG);
    }

    public static String postMultipart(String url, MultipartParameter parameter, RequestConfig config) {
        return POST.postMultipart(url, parameter, config);
    }

    public static String postFile(String url, File file) {
        return postFile(url, file, DEFAULT_CONFIG);
    }

    public static String postFile(String url, File file, RequestConfig config) {
        return POST.postFile(url, file, config);
    }

    public static String postFile(String url, String fileName, byte[] file) {
        return postFile(url, fileName, file, DEFAULT_CONFIG);
    }

    public static String postFile(String url, String fileName, byte[] file, RequestConfig config) {
        return POST.postFile(url, fileName, file, config);
    }
}