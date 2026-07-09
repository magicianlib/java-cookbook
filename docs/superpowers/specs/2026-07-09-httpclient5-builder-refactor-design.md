# cookbook-httpclient5 Fluent Builder 重构设计

> **背景**：现有 `HttpClientUtils` 以 ~30 个静态重载覆盖 GET / 下载 / postJson / post / postForm /
> postMultipart / postFile，每个方法再按「默认 config / 显式 config」×「String 响应 / stream 消费」
> 笛卡尔展开，是典型的 telescoping overload 反模式——新增一个维度（charset、自定义 handler）即翻倍。
> 同时存在三处真实缺陷：请求体日志会把整个大文件读进内存（注释与实现矛盾）、`filename*` 解析大小写敏感
> 漏掉真实服务器响应、禁用重定向时 3xx 被当错误抛出。
>
> 已确认：除 `HttpClientUtils` 及其测试外**无外部调用方**，可做不兼容变更。本次**保留底层骨架**
>（`SyncClient`、`Headers`、`MinimalField`、各 `*ResponseHandler`、`HeaderHelper`、`HttpRequestConfig`），
> 仅替换入口层并修缺陷，不推倒重写。

## 目标

- 用**纯 fluent builder** 取代 ~30 个静态重载：`HttpClientUtils.get(url)` / `post(url)` 返回 `RequestBuilder`，
  body / config / 终结操作正交组合，彻底消灭重载爆炸。
- 修三处缺陷：payload 日志有界读取、`filename*` 大小写不敏感、响应错误阈值 `>= 400`。
- 修 `HttpRequestConfig.DEFAULT` 共享可变脚枪。
- 显式声明 `slf4j-api`、补 README 用法。

## 约束

- 纯 Java、Java 21、httpclient5 5.6、slf4j-api、JUnit 5、Lombok。
- 仅同步客户端（不加 async——独立议题）。
- 不暴露 HTTP 状态码到响应对象（保持「非 4xx 即成功」语义）。
- 不动连接池参数（maxTotal=200 / maxPerRoute=20 / 各超时）——属调参，不在本次范围。
- 底层响应处理器、`Headers`、`MinimalField`、`StringEntityResponse`、`FileEntityResponse` **保持不变**。

## 核心决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 入口形态 | 纯 fluent，无快捷方法 | Java 不能按返回类型重载，`get(url)` 无法同时返回响应与 builder；纯 fluent 单一风格，最彻底消灭重载 |
| Builder 归属 | `HttpClientUtils` 内 public 嵌套静态类 `RequestBuilder` | 与静态工厂 `get`/`post` 同文件共存，便于发现；不新增顶层类型 |
| body 维度 | `.json()/.form()/.multipart()/.file()/.body()` setter | 与 method 正交，POST 专属 |
| 响应维度 | `.asString()/.stream()/.downloadTo()/.downloadToRemoteName()` 终结操作 | 终结即执行，触发 `SyncClient.execute` |
| config 注入 | `.config(HttpRequestConfig)` + `.header(k,v)` | 整体传入复用现有 bean；`header` 累加到 builder 内部，与 config.headers 合并下发 |
| DEFAULT 可变性 | headers 不可变，`addHeader` 在 DEFAULT 上抛异常 | 防止 `DEFAULT.addHeader(...)` 污染全局；定制走 `DEFAULT.copy()` 或 `new` |
| 错误阈值 | `>= 400` 抛 `HttpResponseException` | 3xx/304 非错误；开重定向（默认）时无影响，禁用时返回响应而非抛 |
| payload 日志 | `readNBytes(1000)` 有界读取 | 修 OOM：原 `EntityUtils.toString` 全量读入，大文件上传即崩 |
| `filename*` | 大小写不敏感，且优先于 `filename` | RFC 5987 规定小写 `filename*` 且优先级高于 `filename` |

## 架构

```
调用方
  │  HttpClientUtils.get(url)  ──┐
  │  HttpClientUtils.post(url) ──┤  返回 RequestBuilder
  │                               │
  │  .json(s)/.form(...)/.file(...)/.body(...)   设置请求体（POST 专属）
  │  .config(HttpRequestConfig)/.header(k,v)     配置 / 请求头
  │  .asString()/.stream(c)/.downloadTo(p)/.downloadToRemoteName(d)  终结→执行
  ▼
RequestBuilder：累积 method/url/entity/effectiveConfig/headers，终结时
  构造 HttpGet/HttpPost + 合并 config → SyncClient.execute(config, request, handler)
  │
  ▼
SyncClient（不变）：连接池单例 + RequestConfig 装配 + 有界 payload 日志 + execute
  │
  ▼
*ResponseHandler（阈值改 >= 400）/ Headers / HeaderHelper（filename* 修复）
```

## 组件

### 1. `HttpClientUtils`（重写）

仅保留两个静态工厂 + 私有 `guessContentType(File)`：

```java
public static RequestBuilder get(String url)   // method = GET
public static RequestBuilder post(String url)   // method = POST
```

删除全部旧重载（`get(url,config)`、`postJson(...)` ×8、`post(...)` ×12、`postForm(...)` ×6、
`postMultipart(...)` ×4、`postFile(...)` ×6、`download(...)` ×4、`getStream(...)` ×4 等）。

### 2. `RequestBuilder`（新增，`HttpClientUtils` 内 public static）

**状态**：method（GET/POST）、url、`HttpEntity entity`（可空）、`HttpRequestConfig config`（可空）、
`Map<String,String> headers`（builder 内部累加，默认空）。

**body setter**（仅 POST；GET 上调用抛 `IllegalStateException`「GET 不支持请求体」）：

```java
RequestBuilder json(String json)                                   // StringEntity(json, APPLICATION_JSON)
RequestBuilder json(byte[] json)                                   // ByteArrayEntity(json, APPLICATION_JSON)
RequestBuilder form(List<? extends NameValuePair> params)          // UrlEncodedFormEntity(params, UTF_8)
RequestBuilder form(List<? extends NameValuePair> params, Charset charset)
RequestBuilder multipart(MultipartEntityBuilder builder)           // builder.build()
RequestBuilder file(File file)                                     // FileEntity(file, guessContentType(file))
RequestBuilder file(File file, ContentType contentType)
RequestBuilder body(String content, ContentType contentType)       // StringEntity
RequestBuilder body(byte[] content, ContentType contentType)       // ByteArrayEntity
```

body setter 重复调用：后者覆盖前者（last wins）。

**config / header**：

```java
RequestBuilder config(HttpRequestConfig config)   // 替换 config（last wins）
RequestBuilder header(String key, String value)   // 累加到 builder 内部 headers
```

**终结操作**（构造 request + 合并 effectiveConfig → `SyncClient.execute`）：

```java
StringEntityResponse asString()                                  // StringResponseHandler
Headers            stream(Consumer<InputStream> consumer)        // StreamResponseHandler
FileEntityResponse downloadTo(Path path)                         // FileDownloadResponseHandler
FileEntityResponse downloadTo(String path)                       // 委托 downloadTo(Paths.get(path))
FileEntityResponse downloadToRemoteName(Path dir)                // RemoteNameFileDownloadResponseHandler(dir, url)
```

**effectiveConfig 合并**（终结时）：

```java
HttpRequestConfig base = (this.config != null) ? this.config : HttpRequestConfig.DEFAULT;
HttpRequestConfig effective = base.copy();          // 可变副本，DEFAULT 不可变也能 copy
this.headers.forEach(effective::addHeader);         // builder headers 叠加在 config.headers 之上
```

`SyncClient.execute(effective, request, handler)` 签名与行为不变。

### 3. `SyncClient`（修 payload 日志，其余不变）

抽出 package-private `resolvePayloadForLog(HttpEntity)`，替换原 `EntityUtils.toString` 全量读：

- entity 为 null / 非 repeatable / content-type 非文本类（非 `text/*`、`application/json`、
  `application/xml`、`+json`、`+xml`）→ 返回 `"Binary/Large Content"`，不读取。
- 否则 `try (InputStream in = entity.getContent()) { byte[] sample = in.readNBytes(1000); ... }`
  有界读取 ≤1000 字节，按 UTF-8 解码；超长追加 `... [truncated, total: <len>]`（len 取
  `entity.getContentLength()`，未知则 `unknown`）。
- repeatable entity 的 `getContent()` 每次返回新流，日志读取不影响真实发送；流必须 try-with-resources 关闭。
- 读取异常 → `"[Error reading payload: <msg>]"`。

该方法可单测：喂一个 10MB 的 repeatable 文本 entity，断言返回内容 ≤1000 字符、且底层流读取字节数 ≤1000
（用计数 `InputStream` 验证有界）。

### 4. `HttpRequestConfig`（DEFAULT 不可变 + 新增 `copy()`）

- `headers` 字段改非 final，加 `@Setter(AccessLevel.NONE)`（保留 `addHeader` 为唯一写入入口，无 `setHeaders` 旁路）。
- 静态初始化块将 `DEFAULT.headers` 包裹为 `Collections.unmodifiableMap`：`DEFAULT.addHeader(...)` 抛
  `UnsupportedOperationException`。
- 新增 `copy()`：返回可变副本，逐字段复制（`redirects`/`connectionRequestTimeout`/`responseTimeout`/
  `proxy`/`credentials`），headers 复制进新 `HashMap`。`DEFAULT.copy()` 即「以默认值起一份可变副本」。
- 其余 API（getter/setter/字段默认值）不变。

### 5. `HeaderHelper.fileNameParse`（`filename*` 大小写不敏感 + 优先）

重写解析顺序（RFC 5987：`filename*` 优先于 `filename`）：

1. `headerValue` 小写化后定位 `filename*=`（大小写不敏感）；命中则取其后至 `;` 的片段，
   按 `charset''pct-encoded` 拆分：`charset` 用 `Charset.forName` 解析（未知则回退 UTF-8），
   `pct-encoded` 部分用该 charset `URLDecoder.decode`。
2. 未命中 `filename*` → 用 HC5 `BasicHeaderValueParser` 解析 `filename=`（`equalsIgnoreCase`，原有逻辑）。
3. 都没有 → null。

### 6. 三个 `*ResponseHandler`（错误阈值 `>= 300` → `>= 400`）

- `StringResponseHandler`：重写 `handleResponse`，先判 `code >= 400` → `EntityUtils.consume` + 抛
  `HttpResponseException`；否则取 entity（null 安全：entity 为 null 时 `new StringEntityResponse(null)`）
  → `handleEntity` → `setHeaders` → 日志。不再依赖 `AbstractHttpClientResponseHandler` 的 `>= 300` 检查。
- `StreamResponseHandler` / `FileDownloadResponseHandler`：将 `code >= 300` 改为 `code >= 400`，其余不变。

### 7. `pom.xml` / `README.md`

- `pom.xml`：显式声明 `org.slf4j:slf4j-api`（与 `cookbook-okhttp` 对齐，不再靠 httpclient5 传递引入）。
- `README.md`：补 fluent 用法示例（GET / POST JSON / form / 上传文件 / 下载 / stream 消费 / 带 config）。

## 错误处理

| 场景 | 处理 |
|---|---|
| GET 上调用 body setter | `IllegalStateException`（编程错误，快速失败） |
| POST 未设 body | 构造无 entity 的 `HttpPost`（HTTP 允许），由服务器决定 |
| 响应 `code >= 400` | `EntityUtils.consume` + 抛 `HttpResponseException` → `SyncClient` 包 `HttpException` |
| 响应 3xx（禁用重定向时） | 不抛，返回响应（通常 entity 为空） |
| `execute` 任一异常 | `SyncClient` 记 ERROR 日志后包 `HttpException` 抛出（不变） |
| payload 日志读取异常 | 不影响请求，payload 记为 `[Error reading payload: ...]` |
| `DEFAULT.addHeader(...)` | `UnsupportedOperationException`（脚枪防护） |

## 测试

- **`HttpClientUtilsTest`（重写）**：内嵌 `HttpServer` 与全部 endpoint 保留，所有调用改为 fluent 形态，
  断言不变（覆盖 GET / GET+config / stream / postJson(String,byte[]) / post / postForm / postMultipart /
  postFile / download / downloadUseRemoteName / 错误路径）。
- **新增 `testRedirectNotTreatedAsError`**：新增 `/redirect-302` endpoint 返回 302；用
  `new HttpRequestConfig().setRedirects(false)` 断言返回响应而非抛 `HttpException`。
- **新增 `SyncClientResolvePayloadTest`（或并入 `SyncClientTest`）**：
  - 大文本 repeatable entity → 返回内容 ≤1000 字符且含 truncation 标记。
  - 计数 `InputStream` 验证底层读取字节数 ≤1000。
  - 非 repeatable / 二进制 content-type → 返回 `"Binary/Large Content"` 且不触发读取。
- **`HeaderHelperTest` 新增**：
  - `fileNameParseLowercaseFilenameStar`：`attachment; filename*=utf-8''%e6%b5%8b%e8%af%95.txt` → `测试.txt`。
  - `fileNameParseStarPrecedence`：同时含 `filename="a.txt"; filename*=UTF-8''b.txt` → `b.txt`（优先级）。
- **`HttpRequestConfigTest` 新增**：`defaultHeadersImmutable` 断言 `DEFAULT.addHeader(...)` 抛
  `UnsupportedOperationException`；`copyProducesMutableIndependentInstance` 断言 copy 可写且不影响 DEFAULT。
- `HeadersTest` / `MinimalFieldTest` 不变。

## 文件清单

- 重写 `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HttpClientUtils.java`
- 新增 `RequestBuilder`（`HttpClientUtils` 内 public static 嵌套类，同文件）
- 改 `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/SyncClient.java`（payload 日志）
- 改 `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HttpRequestConfig.java`（DEFAULT 不可变 + copy）
- 改 `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/HeaderHelper.java`（filename*）
- 改 `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/StringResponseHandler.java`（阈值）
- 改 `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/StreamResponseHandler.java`（阈值）
- 改 `cookbook-httpclient5/src/main/java/io/ituknown/httpclient5/response/FileDownloadResponseHandler.java`（阈值）
- 改 `cookbook-httpclient5/pom.xml`（slf4j-api）
- 改 `cookbook-httpclient5/README.md`（用法示例）
- 重写 `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpClientUtilsTest.java`
- 新增 `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/SyncClientTest.java`（payload 日志单测）
- 改 `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HeaderHelperTest.java`
- 改 `cookbook-httpclient5/src/test/java/io/ituknown/httpclient5/HttpRequestConfigTest.java`

## 迁移说明（旧 → 新）

| 旧 API | 新 API |
|---|---|
| `get(url)` / `get(url, config)` | `get(url).asString()` / `get(url).config(c).asString()` |
| `getStream(url, c)` / `getStream(url, config, c)` | `get(url).stream(c)` / `get(url).config(c).stream(c)` |
| `postJson(url, json)` / `+config` | `post(url).json(json).asString()` / `.config(c)` |
| `postJson(url, json, c)` (stream) | `post(url).json(json).stream(c)` |
| `postJson(url, bytes)` | `post(url).json(bytes).asString()` |
| `post(url, content, ct)` / `+config` | `post(url).body(content, ct).asString()` |
| `post(url, bytes, ct)` / `+config,charset` | `post(url).body(bytes, ct).asString()`（charset 由 entity 自带） |
| `postForm(url, params)` / `+config,charset` | `post(url).form(params).asString()` / `.form(params, charset)` |
| `postMultipart(url, builder)` | `post(url).multipart(builder).asString()` |
| `postFile(url, file)` / `+contentType` | `post(url).file(file).asString()` / `.file(file, ct)` |
| `download(url, path)` / `+config` | `get(url).downloadTo(path)` / `.config(c)` |
| `downloadUseRemoteName(url, dir)` / `+config` | `get(url).downloadToRemoteName(dir)` / `.config(c)` |

## 非目标（YAGNI）

- 异步客户端（独立议题）。
- 响应对象暴露 HTTP 状态码（保持「非 4xx 即成功」语义，需另设计）。
- 连接池参数调优。
- 自定义 `HttpClientResponseHandler` 终结操作（可后续按需加 `.execute(handler)`）。
- HTTP/2、Cookie store、拦截器等高级特性。
