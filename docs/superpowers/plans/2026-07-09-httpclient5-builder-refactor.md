# cookbook-httpclient5 Fluent Builder 重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `HttpClientUtils` 的 ~30 个静态重载重构为纯 fluent builder，并修复 payload 日志 OOM、`filename*` 大小写敏感、3xx 错误阈值三处缺陷。

**Architecture:** 保留底层骨架（`SyncClient` 执行引擎、`Headers`/`MinimalField`、各 `*ResponseHandler`、`HeaderHelper`、`HttpRequestConfig`），仅替换入口层：`HttpClientUtils.get(url)`/`post(url)` 返回嵌套 `RequestBuilder`，body setter / config / 终结操作正交组合。缺陷修复各自独立成任务，先于 builder 完成。

**Tech Stack:** Java 21、Apache HttpClient5 5.6、slf4j-api、JUnit 5、Lombok、Maven。

## Global Constraints

- 纯 Java 21，仅用 httpclient5 5.6 / slf4j-api / JUnit 5 / Lombok，不引入新依赖。
- 仅同步客户端；不暴露 HTTP 状态码到响应对象；不动连接池参数（maxTotal=200 / maxPerRoute=20 / 各超时）。
- `response` 包下的 `Headers`、`MinimalField`、`StringEntityResponse`、`FileEntityResponse` 保持不变。
- 提交信息用中文 conventional commits：`refactor(httpclient5):` / `fix(httpclient5):` / `test(httpclient5):` / `docs(httpclient5):`。
- 所有测试命令在仓库根执行：`mvn -pl cookbook-httpclient5 test`（单测加 `-Dtest=类名`）。

## File Structure

| 文件 | 职责 | 任务 |
|---|---|---|
| `HttpRequestConfig.java` | 请求配置 bean；DEFAULT 不可变 + `copy()` | Task 1 |
| `HeaderHelper.java` | 响应头解析；`filename*` 大小写不敏感 | Task 2 |
| `SyncClient.java` | 同步执行引擎；有界 payload 日志 `resolvePayloadForLog` | Task 3 |
| `HttpClientUtils.java` | 静态工厂 `get`/`post` + 嵌套 `RequestBuilder` | Task 4, 5 |
| `StringResponseHandler.java` / `StreamResponseHandler.java` / `FileDownloadResponseHandler.java` | 错误阈值 `>= 400` | Task 6 |
| `pom.xml` / `README.md` | slf4j-api 显式依赖 / 用法文档 | Task 7 |
| `HttpRequestConfigTest.java` / `HeaderHelperTest.java` / `SyncClientTest.java`(新) / `HttpClientUtilsTest.java` | 测试 | 1, 2, 3, 4, 5, 6 |

依赖顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7。Task 4 的 builder 依赖 Task 1 的 `copy()` 与 Task 3 的 `SyncClient`。

---

### Task 1: HttpRequestConfig — DEFAULT 不可变 + copy()

**Files:**
- Modify: `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HttpRequestConfig.java`
- Test: `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpRequestConfigTest.java`

**Interfaces:**
- Consumes: 无
- Produces: `HttpRequestConfig.copy()` → `HttpRequestConfig`（可变独立副本）；`DEFAULT.addHeader(...)` 抛 `UnsupportedOperationException`

- [ ] **Step 1: 写失败测试**

在 `HttpRequestConfigTest` 末尾追加：

```java
    @Test
    void defaultHeadersImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> HttpRequestConfig.DEFAULT.addHeader("X", "y"));
    }

    @Test
    void copyProducesMutableIndependentInstance() {
        HttpRequestConfig copy = HttpRequestConfig.DEFAULT.copy();
        copy.addHeader("X-Test", "value");
        copy.setRedirects(false);
        copy.setResponseTimeout(9_000L);

        assertTrue(HttpRequestConfig.DEFAULT.getHeaders().isEmpty());
        assertTrue(HttpRequestConfig.DEFAULT.isRedirects());
        assertEquals(3_000L, HttpRequestConfig.DEFAULT.getResponseTimeout());
        assertEquals(1, copy.getHeaders().size());
        assertFalse(copy.isRedirects());
        assertEquals(9_000L, copy.getResponseTimeout());
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HttpRequestConfigTest`
Expected: FAIL — `copy()` 不存在（编译错误）。

- [ ] **Step 3: 实现**

将 `HttpRequestConfig.java` 整体替换为：

```java
package io.ituknown.httpclient5;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.auth.CredentialsProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class HttpRequestConfig {
    static final HttpRequestConfig DEFAULT;

    static {
        DEFAULT = new HttpRequestConfig();
        DEFAULT.headers = Collections.unmodifiableMap(DEFAULT.headers);
    }

    /**
     * 是否允许请求重定向
     */
    private boolean redirects = true;
    /**
     * 从连接池获取连接超时时间（毫秒）
     */
    private long connectionRequestTimeout = 200L;
    /**
     * 等待目标服务器数据响应超时时间（毫秒）
     */
    private long responseTimeout = 3_000L;
    /**
     * 请求代理服务器
     */
    private String proxy;
    /**
     * 请求认证
     */
    private CredentialsProvider credentials;
    /**
     * 请求头
     */
    @Setter(AccessLevel.NONE)
    private Map<String, String> headers = new HashMap<>();

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * 返回一份可变的独立副本。DEFAULT 不可变，定制时应 copy 后再修改。
     */
    public HttpRequestConfig copy() {
        HttpRequestConfig c = new HttpRequestConfig();
        c.redirects = this.redirects;
        c.connectionRequestTimeout = this.connectionRequestTimeout;
        c.responseTimeout = this.responseTimeout;
        c.proxy = this.proxy;
        c.credentials = this.credentials;
        c.headers.putAll(this.headers);
        return c;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HttpRequestConfigTest`
Expected: PASS（全部用例含原有 defaultValues/setRedirects 等）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HttpRequestConfig.java \
        cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpRequestConfigTest.java
git commit -m "refactor(httpclient5): HttpRequestConfig DEFAULT 不可变并新增 copy()"
```

---

### Task 2: HeaderHelper.fileNameParse — 大小写不敏感 + filename* 优先

**Files:**
- Modify: `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HeaderHelper.java`
- Test: `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HeaderHelperTest.java`

**Interfaces:**
- Consumes: 无
- Produces: `HeaderHelper.fileNameParse(Header)` 支持 RFC 5987 小写 `filename*` 且优先于 `filename`

- [ ] **Step 1: 写失败测试**

在 `HeaderHelperTest` 的 `fileNameParseRFC5987` 后追加：

```java
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
    void fileNameParseUnknownCharsetFallsBackToUtf8() {
        Header header = new BasicHeader("Content-Disposition", "attachment; filename*=WTF-8''%E6%B5%8B.txt");
        assertEquals("测.txt", HeaderHelper.fileNameParse(header));
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HeaderHelperTest`
Expected: FAIL — `fileNameParseLowercaseFilenameStar` 返回 null（小写 `filename*=` 未命中）。

- [ ] **Step 3: 实现**

将 `HeaderHelper.fileNameParse` 方法替换为：

```java
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
```

并在 `HeaderHelper.java` import 区补：

```java
import java.nio.charset.Charset;
import java.util.Locale;
```

（`URLDecoder`、`StandardCharsets` 已有导入。）

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HeaderHelperTest`
Expected: PASS（含原有 fileNameParseStandardFilename/RFC5987/NullHeader 等）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HeaderHelper.java \
        cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HeaderHelperTest.java
git commit -m "fix(httpclient5): filename* 解析大小写不敏感并优先于 filename"
```

---

### Task 3: SyncClient — 有界 payload 日志

**Files:**
- Modify: `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/SyncClient.java`
- Test: `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/SyncClientTest.java`（新建）

**Interfaces:**
- Consumes: 无
- Produces: `SyncClient.resolvePayloadForLog(HttpEntity)` → `String`（package-private static，有界读取 ≤1000 字节）

- [ ] **Step 1: 写失败测试**

新建 `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/SyncClientTest.java`：

```java
package io.ituknown.httpclient5;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SyncClientTest {

    /** 记录实际读取字节数的 InputStream。 */
    static final class CountingInputStream extends FilterInputStream {
        final AtomicLong counter;

        CountingInputStream(InputStream in, AtomicLong counter) {
            super(in);
            this.counter = counter;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) counter.incrementAndGet();
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) counter.addAndGet(n);
            return n;
        }
    }

    /**
     * 测试用 entity：可配置 repeatable，每次 getContent() 返回新的计数流。
     * 注意 HC5 AbstractHttpEntity 无无参构造器，必须显式 super(ct, null)。
     */
    static final class TestEntity extends AbstractHttpEntity {
        private final byte[] data;
        private final AtomicLong counter;
        private final boolean repeatable;

        TestEntity(String text, ContentType ct, AtomicLong counter, boolean repeatable) {
            super(ct, null);
            this.data = text.getBytes(StandardCharsets.UTF_8);
            this.counter = counter;
            this.repeatable = repeatable;
        }

        @Override
        public boolean isRepeatable() {
            return repeatable;
        }

        @Override
        public long getContentLength() {
            return data.length;
        }

        @Override
        public InputStream getContent() {
            return new CountingInputStream(new ByteArrayInputStream(data), counter);
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        public void close() {
        }
    }

    @Test
    void payloadLogBoundedForLargeTextEntity() {
        String big = "x".repeat(10_000);
        AtomicLong counter = new AtomicLong();
        HttpEntity entity = new TestEntity(big, ContentType.TEXT_PLAIN, counter, true);

        String payload = SyncClient.resolvePayloadForLog(entity);

        int contentPart = payload.contains("... [truncated")
                ? payload.indexOf("... [truncated")
                : payload.length();
        assertTrue(contentPart <= 1000, "内容部分应 <= 1000, 实际 " + contentPart);
        assertTrue(payload.contains("[truncated"));
        assertTrue(counter.get() <= 1000, "底层读取应 <= 1000, 实际 " + counter.get());
    }

    @Test
    void payloadLogSkipsBinaryContentType() {
        String big = "x".repeat(10_000);
        AtomicLong counter = new AtomicLong();
        HttpEntity entity = new TestEntity(big, ContentType.APPLICATION_OCTET_STREAM, counter, true);

        String payload = SyncClient.resolvePayloadForLog(entity);

        assertEquals("Binary/Large Content", payload);
        assertEquals(0, counter.get(), "二进制类型不应触发读取");
    }

    @Test
    void payloadLogSkipsNonRepeatableEntity() {
        AtomicLong counter = new AtomicLong();
        HttpEntity entity = new TestEntity("x".repeat(10_000), ContentType.TEXT_PLAIN, counter, false);
        assertEquals("Binary/Large Content", SyncClient.resolvePayloadForLog(entity));
        assertEquals(0, counter.get(), "非 repeatable 不应触发读取");
    }

    @Test
    void payloadLogNullEntity() {
        assertEquals("Binary/Large Content", SyncClient.resolvePayloadForLog(null));
    }

    @Test
    void payloadLogSmallTextEntityFull() {
        HttpEntity entity = new StringEntity("hello", ContentType.TEXT_PLAIN);
        assertEquals("hello", SyncClient.resolvePayloadForLog(entity));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=SyncClientTest`
Expected: FAIL — `resolvePayloadForLog` 不存在（编译错误）。

- [ ] **Step 3: 实现**

在 `SyncClient.java` import 区，移除不再使用的 `import org.apache.hc.core5.http.io.entity.EntityUtils;`，新增：

```java
import java.io.InputStream;
import java.util.Locale;
```

将 `execute` 方法中的 payload 构造块（原 `String payload = "Binary/Large Content"; ...` 直到对应 `}` 的整块，即原文件 `if (LOGGER.isInfoEnabled())` 内、`String payload = ...` 起到 `payload` 计算结束的部分）替换为单行：

```java
                String payload = resolvePayloadForLog(requestBase.getEntity());
```

具体地，删除以下代码：

```java
                String payload = "Binary/Large Content";

                if (requestBase.getEntity() != null) {

                    HttpEntity entity = requestBase.getEntity();

                    // Entity 必须是常见的可读文本类型
                    if (entity.isRepeatable()) {
                        try {
                            // 限制读取长度，避免 EntityUtils.toString 加载超大文件到内存
                            String content = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                            if (content != null) {
                                payload = content.length() > 1000
                                        ? content.substring(0, 1000) + "... [Total: " + content.length() + "]"
                                        : content;
                            }
                        } catch (Exception e) {
                            payload = "[Error reading payload: " + e.getMessage() + "]";
                        }
                    }
                }
```

替换为：

```java
                String payload = resolvePayloadForLog(requestBase.getEntity());
```

在 `SyncClient` 类内（`execute` 方法之后）新增两个方法：

```java
    /**
     * 为日志生成请求体摘要：最多读取 1000 字节，避免大文件全量加载进内存。
     * 非 repeatable 或非文本 content-type 不读取，直接返回占位符。
     */
    static String resolvePayloadForLog(HttpEntity entity) {
        if (entity == null || !entity.isRepeatable()) {
            return "Binary/Large Content";
        }
        if (!isTextLike(entity)) {
            return "Binary/Large Content";
        }
        try (InputStream in = entity.getContent()) {
            byte[] sample = in.readNBytes(1000);
            if (sample.length == 0) {
                return "";
            }
            String content = new String(sample, StandardCharsets.UTF_8);
            long total = entity.getContentLength();
            if (total > 1000 || (total < 0 && sample.length == 1000)) {
                return content + "... [truncated, total: " + (total > 0 ? total : "unknown") + "]";
            }
            return content;
        } catch (Exception e) {
            return "[Error reading payload: " + e.getMessage() + "]";
        }
    }

    private static boolean isTextLike(HttpEntity entity) {
        String ct = entity.getContentType();
        if (ct == null) {
            return false;
        }
        String lower = ct.toLowerCase(Locale.ROOT);
        return lower.startsWith("text/")
                || lower.contains("application/json")
                || lower.contains("application/xml")
                || lower.contains("+json")
                || lower.contains("+xml")
                || lower.contains("application/x-www-form-urlencoded");
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=SyncClientTest`
Expected: PASS。再跑全量确认未破坏现有：`mvn -pl cookbook-httpclient5 test`，Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/SyncClient.java \
        cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/SyncClientTest.java
git commit -m "fix(httpclient5): payload 日志改为有界读取避免大文件 OOM"
```

---

### Task 4: RequestBuilder GET 路径 + 工厂 + 迁移 GET/stream/download/error 测试

**Files:**
- Modify: `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HttpClientUtils.java`（重写）
- Test: `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpClientUtilsTest.java`（重写为 fluent，本任务仅含 GET/stream/download/error 用例）

**Interfaces:**
- Consumes: `HttpRequestConfig.DEFAULT.copy()`（Task 1）、`SyncClient.execute(config, request, handler)`（Task 3）
- Produces: `HttpClientUtils.get(String)` / `post(String)` → `RequestBuilder`；`RequestBuilder.config/header/asString/stream/downloadTo/downloadToRemoteName`

- [ ] **Step 1: 写失败测试（重写测试文件为 fluent，仅 GET 家族）**

将 `HttpClientUtilsTest.java` 整体替换为：

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HttpClientUtilsTest`
Expected: FAIL — `HttpClientUtils.get(url).asString()` 不存在（编译错误，旧 API 是 `get(url)` 直接返回响应）。

- [ ] **Step 3: 实现（重写 HttpClientUtils，GET 路径，不含 body setter）**

将 `HttpClientUtils.java` 整体替换为：

```java
package io.ituknown.httpclient5;

import io.ituknown.httpclient5.response.FileDownloadResponseHandler;
import io.ituknown.httpclient5.response.FileEntityResponse;
import io.ituknown.httpclient5.response.Headers;
import io.ituknown.httpclient5.response.RemoteNameFileDownloadResponseHandler;
import io.ituknown.httpclient5.response.StreamResponseHandler;
import io.ituknown.httpclient5.response.StringEntityResponse;
import io.ituknown.httpclient5.response.StringResponseHandler;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.HttpEntity;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class HttpClientUtils {

    private HttpClientUtils() {
    }

    public static RequestBuilder get(String url) {
        return new RequestBuilder("GET", url);
    }

    public static RequestBuilder post(String url) {
        return new RequestBuilder("POST", url);
    }

    public static final class RequestBuilder {

        private final String method;
        private final String url;
        private HttpEntity entity;
        private HttpRequestConfig config;
        private final Map<String, String> headers = new LinkedHashMap<>();

        RequestBuilder(String method, String url) {
            this.method = method;
            this.url = url;
        }

        public RequestBuilder config(HttpRequestConfig config) {
            this.config = config;
            return this;
        }

        public RequestBuilder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public StringEntityResponse asString() {
            return SyncClient.execute(effectiveConfig(), buildRequest(), new StringResponseHandler());
        }

        public Headers stream(Consumer<InputStream> consumer) {
            return SyncClient.execute(effectiveConfig(), buildRequest(), new StreamResponseHandler(consumer));
        }

        public FileEntityResponse downloadTo(Path path) {
            return SyncClient.execute(effectiveConfig(), buildRequest(), new FileDownloadResponseHandler(path));
        }

        public FileEntityResponse downloadTo(String path) {
            return downloadTo(Path.of(path));
        }

        public FileEntityResponse downloadToRemoteName(Path dir) {
            return SyncClient.execute(effectiveConfig(), buildRequest(), new RemoteNameFileDownloadResponseHandler(dir, url));
        }

        private HttpUriRequestBase buildRequest() {
            if ("GET".equals(method)) {
                return new HttpGet(url);
            }
            HttpPost post = new HttpPost(url);
            if (entity != null) {
                post.setEntity(entity);
            }
            return post;
        }

        private HttpRequestConfig effectiveConfig() {
            HttpRequestConfig base = (config != null) ? config : HttpRequestConfig.DEFAULT;
            HttpRequestConfig effective = base.copy();
            headers.forEach(effective::addHeader);
            return effective;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HttpClientUtilsTest`
Expected: PASS（GET / stream / download / error 全部通过）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HttpClientUtils.java \
        cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpClientUtilsTest.java
git commit -m "refactor(httpclient5): 引入 RequestBuilder GET 路径 fluent API"
```

---

### Task 5: RequestBuilder POST body setter + 迁移 POST 测试

**Files:**
- Modify: `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HttpClientUtils.java`（在 `RequestBuilder` 内补 body setter）
- Test: `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpClientUtilsTest.java`（追加 POST 用例）

**Interfaces:**
- Consumes: Task 4 的 `RequestBuilder`
- Produces: `RequestBuilder.json(String)` / `json(byte[])` / `form(List)` / `form(List,Charset)` / `multipart(MultipartEntityBuilder)` / `file(File)` / `file(File,ContentType)` / `body(String,ContentType)` / `body(byte[],ContentType)`

- [ ] **Step 1: 写失败测试**

在 `HttpClientUtilsTest` 的 `testGetServerError` 之前（`// ========== Error handling ==========` 段之前）插入 POST 用例段：

```java
    // ========== POST JSON ==========

    @Test
    void testPostJsonString() {
        String json = "{\"name\":\"test\"}";
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo").json(json).asString();
        assertEquals("echo:" + json, result.getEntity());
    }

    @Test
    void testPostJsonStringWithConfig() {
        String json = "{\"name\":\"test\"}";
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo").json(json).config(config).asString();
        assertEquals("echo:" + json, result.getEntity());
    }

    @Test
    void testPostJsonBytes() {
        byte[] json = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo").json(json).asString();
        assertEquals("echo:" + new String(json, StandardCharsets.UTF_8), result.getEntity());
    }

    @Test
    void testPostJsonBytesWithConfig() {
        byte[] json = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo").json(json).config(config).asString();
        assertEquals("echo:" + new String(json, StandardCharsets.UTF_8), result.getEntity());
    }

    @Test
    void testPostJsonStringStream() {
        String json = "{\"stream\":true}";
        AtomicReference<String> captured = new AtomicReference<>();
        HttpClientUtils.post(baseUrl + "/post-echo").json(json).stream(in -> {
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
        HttpClientUtils.post(baseUrl + "/post-echo").json(json).stream(in -> {
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
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo").body("plain text", ContentType.TEXT_PLAIN).asString();
        assertEquals("echo:plain text", result.getEntity());
    }

    @Test
    void testPostStringWithConfig() {
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo").body("data", ContentType.TEXT_PLAIN).config(config).asString();
        assertEquals("echo:data", result.getEntity());
    }

    @Test
    void testPostBytes() {
        byte[] content = "binary-data".getBytes(StandardCharsets.UTF_8);
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo").body(content, ContentType.APPLICATION_OCTET_STREAM).asString();
        assertEquals("echo:binary-data", result.getEntity());
    }

    @Test
    void testPostBytesWithConfig() {
        byte[] content = "binary-data".getBytes(StandardCharsets.UTF_8);
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-echo").body(content, ContentType.APPLICATION_OCTET_STREAM).config(config).asString();
        assertEquals("echo:binary-data", result.getEntity());
    }

    @Test
    void testPostStringStream() {
        AtomicReference<String> captured = new AtomicReference<>();
        HttpClientUtils.post(baseUrl + "/post-echo").body("stream-text", ContentType.TEXT_PLAIN).stream(in -> {
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
        HttpClientUtils.post(baseUrl + "/post-echo").body(content, ContentType.APPLICATION_OCTET_STREAM).stream(in -> {
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
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-form").form(params).asString();
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
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-form").form(params).config(config).asString();
        assertTrue(result.getEntity().contains("key=value"));
    }

    @Test
    void testPostFormStream() {
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("q", "java"));
        AtomicReference<String> captured = new AtomicReference<>();
        HttpClientUtils.post(baseUrl + "/post-form").form(params).stream(in -> {
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
        org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder builder =
                org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder.create()
                        .addTextBody("field1", "value1", ContentType.TEXT_PLAIN)
                        .addTextBody("field2", "value2", ContentType.TEXT_PLAIN);
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-multipart").multipart(builder).asString();
        String entity = result.getEntity();
        assertTrue(entity.startsWith("multipart:"));
        assertTrue(entity.contains("multipart/form-data"));
    }

    @Test
    void testPostMultipartWithConfig() {
        org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder builder =
                org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder.create()
                        .addTextBody("name", "test", ContentType.TEXT_PLAIN);
        HttpRequestConfig config = new HttpRequestConfig();
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-multipart").multipart(builder).config(config).asString();
        assertTrue(result.getEntity().contains("multipart/form-data"));
    }

    @Test
    void testPostMultipartStream() {
        org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder builder =
                org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder.create()
                        .addTextBody("data", "hello", ContentType.TEXT_PLAIN);
        AtomicReference<String> captured = new AtomicReference<>();
        HttpClientUtils.post(baseUrl + "/post-multipart").multipart(builder).stream(in -> {
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
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-file").file(tempFile.toFile()).asString();
        assertTrue(result.getEntity().startsWith("received:"));
        assertTrue(result.getEntity().endsWith("bytes"));
    }

    @Test
    void testPostFileWithContentType() throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "upload", ".json");
        Files.writeString(tempFile, "{\"data\":1}");
        StringEntityResponse result = HttpClientUtils.post(baseUrl + "/post-file").file(tempFile.toFile(), ContentType.APPLICATION_JSON).asString();
        assertTrue(result.getEntity().startsWith("received:"));
    }

    @Test
    void testPostFileStream() throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "upload", ".txt");
        Files.writeString(tempFile, "stream-file-content");
        AtomicReference<String> captured = new AtomicReference<>();
        HttpClientUtils.post(baseUrl + "/post-file").file(tempFile.toFile()).stream(in -> {
            try {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        assertTrue(captured.get().startsWith("received:"));
    }
```

并在 Error handling 段内 `testGetServerError` 后追加：

```java
    @Test
    void testPostJsonServerError() {
        assertThrows(HttpException.class, () -> HttpClientUtils.post(baseUrl + "/error").json("{}").asString());
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HttpClientUtilsTest`
Expected: FAIL — `json`/`form`/`multipart`/`file`/`body` 方法不存在（编译错误）。

- [ ] **Step 3: 实现（补 body setter）**

在 `HttpClientUtils.java` import 区新增：

```java
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.File;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
```

在 `RequestBuilder` 类内、`config(...)` 方法之前插入 body setter 与私有 `setEntity`、`guessContentType`：

```java
        // ---- body setter (仅 POST) ----

        public RequestBuilder json(String json) {
            return setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        }

        public RequestBuilder json(byte[] json) {
            return setEntity(new ByteArrayEntity(json, ContentType.APPLICATION_JSON));
        }

        public RequestBuilder form(List<? extends NameValuePair> params) {
            return form(params, StandardCharsets.UTF_8);
        }

        public RequestBuilder form(List<? extends NameValuePair> params, Charset charset) {
            return setEntity(new UrlEncodedFormEntity(params, charset));
        }

        public RequestBuilder multipart(MultipartEntityBuilder builder) {
            return setEntity(builder.build());
        }

        public RequestBuilder file(File file) {
            return file(file, guessContentType(file));
        }

        public RequestBuilder file(File file, ContentType contentType) {
            return setEntity(new FileEntity(file, contentType));
        }

        public RequestBuilder body(String content, ContentType contentType) {
            return setEntity(new StringEntity(content, contentType));
        }

        public RequestBuilder body(byte[] content, ContentType contentType) {
            return setEntity(new ByteArrayEntity(content, contentType));
        }

        private RequestBuilder setEntity(HttpEntity entity) {
            if (!"POST".equals(method)) {
                throw new IllegalStateException("GET request does not support a request body");
            }
            this.entity = entity;
            return this;
        }
```

并在 `HttpClientUtils` 类内（`RequestBuilder` 之外，工厂方法之后）新增静态 helper：

```java
    private static ContentType guessContentType(File file) {
        String type = URLConnection.guessContentTypeFromName(file.getName());
        return ContentType.create(type != null ? type : "application/octet-stream");
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HttpClientUtilsTest`
Expected: PASS（全部 GET + POST 用例）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HttpClientUtils.java \
        cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpClientUtilsTest.java
git commit -m "refactor(httpclient5): RequestBuilder 补全 POST body setter"
```

---

### Task 6: 响应错误阈值 >= 400 + redirect 用例

**Files:**
- Modify: `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/StringResponseHandler.java`
- Modify: `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/StreamResponseHandler.java`
- Modify: `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/FileDownloadResponseHandler.java`
- Test: `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpClientUtilsTest.java`

**Interfaces:**
- Consumes: Task 5 的 fluent API
- Produces: 三个 handler 在 `code >= 400` 才抛 `HttpResponseException`；3xx 返回响应

- [ ] **Step 1: 写失败测试**

在 `HttpClientUtilsTest.registerEndpoints()` 内追加 302 endpoint：

```java
        server.createContext("/redirect-302", exchange -> {
            exchange.getResponseHeaders().put("Location", List.of(baseUrl + "/get"));
            byte[] body = "redirect-body".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(302, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
```

并在 Error handling 段追加：

```java
    @Test
    void testRedirectNotTreatedAsError() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setRedirects(false);
        // 302 在 >= 400 阈值下不再当错误抛出
        StringEntityResponse result = HttpClientUtils.get(baseUrl + "/redirect-302").config(config).asString();
        assertNotNull(result);
        assertEquals("redirect-body", result.getEntity());
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-httpclient5 test -Dtest=HttpClientUtilsTest`
Expected: FAIL — `testRedirectNotTreatedAsError` 抛 `HttpException`（StringResponseHandler 经 `AbstractHttpClientResponseHandler` 在 `>= 300` 即抛）。

- [ ] **Step 3: 实现**

**3a.** 将 `StringResponseHandler.java` 的 `handleResponse` 方法替换为（不再依赖父类 `>= 300` 检查）：

```java
    @Override
    public StringEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        int statusCode = response.getCode();

        if (statusCode >= 400) {
            EntityUtils.consume(response.getEntity());
            LOGGER.warn("HTTP Failed [{}], Reason: {}", statusCode, response.getReasonPhrase());
            throw new HttpResponseException(statusCode, response.getReasonPhrase());
        }

        HttpEntity entity = response.getEntity();
        StringEntityResponse result = (entity == null)
                ? new StringEntityResponse(null)
                : handleEntity(entity);
        result.setHeaders(HeaderHelper.resolveHeader(response));

        if (LOGGER.isInfoEnabled()) {
            String body = result.getEntity();
            String logContent = (body != null && body.length() > 1000)
                    ? body.substring(0, 1000) + "... [truncated, total: " + body.length() + "]"
                    : body;
            LOGGER.info("HTTP Success [{}], Content: {}", statusCode, logContent);
        }

        return result;
    }
```

（`handleEntity`、类声明、`@Contract` 注解、import 保持不变。）

**3b.** `StreamResponseHandler.java`：将 `if (response.getCode() >= 300)` 改为 `if (response.getCode() >= 400)`。

**3c.** `FileDownloadResponseHandler.java`：将 `if (response.getCode() >= 300)` 改为 `if (response.getCode() >= 400)`。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-httpclient5 test`
Expected: PASS（全模块，含 redirect 用例与原有 500 错误用例仍抛异常）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/StringResponseHandler.java \
        cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/StreamResponseHandler.java \
        cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/FileDownloadResponseHandler.java \
        cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpClientUtilsTest.java
git commit -m "fix(httpclient5): 响应错误阈值改为 >= 400，3xx 不再当错误抛出"
```

---

### Task 7: pom.xml 显式 slf4j-api + README 用法

**Files:**
- Modify: `cookbook-httpclient5/pom.xml`
- Modify: `cookbook-httpclient5/README.md`

**Interfaces:**
- Consumes: 无
- Produces: 显式 `slf4j-api` 依赖；README 用法示例

- [ ] **Step 1: 改 pom.xml**

在 `<properties>` 内追加：

```xml
        <slf4j-api.version>2.0.17</slf4j-api.version>
```

在 `<dependencies>` 内、httpclient5 依赖之后追加：

```xml
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j-api.version}</version>
            <scope>compile</scope>
        </dependency>
```

- [ ] **Step 2: 重写 README.md**

将 `README.md` 整体替换为：

````markdown
# cookbook-httpclient5

Apache HttpClient 5 的同步封装，提供 fluent builder API。

## 依赖

```xml
<dependency>
    <groupId>io.ituknown</groupId>
    <artifactId>cookbook-httpclient5</artifactId>
    <version>${revision}</version>
</dependency>
```

## 用法

### GET

```java
StringEntityResponse r = HttpClientUtils.get(url).asString();
Headers headers = HttpClientUtils.get(url).stream(in -> read(in));
```

### POST JSON / Form / Multipart / File

```java
HttpClientUtils.post(url).json(json).asString();
HttpClientUtils.post(url).form(params).stream(in -> read(in));
HttpClientUtils.post(url).multipart(MultipartEntityBuilder.create().addTextBody("k", "v")).asString();
HttpClientUtils.post(url).file(new File("a.txt")).asString();
HttpClientUtils.post(url).body(bytes, ContentType.APPLICATION_OCTET_STREAM).asString();
```

### 下载

```java
HttpClientUtils.get(url).downloadTo(Path.of("/tmp/a.txt"));
HttpClientUtils.get(url).downloadToRemoteName(Path.of("/tmp"));
```

### 带 config

```java
HttpRequestConfig config = HttpRequestConfig.DEFAULT.copy();
config.setProxy("127.0.0.1:8080");
config.addHeader("Authorization", "Bearer xxx");
HttpClientUtils.get(url).config(config).asString();
```

## 参考

- https://hc.apache.org/index.html
- https://javadoc.io/doc/org.apache.httpcomponents.client5/httpclient5/latest/index.html
````

- [ ] **Step 3: 验证构建与全量测试**

Run: `mvn -pl cookbook-httpclient5 test`
Expected: BUILD SUCCESS，全部测试 PASS。

- [ ] **Step 4: 提交**

```bash
git add cookbook-httpclient5/pom.xml cookbook-httpclient5/README.md
git commit -m "docs(httpclient5): 显式 slf4j-api 依赖并补 README 用法"
```
