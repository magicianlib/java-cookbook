# RsaUtils 完全重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `cookbook-crypto` 的 RSA 能力重构为 `RsaKeys`（密钥加载/解析/生成）+ `RsaUtils`（OAEP 加解密），支持 PKCS#8/X.509 的 PEM/DER 文件与字符串入参。

**Architecture:** 新增 `RsaKeys`（`final` + 私有构造）内聚密钥加载/解析/生成；`RsaUtils` 重写为纯加解密（OAEP-SHA256+MGF1-SHA256，限长，精确异常，日志，null 校验）。`Production` 保留供 `AesUtils`/`HashWithRsa` 使用；`HashWithRsa` 不动。测试补齐 round-trip 与各类加载/错误路径。

**Tech Stack:** Java 21、JDK 内置 `java.security`/`javax.crypto`（不引入 BouncyCastle）、slf4j-api、JUnit 5。设计依据：`docs/superpowers/specs/2026-06-17-rsautils-refactor-design.md`。

---

## 文件结构

| 文件 | 职责 | 动作 |
|---|---|---|
| `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java` | 密钥加载（Path/InputStream）、解析（PEM/Base64 字符串）、生成（KeyPair/RsaKeyPair） | 新建 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaUtils.java` | OAEP 加解密 + 限长 + 安全常量 | 重写 |
| `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java` | 加载/解析/生成 测试 | 新建 |
| `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaUtilsTest.java` | 加解密 round-trip/限长/错误路径 测试 | 重写 |

包内既有约定（实现须遵循）：
- `Base64.toString(byte[])` / `Base64.toByte(String)` 已存在。
- `Production<R>`（`@FunctionalInterface extends Function<byte[], R>`）已存在，仍被 `AesUtils`/`HashWithRsa` 使用，**不要删除**。
- `AesUtils` 使用 `org.slf4j.Logger`，日志风格以此为基准。

测试运行命令（模块根 `java-cookbook`）：`mvn -pl cookbook-crypto -am test`

---

## Task 1: 基础设施 —— `RsaKeys` 常量与 PEM 解析原语

**Files:**
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java`

本任务先落 `RsaKeys` 的骨架：`final` 类 + 私有构造 + 常量 + 包私有的 PEM 识别/解码原语，并用一个聚焦的单元测试锁住 PEM 解码行为。密钥构建/加载/生成方法留到后续任务。

- [ ] **Step 1: 写失败测试 —— PEM 识别与 Base64 内容提取**

创建 `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java`：

```java
package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RsaKeysTest {

    /** 包私有原语：应把 PEM（含头尾、含换行）还原为 DER 字节。 */
    @Test
    public void testExtractPemContentStripsHeaders() {
        String pem = """
                -----BEGIN PRIVATE KEY-----
                AAAB
                CCDE
                -----END PRIVATE KEY-----
                """;
        // 去头尾后拼接 AAABCCDE
        byte[] expected = Base64.toByte("AAABCCDE");
        byte[] actual = RsaKeys.extractPemContent(pem.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        Assertions.assertArrayEquals(expected, actual);
    }

    /** 非 PEM（裸 DER 二进制）原样返回。 */
    @Test
    public void testExtractPemContentPassesThroughDer() {
        byte[] der = {0x30, (byte) 0x82, 0x01, 0x22, 0x02, 0x01, 0x00};
        byte[] actual = RsaKeys.extractPemContent(der);
        Assertions.assertArrayEquals(der, actual);
    }

    @Test
    public void testIsPemDetectsHeader() {
        Assertions.assertTrue(RsaKeys.isPem("-----BEGIN PRIVATE KEY-----\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        Assertions.assertFalse(RsaKeys.isPem(new byte[]{0x30, (byte) 0x82}));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaKeysTest -q`
Expected: 编译失败 —— `RsaKeys` 不存在。

- [ ] **Step 3: 写最小实现**

创建 `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java`：

```java
package io.ituknown.crypto;

import java.nio.charset.StandardCharsets;

/**
 * RSA 密钥加载、解析与生成工具类。
 * <p>
 * 支持：
 * <ul>
 *   <li>私钥：{@code PKCS#8} 编码，PEM（{@code -----BEGIN PRIVATE KEY-----}）或 DER（二进制）</li>
 *   <li>公钥：{@code X.509 SubjectPublicKeyInfo} 编码，PEM（{@code -----BEGIN PUBLIC KEY-----}）或 DER（二进制）</li>
 * </ul>
 * 加载方法会按内容<b>自动识别</b> PEM（文本）与 DER（二进制）：内容以 ASCII
 * {@code -----BEGIN } 开头视为 PEM，否则视为 DER。
 * <p>
 * <b>不支持 PKCS#1</b>（{@code -----BEGIN RSA PRIVATE KEY-----}），该格式需要 BouncyCastle，本期不在范围内。
 */
public final class RsaKeys {

    private RsaKeys() {
    }

    /** PEM 头部前缀（ASCII）。 */
    static final String PEM_BEGIN = "-----BEGIN ";

    /**
     * 判断给定内容是否为 PEM 文本。
     *
     * @param content 原始字节
     * @return 内容以 {@code -----BEGIN } 开头时返回 true
     */
    static boolean isPem(byte[] content) {
        byte[] marker = PEM_BEGIN.getBytes(StandardCharsets.US_ASCII);
        if (content.length < marker.length) {
            return false;
        }
        for (int i = 0; i < marker.length; i++) {
            if (content[i] != marker[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从 PEM 文本中提取 DER 字节：去除 {@code -----BEGIN-----} / {@code -----END-----} 行，
     * 拼接其余行后 Base64 解码。非 PEM 内容原样返回。
     *
     * @param content 原始字节（可能为 PEM 或 DER）
     * @return DER 字节
     */
    static byte[] extractPemContent(byte[] content) {
        if (!isPem(content)) {
            return content;
        }
        String text = new String(content, StandardCharsets.US_ASCII);
        StringBuilder base64 = new StringBuilder();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-----") || trimmed.isEmpty()) {
                continue; // 跳过头尾行与空行
            }
            base64.append(trimmed);
        }
        return Base64.toByte(base64.toString());
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaKeysTest -q`
Expected: PASS（3 个测试）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java
git commit -m "feat(crypto): add RsaKeys PEM detection primitives"
```

---

## Task 2: `RsaKeys` —— 密钥构建（PKCS#8 / X.509）

**Files:**
- Modify: `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java`

加入把 DER 字节构造成 `PrivateKey`/`PublicKey` 的包私有原语，并用动态生成的密钥验证 `buildPublicKey`（避免硬编码密钥失效）。

- [ ] **Step 1: 写失败测试 —— 从 DER 字节构建公钥**

在 `RsaKeysTest` 新增测试：

```java
    @Test
    public void testBuildPublicKeyFromDer() throws Exception {
        java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        java.security.PublicKey original = g.generateKeyPair().getPublic();
        java.security.PublicKey rebuilt = RsaKeys.buildPublicKey(original.getEncoded());
        Assertions.assertEquals(original, rebuilt);
        Assertions.assertEquals("X.509", rebuilt.getFormat());
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaKeysTest -q`
Expected: 编译失败 —— `buildPublicKey` 未定义。

- [ ] **Step 3: 写实现**

在 `RsaKeys.java` 顶部 `import` 区追加：

```java
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
```

在类内（`extractPemContent` 之后）追加：

```java
    private static final String RSA = "RSA";

    /**
     * 用 PKCS#8 DER 字节构建 RSA 私钥。
     *
     * @param derKey PKCS#8 编码的私钥字节
     * @return RSA 私钥
     * @throws GeneralSecurityException 密钥格式非法时抛出
     */
    static PrivateKey buildPrivateKey(byte[] derKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(derKey));
    }

    /**
     * 用 X.509 SubjectPublicKeyInfo DER 字节构建 RSA 公钥。
     *
     * @param derKey X.509 编码的公钥字节
     * @return RSA 公钥
     * @throws GeneralSecurityException 密钥格式非法时抛出
     */
    static PublicKey buildPublicKey(byte[] derKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(derKey));
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaKeysTest -q`
Expected: PASS（4 个测试）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java
git commit -m "feat(crypto): add RsaKeys key builders (PKCS#8/X.509)"
```

---

## Task 3: `RsaKeys` —— 公开加载/解析方法（Path/InputStream/字符串）

**Files:**
- Modify: `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java`

公开密钥加载入口：`loadPrivateKey/loadPublicKey`（Path/InputStream），`parsePrivateKeyPem/parsePublicKeyPem`（PEM 字符串），`parsePrivateKeyBase64/parsePublicKeyBase64`（裸 Base64）。
**`InputStream` 不 close**，Javadoc 明确「调用方需自行 close」。

- [ ] **Step 1: 写失败测试 —— 各入口动态生成密钥后回读**

在 `RsaKeysTest` 加 `import`：

```java
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
```

新增测试（动态生成密钥对，避免硬编码失效）：

```java
    private static KeyPair newKeyPair() throws Exception {
        java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    @Test
    public void testLoadPrivateKeyPkcs8PemFromPath() throws Exception {
        KeyPair kp = newKeyPair();
        Path tmp = Files.createTempFile("rsa-priv", ".pem");
        Files.writeString(tmp, toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        PrivateKey key = RsaKeys.loadPrivateKey(tmp);
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testLoadPrivateKeyPkcs8DerFromStream() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] der = kp.getPrivate().getEncoded();
        PrivateKey key;
        try (var in = new ByteArrayInputStream(der)) {
            key = RsaKeys.loadPrivateKey(in);
        }
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testLoadPublicKeyX509PemFromStream() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] pem = toPem("PUBLIC KEY", kp.getPublic().getEncoded()).getBytes(StandardCharsets.US_ASCII);
        PublicKey key;
        try (var in = new ByteArrayInputStream(pem)) {
            key = RsaKeys.loadPublicKey(in);
        }
        Assertions.assertEquals(kp.getPublic(), key);
    }

    @Test
    public void testLoadPublicKeyX509DerFromPath() throws Exception {
        KeyPair kp = newKeyPair();
        Path tmp = Files.createTempFile("rsa-pub", ".der");
        Files.write(tmp, kp.getPublic().getEncoded());
        PublicKey key = RsaKeys.loadPublicKey(tmp);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    @Test
    public void testParsePrivateKeyPemInline() throws Exception {
        KeyPair kp = newKeyPair();
        PrivateKey key = RsaKeys.parsePrivateKeyPem(toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testParsePublicKeyBase64Inline() throws Exception {
        KeyPair kp = newKeyPair();
        String base64 = Base64.toString(kp.getPublic().getEncoded());
        PublicKey key = RsaKeys.parsePublicKeyBase64(base64);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    /** 把 DER 字节包装成带换行的 PEM 文本。 */
    private static String toPem(String type, byte[] der) {
        String base64 = Base64.toString(der);
        StringBuilder sb = new StringBuilder("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        return sb.append("-----END ").append(type).append("-----\n").toString();
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaKeysTest -q`
Expected: 编译失败 —— `loadPrivateKey` 等公开方法未定义。

- [ ] **Step 3: 写实现**

在 `RsaKeys.java` 顶部 `import` 区追加：

```java
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
```

在类内（`buildPublicKey` 之后）追加：

```java
    /**
     * 从文件加载 RSA 私钥（PKCS#8）。自动识别 PEM/DER。
     *
     * @param path 私钥文件路径
     * @return RSA 私钥
     * @throws IOException             读取文件失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey loadPrivateKey(Path path) throws IOException, GeneralSecurityException {
        return buildPrivateKey(extractPemContent(Files.readAllBytes(path)));
    }

    /**
     * 从输入流加载 RSA 私钥（PKCS#8）。自动识别 PEM/DER。
     * <p>
     * <b>注意：本方法不会关闭传入的流，调用方需自行 close。</b>
     *
     * @param in 私钥输入流（由调用方负责关闭）
     * @return RSA 私钥
     * @throws IOException             读取流失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey loadPrivateKey(InputStream in) throws IOException, GeneralSecurityException {
        return buildPrivateKey(extractPemContent(readAllBytes(in)));
    }

    /**
     * 从文件加载 RSA 公钥（X.509）。自动识别 PEM/DER。
     *
     * @param path 公钥文件路径
     * @return RSA 公钥
     * @throws IOException             读取文件失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey loadPublicKey(Path path) throws IOException, GeneralSecurityException {
        return buildPublicKey(extractPemContent(Files.readAllBytes(path)));
    }

    /**
     * 从输入流加载 RSA 公钥（X.509）。自动识别 PEM/DER。
     * <p>
     * <b>注意：本方法不会关闭传入的流，调用方需自行 close。</b>
     *
     * @param in 公钥输入流（由调用方负责关闭）
     * @return RSA 公钥
     * @throws IOException             读取流失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey loadPublicKey(InputStream in) throws IOException, GeneralSecurityException {
        return buildPublicKey(extractPemContent(readAllBytes(in)));
    }

    /**
     * 解析内存中的 RSA 私钥 PEM 字符串（含 {@code -----BEGIN-----} 头）。
     *
     * @param pem PEM 文本
     * @return RSA 私钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey parsePrivateKeyPem(String pem) throws GeneralSecurityException {
        return buildPrivateKey(extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * 解析内存中的 RSA 公钥 PEM 字符串（含 {@code -----BEGIN-----} 头）。
     *
     * @param pem PEM 文本
     * @return RSA 公钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey parsePublicKeyPem(String pem) throws GeneralSecurityException {
        return buildPublicKey(extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * 解析裸 Base64 编码的 RSA 私钥（DER 的 Base64，无 PEM 头）。兼容旧 API 迁移。
     *
     * @param base64 裸 Base64 私钥
     * @return RSA 私钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey parsePrivateKeyBase64(String base64) throws GeneralSecurityException {
        return buildPrivateKey(Base64.toByte(base64));
    }

    /**
     * 解析裸 Base64 编码的 RSA 公钥（DER 的 Base64，无 PEM 头）。兼容旧 API 迁移。
     *
     * @param base64 裸 Base64 公钥
     * @return RSA 公钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey parsePublicKeyBase64(String base64) throws GeneralSecurityException {
        return buildPublicKey(Base64.toByte(base64));
    }

    /** 把输入流排空为字节数组（不关闭流）。 */
    private static byte[] readAllBytes(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaKeysTest -q`
Expected: PASS（10 个测试）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java
git commit -m "feat(crypto): add RsaKeys load/parse entries (Path/InputStream/PEM/Base64)"
```

---

## Task 4: `RsaKeys` —— 密钥生成（强制 ≥2048）

**Files:**
- Modify: `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java`

加入 `generateKeyPair()`/`(int)` 与 `generateBase64KeyPair()`/`(int)`，强制 `keySize >= 2048`，返回 `RsaKeyPair` record。

- [ ] **Step 1: 写失败测试**

在 `RsaKeysTest` 加 `import`：

```java
import org.junit.jupiter.api.Assertions;
```
（若已存在则跳过）

新增测试：

```java
    @Test
    public void testGenerateKeyPairDefaultSize() throws Exception {
        java.security.KeyPair kp = RsaKeys.generateKeyPair();
        int bits = ((java.security.interfaces.RSAKey) kp.getPublic()).getModulus().bitLength();
        Assertions.assertEquals(2048, bits);
    }

    @Test
    public void testGenerateKeyPairRejectsWeakSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.generateKeyPair(1024));
    }

    @Test
    public void testGenerateBase64KeyPairRoundTrip() throws Exception {
        RsaKeys.RsaKeyPair pair = RsaKeys.generateBase64KeyPair(2048);
        Assertions.assertNotNull(pair.privateKeyBase64());
        Assertions.assertNotNull(pair.publicKeyBase64());
        // 生成的 Base64 可被 parse*Base64 还原
        Assertions.assertNotNull(RsaKeys.parsePrivateKeyBase64(pair.privateKeyBase64()));
        Assertions.assertNotNull(RsaKeys.parsePublicKeyBase64(pair.publicKeyBase64()));
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaKeysTest -q`
Expected: 编译失败 —— `generateKeyPair` 等未定义。

- [ ] **Step 3: 写实现**

在 `RsaKeys.java` 顶部 `import` 区追加：

```java
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
```

在类内追加（`parsePublicKeyBase64` 之后、`readAllBytes` 之前）：

```java
    /** 默认密钥长度（位）。 */
    public static final int DEFAULT_KEY_SIZE = 2048;

    /** 安全密钥长度下限（位）。低于此值视为弱密钥并拒绝。 */
    public static final int MIN_KEY_SIZE = 2048;

    /**
     * 生成默认长度（2048 位）的 RSA 密钥对。
     *
     * @return RSA 密钥对
     * @throws NoSuchAlgorithmException 不支持 RSA 算法
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPair(DEFAULT_KEY_SIZE);
    }

    /**
     * 生成指定长度的 RSA 密钥对。
     *
     * @param keySize 密钥长度（位），必须 {@code >= 2048}
     * @return RSA 密钥对
     * @throws IllegalArgumentException keySize 小于 2048
     * @throws NoSuchAlgorithmException 不支持 RSA 算法
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        if (keySize < MIN_KEY_SIZE) {
            throw new IllegalArgumentException("keySize must be >= " + MIN_KEY_SIZE + ", but was " + keySize);
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }

    /**
     * 生成默认长度（2048 位）的 RSA 密钥对，返回 Base64 编码的公私钥。
     *
     * @return Base64 密钥对
     * @throws NoSuchAlgorithmException 不支持 RSA 算法
     */
    public static RsaKeyPair generateBase64KeyPair() throws NoSuchAlgorithmException {
        return generateBase64KeyPair(DEFAULT_KEY_SIZE);
    }

    /**
     * 生成指定长度的 RSA 密钥对，返回 Base64 编码的公私钥。
     *
     * @param keySize 密钥长度（位），必须 {@code >= 2048}
     * @return Base64 密钥对
     * @throws IllegalArgumentException keySize 小于 2048
     * @throws NoSuchAlgorithmException 不支持 RSA 算法
     */
    public static RsaKeyPair generateBase64KeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair(keySize);
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey pri = (RSAPrivateKey) keyPair.getPrivate();
        return new RsaKeyPair(Base64.toString(pri.getEncoded()), Base64.toString(pub.getEncoded()));
    }

    /**
     * Base64 编码的 RSA 公私钥对。
     *
     * @param privateKeyBase64 PKCS#8 私钥的 Base64
     * @param publicKeyBase64  X.509 公钥的 Base64
     */
    public record RsaKeyPair(String privateKeyBase64, String publicKeyBase64) {
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaKeysTest -q`
Expected: PASS（13 个测试）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java
git commit -m "feat(crypto): add RsaKeys key generation with >=2048 enforcement"
```

---

## Task 5: `RsaUtils` 重写 —— OAEP 加解密核心 + 限长

**Files:**
- Modify: `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaUtils.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaUtilsTest.java`

完全重写 `RsaUtils`：`final` + 私有构造；OAEP（SHA-256 + MGF1-SHA256，显式 `OAEPParameterSpec`）；核心 `encrypt(byte[], PublicKey)` / `decrypt(byte[], PrivateKey)` 返回 `byte[]`；超长明文限长；null 校验 + SLF4J；精确异常。

- [ ] **Step 1: 写失败测试 —— round-trip 与限长**

先把 `RsaUtilsTest.java` 完全替换为：

```java
package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RsaUtilsTest {

    private static java.security.KeyPair newKeyPair() throws Exception {
        java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    @Test
    public void testEncryptDecryptRoundTripBytes() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        byte[] data = "hello,rsa".getBytes(StandardCharsets.UTF_8);
        byte[] cipher = RsaUtils.encrypt(data, kp.getPublic());
        byte[] plain = RsaUtils.decrypt(cipher, kp.getPrivate());
        Assertions.assertArrayEquals(data, plain);
    }

    @Test
    public void testEncryptToBase64AndDecryptFromStringRoundTrip() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        String ct = RsaUtils.encryptToBase64("hello,rsa", kp.getPublic());
        byte[] plain = RsaUtils.decryptFromBase64(ct, kp.getPrivate());
        Assertions.assertArrayEquals("hello,rsa".getBytes(StandardCharsets.UTF_8), plain);
    }

    @Test
    public void testEncryptToBase64AndDecryptFromBase64ToString() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        String ct = RsaUtils.encryptToBase64("hello,rsa", kp.getPublic());
        String pt = RsaUtils.decryptFromBase64ToString(ct, kp.getPrivate());
        Assertions.assertEquals("hello,rsa", pt);
    }

    @Test
    public void testDecryptToString() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        byte[] data = "hello,rsa".getBytes(StandardCharsets.UTF_8);
        byte[] cipher = RsaUtils.encrypt(data, kp.getPublic());
        Assertions.assertEquals("hello,rsa", RsaUtils.decryptToString(cipher, kp.getPrivate()));
    }

    @Test
    public void testOversizedPlaintextRejected() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        byte[] tooBig = new byte[256]; // 2048/OAEP-SHA256 上限为 190 字节
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaUtils.encrypt(tooBig, kp.getPublic()));
    }

    @Test
    public void testNullArgsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaUtils.encrypt(null, null));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaUtilsTest -q`
Expected: 编译失败 —— `RsaUtils` 仍是旧签名（无 `encrypt(byte[], PublicKey)`、无 `encryptToBase64(String,...)` 等）。

- [ ] **Step 3: 写实现 —— 完全重写 `RsaUtils.java`**

把 `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaUtils.java` 完全替换为：

```java
package io.ituknown.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;

/**
 * RSA 非对称加解密工具类（公钥加密 / 私钥解密）。
 * <p>
 * 使用 {@code RSA/ECB/OAEPWithSHA-256AndMGF1Padding}，并显式指定 hash 与 MGF1 均为 SHA-256
 * （该转换字符串默认 MGF1 用 SHA-1，显式指定以避免互操作意外）。私钥签名/验签请使用 {@link HashWithRsa}。
 * <p>
 * <b>长度限制</b>：OAEP-SHA256 下单块明文上限为 {@code keySizeBytes - 66}（2048 位密钥为 190 字节）。
 * 超长明文会抛 {@link IllegalArgumentException}。RSA 不适合加密大数据，请改用混合加密（RSA 包 AES 密钥）。
 * 密钥加载请使用 {@link RsaKeys}。
 */
public final class RsaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RsaUtils.class);

    /** 转换名（仅用于日志，实际 init 使用 {@link #OAEP_SPEC}）。 */
    public static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /** OAEP 参数：hash=SHA-256，MGF1=SHA-256，PSource 默认。 */
    static final OAEPParameterSpec OAEP_SPEC = new OAEPParameterSpec(
            "SHA-256", "MGF1", java.security.spec.MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    /** OAEP with SHA-256 的固定开销字节数（2*hashLen + 2 = 66）。 */
    static final int OAEP_OVERHEAD_BYTES = 66;

    private RsaUtils() {
    }

    /**
     * 公钥加密。明文为任意字节，返回密文字节。
     *
     * @param plaintext 明文
     * @param pubKey    公钥
     * @return 密文
     * @throws IllegalArgumentException 明文为 null、超长
     * @throws NoSuchAlgorithmException 不支持 RSA/OAEP
     * @throws NoSuchPaddingException   不支持 OAEP 填充
     * @throws InvalidKeyException      公钥非法
     * @throws IllegalBlockSizeException 加密块大小非法
     */
    public static byte[] encrypt(byte[] plaintext, PublicKey pubKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException {
        requireNonNull(plaintext, "plaintext");
        requireNonNull(pubKey, "pubKey");
        int maxInput = maxPlaintextBytes(pubKey);
        if (plaintext.length > maxInput) {
            throw new IllegalArgumentException(
                    "Plaintext too large: " + plaintext.length + " bytes, max " + maxInput
                            + " for this key. Use hybrid encryption (RSA + AES) for large data.");
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, pubKey, OAEP_SPEC);
            return cipher.doFinal(plaintext);
        } catch (InvalidAlgorithmParameterException | BadPaddingException e) {
            // OAEP_SPEC 是固定常量，二者均不应发生，属编程错误而非调用方错误
            throw new IllegalStateException("Failed to init/finish RSA OAEP encryption", e);
        }
    }

    /**
     * 公钥加密便捷方法：UTF-8 明文 → Base64 密文。
     *
     * @param plaintext 明文
     * @param pubKey    公钥
     * @return Base64 密文
     */
    public static String encryptToBase64(String plaintext, PublicKey pubKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException {
        return Base64.toString(encrypt(plaintext.getBytes(StandardCharsets.UTF_8), pubKey));
    }

    /**
     * 私钥解密。密文为字节，返回明文字节。
     *
     * @param ciphertext 密文
     * @param priKey     私钥
     * @return 明文
     * @throws IllegalArgumentException 密文为 null
     * @throws NoSuchAlgorithmException 不支持 RSA/OAEP
     * @throws NoSuchPaddingException   不支持 OAEP 填充
     * @throws InvalidKeyException      私钥非法
     * @throws IllegalBlockSizeException 密文块大小非法
     * @throws BadPaddingException      密钥不匹配或密文损坏
     */
    public static byte[] decrypt(byte[] ciphertext, PrivateKey priKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        requireNonNull(ciphertext, "ciphertext");
        requireNonNull(priKey, "priKey");
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        try {
            cipher.init(Cipher.DECRYPT_MODE, priKey, OAEP_SPEC);
            return cipher.doFinal(ciphertext);
        } catch (InvalidAlgorithmParameterException e) {
            // OAEP_SPEC 是固定常量，不应发生，属编程错误
            throw new IllegalStateException("Failed to init RSA OAEP cipher", e);
        } catch (BadPaddingException e) {
            // 不在异常消息里泄露任何细节
            LOGGER.error("RSA decryption failed: bad padding (wrong key or tampered ciphertext)");
            throw e;
        }
    }

    /**
     * 私钥解密便捷方法：Base64 密文 → 明文字节。
     *
     * @param base64Ciphertext Base64 密文
     * @param priKey           私钥
     * @return 明文
     */
    public static byte[] decryptFromBase64(String base64Ciphertext, PrivateKey priKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        return decrypt(Base64.toByte(base64Ciphertext), priKey);
    }

    /**
     * 私钥解密便捷方法：密文字节 → UTF-8 明文字符串。
     *
     * @param ciphertext 密文
     * @param priKey     私钥
     * @return UTF-8 明文
     */
    public static String decryptToString(byte[] ciphertext, PrivateKey priKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        return new String(decrypt(ciphertext, priKey), StandardCharsets.UTF_8);
    }

    /**
     * 私钥解密便捷方法：Base64 密文 → UTF-8 明文字符串（最常用）。
     *
     * @param base64Ciphertext Base64 密文
     * @param priKey           私钥
     * @return UTF-8 明文
     */
    public static String decryptFromBase64ToString(String base64Ciphertext, PrivateKey priKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        return decryptToString(Base64.toByte(base64Ciphertext), priKey);
    }

    /** 计算给定密钥在 OAEP-SHA256 下的单块明文上限：keySizeBytes - 66。 */
    static int maxPlaintextBytes(PublicKey pubKey) {
        int keyBits = ((RSAKey) pubKey).getModulus().bitLength();
        return keyBits / 8 - OAEP_OVERHEAD_BYTES;
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaUtilsTest -q`
Expected: PASS（6 个测试）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/RsaUtils.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/RsaUtilsTest.java
git commit -m "feat(crypto): rewrite RsaUtils with OAEP, length-limit, precise exceptions"
```

---

## Task 6: `RsaUtils` —— 错误路径测试（密钥不匹配 → BadPadding）

**Files:**
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaUtilsTest.java`

补一个「密钥不匹配解密失败」的测试，确保 OAEP 在错误密钥下抛 `BadPaddingException`（而非静默错误）。

- [ ] **Step 1: 写失败测试**

在 `RsaUtilsTest` 新增：

```java
    @Test
    public void testDecryptWithWrongKeyThrowsBadPadding() throws Exception {
        java.security.KeyPair encryptKp = newKeyPair();
        java.security.KeyPair decryptKp = newKeyPair();
        byte[] cipher = RsaUtils.encrypt("hello,rsa".getBytes(StandardCharsets.UTF_8), encryptKp.getPublic());
        Assertions.assertThrows(javax.crypto.BadPaddingException.class,
                () -> RsaUtils.decrypt(cipher, decryptKp.getPrivate()));
    }
```

- [ ] **Step 2: 运行测试确认通过（本任务无需改实现）**

Run: `mvn -pl cookbook-crypto -am test -Dtest=RsaUtilsTest -q`
Expected: PASS（7 个测试）。
> 说明：实现已在 Task 5 完成，此测试用于固化错误路径契约。若失败，说明 `decrypt` 未正确抛出 `BadPaddingException`，需回查 Task 5 实现。

- [ ] **Step 3: 提交**

```bash
git add cookbook-crypto/src/test/java/io/ituknown/crypto/RsaUtilsTest.java
git commit -m "test(crypto): assert wrong-key RSA decryption raises BadPaddingException"
```

---

## Task 7: 全量回归与清理

**Files:**
- 全模块 `mvn test`

确认整个 `cookbook-crypto`（含依赖 `Production` 的 `AesUtils`/`HashWithRsa`）编译通过、测试全绿，无遗留对旧 `RsaUtils` API 的引用。

- [ ] **Step 1: 全模块测试**

Run: `mvn -pl cookbook-crypto -am test -q`
Expected: BUILD SUCCESS，所有测试通过（`RsaKeysTest` 13 个 + `RsaUtilsTest` 7 个 + 其余 `AesUtilsTest`/`HashWithRsaTest` 等）。

- [ ] **Step 2: 确认无残留旧 API 引用**

Run: `grep -rn "Production" cookbook-crypto/src/main/java/io/ituknown/crypto/RsaUtils.java`
Expected: 无匹配（`RsaUtils` 已不再使用 `Production`）。

Run: `grep -rn "encryptToBase64String\|decryptBase64\|generateBase64KeyPair\|RsaUtils.Pair" cookbook-crypto/src`
Expected: 无匹配（旧 API 已移除）。

- [ ] **Step 3: 提交（若有 lint/格式调整）**

Run: `git status`
若 `git status` 干净则无需提交；若有格式微调：

```bash
git add -A
git commit -m "chore(crypto): cleanup after RsaUtils refactor"
```

---

## Self-Review 记录

- **Spec 覆盖**：`RsaKeys` 加载/解析/生成（Task 1-4）、`RsaUtils` OAEP 加解密+限长（Task 5）、错误处理与 BadPadding（Task 6）、round-trip（Task 5）、keySize≥2048（Task 4）、`InputStream` 不 close 由调用方负责（Task 3 Javadoc）、PKCS#1 out-of-scope（Task 1 Javadoc）—— spec 各点均有对应任务。
- **占位符扫描**：无 TBD/TODO，所有代码步骤含完整代码。
- **类型一致性**：`RsaKeyPair(privateKeyBase64, publicKeyBase64)`、`maxPlaintextBytes`、`OAEP_SPEC`、`buildPrivateKey/buildPublicKey`、`extractPemContent/isPem`、`parsePublicKeyBase64` 等命名在任务间一致。
- **注意点**：所有密钥相关测试均动态生成密钥对（`KeyPairGenerator`），不依赖任何硬编码密钥常量，避免密钥失效导致测试腐烂。
