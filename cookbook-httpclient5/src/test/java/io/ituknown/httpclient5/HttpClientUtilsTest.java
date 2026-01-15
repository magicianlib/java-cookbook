package io.ituknown.httpclient5;

import io.ituknown.httpclient5.response.FileEntityResponse;
import io.ituknown.httpclient5.response.StringEntityResponse;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpClientUtilsTest {
    @Test
    public void testGet() {
        CustomRequestConfig config = new CustomRequestConfig();
        config.setProxy("127.0.0.1:7897");
        StringEntityResponse result = HttpClientUtils.get("http://baidu.com", config);
        System.out.println(result.getEntity());
        for (Header header : result.getHeadersUseEmptyIsAbsent()) {
            System.out.println(header.getName() + ": " + header.getValue());
        }

        Assertions.assertNotNull(result.getEntity());
    }

    @Test
    public void testDownload() {
        CustomRequestConfig config = new CustomRequestConfig();
        config.setProxy("127.0.0.1:7897");
        FileEntityResponse result = HttpClientUtils.download("https://github.com/magicianlib.png", config, "C:\\Users\\WINDOWS\\Downloads\\tmp\\tmp\\magicianlib.png");
        System.out.println(result.getFilename());
        System.out.println(result.getFileSize());
        for (Header header : result.getHeadersUseEmptyIsAbsent()) {
            System.out.println(header.getName() + ": " + header.getValue());
        }

        Assertions.assertTrue(result.getFileSize() > 0);
    }

    @Test
    public void testDownloadUseRemoteName() {
        CustomRequestConfig config = new CustomRequestConfig();
        config.setProxy("127.0.0.1:7897");

        Path path = Paths.get("C:\\Users\\WINDOWS\\Downloads\\tmp\\tmp");
        FileEntityResponse result = HttpClientUtils.downloadUseRemoteName("https://github.com/magicianlib.png", config, path);
        System.out.println(result.getFilename());
        System.out.println(result.getFileSize());
        for (Header header : result.getHeadersUseEmptyIsAbsent()) {
            System.out.println(header.getName() + ": " + header.getValue());
        }

        Assertions.assertTrue(result.getFileSize() > 0);
    }

}