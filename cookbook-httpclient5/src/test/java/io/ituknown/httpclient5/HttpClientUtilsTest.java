package io.ituknown.httpclient5;

import com.sun.net.httpserver.HttpServer;
import io.ituknown.httpclient5.response.FileEntityResponse;
import io.ituknown.httpclient5.response.Headers;
import io.ituknown.httpclient5.response.StringEntityResponse;
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
        StringEntityResponse result = HttpClientUtils.get(baseUrl + "/get").asString();
        assertEquals("Hello World", result.getEntity());
        assertNotNull(result.getHeaders());
    }

    @Test
    void testGetWithCustomHeaders() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.addHeader("X-Request-Id", "12345");

        StringEntityResponse result = HttpClientUtils.get(baseUrl + "/get-custom-headers").config(config).asString();

        assertEquals("ok", result.getEntity());
        assertEquals("test-value", result.getHeaders().getField("X-Custom").value());
    }

    @Test
    void testGetStream() {
        AtomicReference<String> captured = new AtomicReference<>();
        Headers headers = HttpClientUtils.get(baseUrl + "/stream").stream(in -> {
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

        Headers headers = HttpClientUtils.get(baseUrl + "/stream").config(config).stream(in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        assertEquals("large-stream-content", captured.get());
        assertNotNull(headers);
    }

    // ========== Download ==========

    @Test
    void testDownload() throws IOException {
        Path targetFile = tempDir.resolve("downloaded-test.txt");
        FileEntityResponse result = HttpClientUtils.get(baseUrl + "/download").downloadTo(targetFile);
        assertTrue(result.getFileSize() > 0);
        assertEquals(targetFile, result.getFilePath());
        assertTrue(Files.exists(targetFile));
        assertEquals("file-content-here", Files.readString(targetFile));
    }

    @Test
    void testDownloadWithConfig() throws IOException {
        HttpRequestConfig config = new HttpRequestConfig();
        Path targetFile = tempDir.resolve("downloaded-config.txt");
        FileEntityResponse result = HttpClientUtils.get(baseUrl + "/download").config(config).downloadTo(targetFile.toString());
        assertTrue(result.getFileSize() > 0);
        assertTrue(Files.exists(targetFile));
    }

    @Test
    void testDownloadUseRemoteName() throws IOException {
        FileEntityResponse result = HttpClientUtils.get(baseUrl + "/download").downloadToRemoteName(tempDir);
        assertTrue(result.getFileSize() > 0);
        assertNotNull(result.getFilePath());
        assertTrue(Files.exists(result.getFilePath()));
        assertEquals("test-download.txt", result.getFilePath().getFileName().toString());
    }

    @Test
    void testDownloadUseRemoteNameFallbackToUrl() throws IOException {
        FileEntityResponse result = HttpClientUtils.get(baseUrl + "/download-no-disposition/data.bin").downloadToRemoteName(tempDir);
        assertTrue(result.getFileSize() > 0);
        assertNotNull(result.getFilePath());
        assertTrue(Files.exists(result.getFilePath()));
        assertEquals("data.bin", result.getFilePath().getFileName().toString());
    }

    @Test
    void testDownloadUseRemoteNameWithConfig() throws IOException {
        HttpRequestConfig config = new HttpRequestConfig();
        FileEntityResponse result = HttpClientUtils.get(baseUrl + "/download").config(config).downloadToRemoteName(tempDir);
        assertTrue(result.getFileSize() > 0);
        assertEquals("test-download.txt", result.getFilePath().getFileName().toString());
    }

    // ========== Error handling ==========

    @Test
    void testGetServerError() {
        assertThrows(HttpException.class, () -> HttpClientUtils.get(baseUrl + "/error").asString());
    }

    @Test
    void testDownloadServerError() throws IOException {
        Path target = tempDir.resolve("should-not-exist.txt");
        assertThrows(HttpException.class, () -> HttpClientUtils.get(baseUrl + "/error").downloadTo(target.toString()));
        assertFalse(Files.exists(target));
    }
}
