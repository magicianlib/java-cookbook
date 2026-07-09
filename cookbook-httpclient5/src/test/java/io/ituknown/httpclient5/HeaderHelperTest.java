package io.ituknown.httpclient5;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeaderHelperTest {

    // --- resolveHeader ---

    @Test
    void resolveHeaderWithHeaders() {
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200, "OK");
        response.addHeader(new BasicHeader("Content-Type", "application/json"));
        response.addHeader(new BasicHeader("X-Custom", "value"));

        var headers = HeaderHelper.resolveHeader(response);
        assertEquals(2, headers.getFields().size());
        assertEquals("application/json", headers.getField("Content-Type").value());
        assertEquals("value", headers.getField("X-Custom").value());
    }

    @Test
    void resolveHeaderEmpty() {
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200, "OK");
        var headers = HeaderHelper.resolveHeader(response);
        assertTrue(headers.getFields().isEmpty());
    }

    // --- fileNameParse ---

    @Test
    void fileNameParseStandardFilename() {
        Header header = new BasicHeader("Content-Disposition", "attachment; Filename=\"example.txt\"");
        assertEquals("example.txt", HeaderHelper.fileNameParse(header));
    }

    @Test
    void fileNameParseFilenameWithoutQuotes() {
        Header header = new BasicHeader("Content-Disposition", "attachment; Filename=example.txt");
        assertEquals("example.txt", HeaderHelper.fileNameParse(header));
    }

    @Test
    void fileNameParseRFC5987() {
        Header header = new BasicHeader("Content-Disposition", "attachment; Filename*=UTF-8''%E6%B5%8B%E8%AF%95.txt");
        assertEquals("测试.txt", HeaderHelper.fileNameParse(header));
    }

    @Test
    void fileNameParseLowercaseFilenameStar() {
        Header header = new BasicHeader("Content-Disposition", "attachment; filename*=utf-8''%E6%B5%8B%E8%AF%95.txt");
        assertEquals("测试.txt", HeaderHelper.fileNameParse(header));
    }

    @Test
    void fileNameParseStarPrecedenceOverFilename() {
        Header header = new BasicHeader("Content-Disposition",
                "attachment; filename=\"a.txt\"; filename*=UTF-8''b.txt");
        assertEquals("b.txt", HeaderHelper.fileNameParse(header));
    }

    @Test
    void fileNameParseMalformedFilenameStarFallsBackToFilename() {
        Header header = new BasicHeader("Content-Disposition",
                "attachment; filename=\"ok.txt\"; filename*=UTF-8''%ZZ");
        assertEquals("ok.txt", HeaderHelper.fileNameParse(header));
    }

    @Test
    void fileNameParseUnknownCharsetFallsBackToUtf8() {
        Header header = new BasicHeader("Content-Disposition", "attachment; filename*=WTF-8''%E6%B5%8B.txt");
        assertEquals("测.txt", HeaderHelper.fileNameParse(header));
    }

    @Test
    void fileNameParseNullHeader() {
        assertNull(HeaderHelper.fileNameParse(null));
    }

    @Test
    void fileNameParseNoFilenameInHeader() {
        Header header = new BasicHeader("Content-Disposition", "inline");
        assertNull(HeaderHelper.fileNameParse(header));
    }

    // --- getFileNameFromUrl ---

    @Test
    void getFileNameFromUrl() {
        assertEquals("file.txt", HeaderHelper.getFileNameFromUrl("https://example.com/path/file.txt"));
    }

    @Test
    void getFileNameFromUrlWithQuery() {
        assertEquals("file.txt", HeaderHelper.getFileNameFromUrl("https://example.com/path/file.txt?a=1&b=2"));
    }

    @Test
    void getFileNameFromUrlWithFragment() {
        assertEquals("file.txt", HeaderHelper.getFileNameFromUrl("https://example.com/path/file.txt#section"));
    }

    @Test
    void getFileNameFromUrlNull() {
        assertNull(HeaderHelper.getFileNameFromUrl(null));
    }

    @Test
    void getFileNameFromUrlEmpty() {
        assertNull(HeaderHelper.getFileNameFromUrl(""));
    }

    @Test
    void getFileNameFromUrlRootPath() {
        assertNull(HeaderHelper.getFileNameFromUrl("https://example.com/"));
    }

    @Test
    void getFileNameFromUrlNoPath() {
        assertNull(HeaderHelper.getFileNameFromUrl("https://example.com"));
    }

    @Test
    void getFileNameFromUrlInvalidUrl() {
        assertNull(HeaderHelper.getFileNameFromUrl("not a url :///"));
    }

    @Test
    void getFileNameFromUrlEncoded() {
        assertEquals("image.png", HeaderHelper.getFileNameFromUrl("https://example.com/files/image.png"));
    }
}
