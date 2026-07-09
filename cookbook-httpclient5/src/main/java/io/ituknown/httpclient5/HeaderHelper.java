package io.ituknown.httpclient5;

import io.ituknown.httpclient5.response.Headers;
import io.ituknown.httpclient5.response.MinimalField;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicHeaderValueParser;
import org.apache.hc.core5.http.message.ParserCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class HeaderHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderHelper.class);

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
     * 从 Content-Disposition 头提取文件名。
     * RFC 5987 扩展格式 filename*=charset''pct-encoded 优先于标准 filename=，且大小写不敏感。
     */
    public static String fileNameParse(Header header) {
        if (header == null) {
            return null;
        }
        String headerValue = header.getValue();
        if (headerValue == null) {
            return null;
        }

        // RFC 5987 扩展格式: filename*=charset''pct-encoded (大小写不敏感, 优先)
        String lower = headerValue.toLowerCase(Locale.ROOT);
        int starIdx = lower.indexOf("filename*=");
        if (starIdx >= 0) {
            String raw = headerValue.substring(starIdx + "filename*=".length()).split(";")[0].trim();
            int sep = raw.toLowerCase(Locale.ROOT).indexOf("''");
            if (sep >= 0) {
                String charsetName = raw.substring(0, sep);
                String encoded = raw.substring(sep + 2);
                Charset charset;
                try {
                    charset = Charset.forName(charsetName);
                } catch (Exception e) {
                    charset = StandardCharsets.UTF_8;
                }
                return URLDecoder.decode(encoded, charset);
            }
        }

        // 标准格式: filename="..." (HC5 解析器, 大小写不敏感)
        ParserCursor cursor = new ParserCursor(0, headerValue.length());
        for (HeaderElement element : BasicHeaderValueParser.INSTANCE.parseElements(headerValue, cursor)) {
            for (NameValuePair param : element.getParameters()) {
                if (param.getName().equalsIgnoreCase("Filename")) {
                    return param.getValue();
                }
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
