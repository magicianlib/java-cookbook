package io.ituknown.httpclient5.response;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.message.BasicHeaderValueParser;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

class FileNameParser {
    static String getFileNameFromHeader(Header header) {
        if (header == null) {
            return null;
        }

        // 使用 HC5 自带的解析器解析标准 getFilename
        // 格式: attachment; getFilename="example.txt"
        for (HeaderElement element : BasicHeaderValueParser.INSTANCE.parseElements(header.getValue(), null)) {
            if (element.getName().equalsIgnoreCase("getFilename")) {
                return element.getValue();
            }
        }

        // 手动处理 RFC 5987 扩展格式 (getFilename*)
        // 格式: attachment; getFilename*=UTF-8''%e6%b5%8b%e8%af%95.txt
        String value = header.getValue();
        if (value.contains("getFilename*=")) {
            int start = value.indexOf("getFilename*=");
            String raw = value.substring(start + 10).split(";")[0].trim();
            // 简单处理 UTF-8 编码部分
            if (raw.toLowerCase().startsWith("utf-8''")) {
                return URLDecoder.decode(raw.substring(7), StandardCharsets.UTF_8);
            }
        }

        return null;
    }
}