package io.ituknown.httpclient5;

import com.sun.net.httpserver.HttpServer;
import io.ituknown.httpclient5.response.FileEntityResponse;
import io.ituknown.httpclient5.response.Headers;
import io.ituknown.httpclient5.response.StringEntityResponse;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpClientUtilsTest {

    @TempDir
    static Path tempDir;

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        registerEndpoints();
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static void registerEndpoints() {
        server.createContext("/get", exchange -> {
            byte[] body = "Hello World".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().put("Content-Type", List.of("text/plain"));
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/get-custom-headers", exchange -> {
            exchange.getResponseHeaders().put("X-Custom", List.of("test-value"));
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/stream", exchange -> {
            byte[] body = "large-stream-content".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/post-echo", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = ("echo:" + requestBody).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().put("Content-Type", List.of("application/json"));
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/post-form", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = ("form:" + requestBody).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/post-multipart", exchange -> {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            byte[] body = ("multipart:" + contentType + ":" + requestBytes.length).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/post-file", exchange -> {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            byte[] body = ("received:" + requestBytes.length + "bytes").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/download", exchange -> {
            byte[] body = "file-content-here".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().put("Content-Disposition", List.of("attachment; Filename=\"test-download.txt\""));
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/download-no-disposition", exchange -> {
            byte[] body = "remote-file-data".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/error", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }

    // ========== GET ==========

    @Test
    void testGet() {
        StringEntityResponse result = HttpClientUtils.get(baseUrl + "/get");

        assertEquals("Hello World", result.getEntity());
        assertNotNull(result.getHeaders());
    }

    @Test
    void testGetWithCustomHeaders() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.addHeader("X-Request-Id", "12345");

        StringEntityResponse result = HttpClientUtils.get(baseUrl + "/get-custom-headers", config);

        assertEquals("ok", result.getEntity());
        assertEquals("test-value", result.getHeaders().getField("X-Custom").value());
    }

    @Test
    void testGetStream() {
        AtomicReference<String> captured = new AtomicReference<>();
        Headers headers = HttpClientUtils.getStream(baseUrl + "/stream", in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertEquals("large-stream-content", captured.get());
        assertNotNull(headers);
    }

    @Test
    void testGetStreamWithConfig() {
        HttpRequestConfig config = new HttpRequestConfig();
        AtomicReference<String> captured = new AtomicReference<>();

        Headers headers = HttpClientUtils.getStream(baseUrl + "/stream", config, in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertEquals("large-stream-content", captured.get());
        assertNotNull(headers);
    }

    // ========== POST JSON ==========

    @Test
    void testPostJsonString() {
        String json = "{\"name\":\"test\"}";
        StringEntityResponse result = HttpClientUtils.postJson(baseUrl + "/post-echo", json);
        assertEquals("echo:" + json, result.getEntity());
    }

    @Test
    void testPostJsonStringWithConfig() {
        String json = "{\"name\":\"test\"}";
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.postJson(baseUrl + "/post-echo", json, config);
        assertEquals("echo:" + json, result.getEntity());
    }

    @Test
    void testPostJsonBytes() {
        byte[] json = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        StringEntityResponse result = HttpClientUtils.postJson(baseUrl + "/post-echo", json);
        assertEquals("echo:" + new String(json, StandardCharsets.UTF_8), result.getEntity());
    }

    @Test
    void testPostJsonBytesWithConfig() {
        byte[] json = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.postJson(baseUrl + "/post-echo", json, config);
        assertEquals("echo:" + new String(json, StandardCharsets.UTF_8), result.getEntity());
    }

    @Test
    void testPostJsonStringStream() {
        String json = "{\"stream\":true}";
        AtomicReference<String> captured = new AtomicReference<>();

        HttpClientUtils.postJson(baseUrl + "/post-echo", json, in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertEquals("echo:" + json, captured.get());
    }

    @Test
    void testPostJsonBytesStream() {
        byte[] json = "{\"stream\":true}".getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> captured = new AtomicReference<>();

        HttpClientUtils.postJson(baseUrl + "/post-echo", json, in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertEquals("echo:" + new String(json, StandardCharsets.UTF_8), captured.get());
    }

    // ========== POST ==========

    @Test
    void testPostString() {
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo", "plain text", ContentType.TEXT_PLAIN);
        assertEquals("echo:plain text", result.getEntity());
    }

    @Test
    void testPostStringWithConfig() {
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo", "data", ContentType.TEXT_PLAIN, config);
        assertEquals("echo:data", result.getEntity());
    }

    @Test
    void testPostBytes() {
        byte[] content = "binary-data".getBytes(StandardCharsets.UTF_8);
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo", content, ContentType.APPLICATION_OCTET_STREAM);
        assertEquals("echo:binary-data", result.getEntity());
    }

    @Test
    void testPostBytesWithConfig() {
        byte[] content = "binary-data".getBytes(StandardCharsets.UTF_8);
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo", content, ContentType.APPLICATION_OCTET_STREAM, config);
        assertEquals("echo:binary-data", result.getEntity());
    }

    @Test
    void testPostStringStream() {
        AtomicReference<String> captured = new AtomicReference<>();

        HttpClientUtils.post(baseUrl + "/post-echo", "stream-text", ContentType.TEXT_PLAIN, in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertEquals("echo:stream-text", captured.get());
    }

    @Test
    void testPostBytesStream() {
        byte[] content = "stream-binary".getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> captured = new AtomicReference<>();

        HttpClientUtils.post(baseUrl + "/post-echo", content, ContentType.APPLICATION_OCTET_STREAM, in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertEquals("echo:stream-binary", captured.get());
    }

    // ========== POST Form ==========

    @Test
    void testPostForm() {
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("username", "admin"));
        params.add(new BasicNameValuePair("password", "123456"));

        StringEntityResponse result = HttpClientUtils.postForm(baseUrl + "/post-form", params);

        String entity = result.getEntity();
        assertTrue(entity.startsWith("form:"));
        assertTrue(entity.contains("username=admin"));
        assertTrue(entity.contains("password=123456"));
    }

    @Test
    void testPostFormWithConfig() {
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("key", "value"));

        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.postForm(baseUrl + "/post-form", params, config);
        assertTrue(result.getEntity().contains("key=value"));
    }

    @Test
    void testPostFormStream() {
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("q", "java"));
        AtomicReference<String> captured = new AtomicReference<>();

        HttpClientUtils.postForm(baseUrl + "/post-form", params, in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertTrue(captured.get().contains("q=java"));
    }

    // ========== POST Multipart ==========

    @Test
    void testPostMultipart() {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addTextBody("field1", "value1", ContentType.TEXT_PLAIN)
                .addTextBody("field2", "value2", ContentType.TEXT_PLAIN);

        StringEntityResponse result = HttpClientUtils.postMultipart(baseUrl + "/post-multipart", builder);

        String entity = result.getEntity();
        assertTrue(entity.startsWith("multipart:"));
        assertTrue(entity.contains("multipart/form-data"));
    }

    @Test
    void testPostMultipartWithConfig() {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addTextBody("name", "test", ContentType.TEXT_PLAIN);

        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.postMultipart(baseUrl + "/post-multipart", builder, config);
        assertTrue(result.getEntity().contains("multipart/form-data"));
    }

    @Test
    void testPostMultipartStream() {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addTextBody("data", "hello", ContentType.TEXT_PLAIN);
        AtomicReference<String> captured = new AtomicReference<>();

        HttpClientUtils.postMultipart(baseUrl + "/post-multipart", builder, in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertTrue(captured.get().contains("multipart/form-data"));
    }

    // ========== POST File ==========

    @Test
    void testPostFile() throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "upload", ".txt");
        Files.writeString(tempFile, "file-content-here");

        StringEntityResponse result = HttpClientUtils.postFile(baseUrl + "/post-file", tempFile.toFile());
        assertTrue(result.getEntity().startsWith("received:"));
        assertTrue(result.getEntity().endsWith("bytes"));
    }

    @Test
    void testPostFileWithContentType() throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "upload", ".json");
        Files.writeString(tempFile, "{\"data\":1}");

        StringEntityResponse result = HttpClientUtils.postFile(
                baseUrl + "/post-file", tempFile.toFile(), ContentType.APPLICATION_JSON);
        assertTrue(result.getEntity().startsWith("received:"));
    }

    @Test
    void testPostFileStream() throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "upload", ".txt");
        Files.writeString(tempFile, "stream-file-content");
        AtomicReference<String> captured = new AtomicReference<>();

        HttpClientUtils.postFile(baseUrl + "/post-file", tempFile.toFile(), in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertTrue(captured.get().startsWith("received:"));
    }

    // ========== Download ==========

    @Test
    void testDownload() throws IOException {
        Path targetFile = tempDir.resolve("downloaded-test.txt");

        FileEntityResponse result = HttpClientUtils.download(baseUrl + "/download", targetFile.toString());

        assertTrue(result.getFileSize() > 0);
        assertEquals(targetFile, result.getFilePath());
        assertTrue(Files.exists(targetFile));
        assertEquals("file-content-here", Files.readString(targetFile));
    }

    @Test
    void testDownloadWithConfig() throws IOException {
        HttpRequestConfig config = new HttpRequestConfig();
        Path targetFile = tempDir.resolve("downloaded-config.txt");

        FileEntityResponse result = HttpClientUtils.download(baseUrl + "/download", config, targetFile.toString());

        assertTrue(result.getFileSize() > 0);
        assertTrue(Files.exists(targetFile));
    }

    @Test
    void testDownloadUseRemoteName() throws IOException {
        FileEntityResponse result = HttpClientUtils.downloadUseRemoteName(baseUrl + "/download", tempDir);

        assertTrue(result.getFileSize() > 0);
        assertNotNull(result.getFilePath());
        assertTrue(Files.exists(result.getFilePath()));
        assertEquals("test-download.txt", result.getFilePath().getFileName().toString());
    }

    @Test
    void testDownloadUseRemoteNameFallbackToUrl() throws IOException {
        FileEntityResponse result = HttpClientUtils.downloadUseRemoteName(
                baseUrl + "/download-no-disposition/data.bin", tempDir);

        assertTrue(result.getFileSize() > 0);
        assertNotNull(result.getFilePath());
        assertTrue(Files.exists(result.getFilePath()));
        assertEquals("data.bin", result.getFilePath().getFileName().toString());
    }

    @Test
    void testDownloadUseRemoteNameWithConfig() throws IOException {
        HttpRequestConfig config = new HttpRequestConfig();

        FileEntityResponse result = HttpClientUtils.downloadUseRemoteName(baseUrl + "/download", config, tempDir);

        assertTrue(result.getFileSize() > 0);
        assertEquals("test-download.txt", result.getFilePath().getFileName().toString());
    }

    // ========== Error handling ==========

    @Test
    void testGetServerError() {
        assertThrows(HttpException.class, () -> HttpClientUtils.get(baseUrl + "/error"));
    }

    @Test
    void testPostJsonServerError() {
        assertThrows(HttpException.class, () -> HttpClientUtils.postJson(baseUrl + "/error", "{}"));
    }

    @Test
    void testDownloadServerError() {
        Path target = tempDir.resolve("should-not-exist.txt");
        assertThrows(HttpException.class, () -> HttpClientUtils.download(baseUrl + "/error", target.toString()));
        assertFalse(Files.exists(target));
    }
}
