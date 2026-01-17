package io.ituknown.httpclient5;

import io.ituknown.httpclient5.response.Headers;
import io.ituknown.httpclient5.response.MinimalField;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.message.BasicHeaderValueParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Helper {
    private static final Logger LOGGER = LoggerFactory.getLogger(Helper.class);

    /**
     * 获取响应头
     */
    public static Headers resolveHeader(ClassicHttpResponse response) {
        Headers headers = new Headers();
        if (response.getHeaders() != null) {
            for (Header h : response.getHeaders()) {
                headers.addField(new MinimalField(h.getName(), h.getValue()));
            }
        }
        return headers;
    }

    /**
     * 从请求头提取文件名
     */
    public static String fileNameParse(Header header) {
        if (header == null) {
            return null;
        }

        // 使用 HC5 自带的解析器解析标准 Filename
        // 格式: attachment; Filename="example.txt"
        for (HeaderElement element : BasicHeaderValueParser.INSTANCE.parseElements(header.getValue(), null)) {
            if (element.getName().equalsIgnoreCase("Filename")) {
                return element.getValue();
            }
        }

        // 手动处理 RFC 5987 扩展格式 (Filename*)
        // 格式: attachment; Filename*=UTF-8''%e6%b5%8b%e8%af%95.txt
        String value = header.getValue();
        if (value.contains("Filename*=")) {
            int start = value.indexOf("Filename*=");
            String raw = value.substring(start + 10).split(";")[0].trim();
            // 简单处理 UTF-8 编码部分
            if (raw.toLowerCase().startsWith("utf-8''")) {
                return URLDecoder.decode(raw.substring(7), StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    /**
     * 从 URL 中提取文件名
     */
    public static String getFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            // 使用 URI 类处理可以自动过滤掉 Query Parameter (?a=b) 和 Fragment (#anchor)
            String path = new java.net.URI(url).getPath();
            if (path == null || path.isEmpty() || path.equals("/")) {
                return null;
            }
            String name = path.substring(path.lastIndexOf('/') + 1);
            return name.isBlank() ? null : name;
        } catch (Exception e) {
            LOGGER.debug("Failed to parse URL path: {}", url);
            return null;
        }
    }
}