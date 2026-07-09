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
HttpRequestConfig config = new HttpRequestConfig();
config.setProxy("127.0.0.1:8080");
config.addHeader("Authorization", "Bearer xxx");
HttpClientUtils.get(url).config(config).asString();
```

## 参考

- https://hc.apache.org/index.html
- https://javadoc.io/doc/org.apache.httpcomponents.client5/httpclient5/latest/index.html
