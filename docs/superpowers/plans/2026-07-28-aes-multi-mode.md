# AES 多模式类型状态重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把写死的 `AES/GCM/NoPadding` 重构为类型状态 fluent API（`Aes.gcm()/.cbc()/.ctr()/.ccm()/.ocb()/.cfb()/.ofb()`），分层支持 SunJCE 原生模式与 BouncyCastle 专属模式/填充，并保留 `AesUtils` 为 GCM 默认门面。

**Architecture:** 新增 `io.ituknown.crypto.aes` 子包：`AesMode`/`Padding` 提供模式与填充元数据，包级 `AesEngine` 统一拼转换串/选参数对象/调用 `Cipher` 并产出「IV + 密文(+AEAD 标签)」组合输出，包级 `BouncyCastleSupport` 幂等懒注册 BC provider，`AesBuilder` 基类 + 三个家族子类 + `Aes` 入口提供编译期安全的 fluent API，`AesUtils` 迁入子包瘦身为门面委托引擎。

**Tech Stack:** Java 21、`javax.crypto`（JCE）、BouncyCastle `bcprov-jdk18on 1.78.1`、slf4j-api、JUnit 5、Maven。

## Global Constraints

- Java 21（`maven.compiler.release=21`）；构建用 `mvn`（无 wrapper），从仓库根运行：`mvn -q -pl cookbook-crypto test`。
- AES 专用类型全部位于子包 `io.ituknown.crypto.aes`；`Hex`/`Base64`/`Require` 留在根包 `io.ituknown.crypto`（子包单向依赖）。
- **不含 ECB**；不含统一 `.mode(...)` 运行期选模式入口；不含「外部密文无前置 IV」的对接解密。
- **异常策略：透传 JCE 受检异常，不包装。** 每个加解密方法声明同一组受检异常：
  `throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException`
  （`NoSuchProviderException` 在引擎内捕获转为 `IllegalStateException`，因 BC 为硬依赖。）
- AEAD 家族（GCM/CCM/OCB）统一用 `GCMParameterSpec(tagBits, iv)` 传参（SunJCE 与 BC 的 JCE AEAD 包装均识别）；分组/流式用 `IvParameterSpec(iv)`。
- 输出格式：`IV(mode.ivLength) + 密文`（AEAD 标签由 `Cipher` 自动拼入密文末尾）；解密从头部按 `mode.ivLength` 切分。
- 默认 IV：`SecureRandom` 生成 `mode.ivLength` 字节随机值；`.iv(byte[])` 显式入口覆盖。
- 注释只描述业务逻辑，不含 Java 标识符/SQL/字面量（遵循全局注释规范）；Markdown 禁用 `---` 横线分隔符。
- Commit 消息 scope 用 `crypto`（如 `feat(crypto): ...`）。
- **测试约定（模块 surefire 限制）**：父 pom 声明了 JUnit BOM 5.12.2 但**未锁定 `maven-surefire-plugin` 版本**，Maven 解析到旧版 provider，按「`public class` + `public void testXxx()` 命名」发现测试（`@Test` 注解不驱动发现；现有 12 个测试皆循此惯例）。故本计划所有测试类须为 `public class`，方法须为 `@Test public void testXxx()`（方法名加 `test` 前缀）。该写法在旧/新 surefire 下均可发现，向后兼容。升级 surefire 属独立议题，不在本次范围。

## File Structure

| 文件 | 责任 | 任务 |
|---|---|---|
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesMode.java`（包级枚举） | 模式元数据：转换名/家族/IV 长度/是否需 BC | T1 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/Padding.java`（public 枚举） | 填充元数据：转换名/是否需 BC | T1 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/Require.java`（改造） | 包级私有 → public；`requireNonNull` 泛型返回受检值 | T2 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesEngine.java`（包级） | 共用加解密引擎；原生路径 T3，BC 路径 T4 | T3/T4 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/BouncyCastleSupport.java`（包级） | 幂等懒注册 BC provider | T4 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesBuilder.java`（包级基类） | 持有状态 + 终结方法 + 自类型 fluent | T5 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AeadAesBuilder.java` | AEAD 家族（`.tagBits`） | T5 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/BlockAesBuilder.java` | 分组家族（`.padding`） | T5 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/StreamAesBuilder.java` | 流式家族 | T5 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/Aes.java`（public 入口） | 家族工厂方法 | T5 |
| `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesUtils.java`（迁入+改造） | GCM 默认门面，委托引擎 | T6 |
| `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java`（新增） | 引擎与 fluent API 测试 | T3/T4/T5 |
| `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesUtilsTest.java`（迁入） | 门面回归（断言不变，仅改包/导入） | T6 |

## Task 1: `AesMode` 与 `Padding` 枚举

**Files:**
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesMode.java`
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/Padding.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java`（本任务新建，仅放枚举元数据测试）

**Interfaces:**
- Consumes: 无
- Produces: `enum AesMode { GCM, CCM, OCB, CBC, CTR, CFB, OFB }`，字段 `String transformation`、`Family family`（`enum Family { AEAD, BLOCK, STREAM }`）、`int ivLength`、`boolean requiresBc`；`enum Padding { NONE, PKCS5, PKCS7, ISO7816, ANSI_X9_23, ISO10126, ZERO }`，字段 `String transformation`、`boolean requiresBc`。

- [ ] **Step 1: 写失败测试（新建 `AesTest`）**

```java
package io.ituknown.crypto.aes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesTest {

    @Test
    void aesModeMetadata() {
        assertEquals("GCM", AesMode.GCM.transformation);
        assertEquals(AesMode.Family.AEAD, AesMode.GCM.family);
        assertEquals(12, AesMode.GCM.ivLength);
        assertFalse(AesMode.GCM.requiresBc);

        assertEquals("CBC", AesMode.CBC.transformation);
        assertEquals(AesMode.Family.BLOCK, AesMode.CBC.family);
        assertEquals(16, AesMode.CBC.ivLength);
        assertFalse(AesMode.CBC.requiresBc);

        assertEquals("CTR", AesMode.CTR.transformation);
        assertEquals(AesMode.Family.STREAM, AesMode.CTR.family);
        assertEquals(16, AesMode.CTR.ivLength);

        assertTrue(AesMode.CCM.requiresBc);
        assertTrue(AesMode.OCB.requiresBc);
        assertEquals(12, AesMode.OCB.ivLength);
        assertEquals(16, AesMode.CFB.ivLength);
        assertEquals(16, AesMode.OFB.ivLength);
    }

    @Test
    void paddingMetadata() {
        assertEquals("NoPadding", Padding.NONE.transformation);
        assertFalse(Padding.NONE.requiresBc);
        assertEquals("PKCS5Padding", Padding.PKCS5.transformation);
        assertFalse(Padding.PKCS5.requiresBc);
        assertTrue(Padding.PKCS7.requiresBc);
        assertEquals("ISO7816-4Padding", Padding.ISO7816.transformation);
        assertEquals("X9.23Padding", Padding.ANSI_X9_23.transformation);
        assertEquals("ISO10126-2Padding", Padding.ISO10126.transformation);
        assertEquals("ZeroBytePadding", Padding.ZERO.transformation);
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: 编译失败——`AesMode` / `Padding` 不存在。

- [ ] **Step 3: 实现 `AesMode`**

```java
package io.ituknown.crypto.aes;

/**
 * AES 工作模式元数据：转换串片段、所属家族、推荐初始化向量长度、是否必须依赖 BouncyCastle。
 */
enum AesMode {

    GCM("GCM", Family.AEAD, 12, false),
    CCM("CCM", Family.AEAD, 12, true),
    OCB("OCB", Family.AEAD, 12, true),
    CBC("CBC", Family.BLOCK, 16, false),
    CTR("CTR", Family.STREAM, 16, false),
    CFB("CFB", Family.STREAM, 16, false),
    OFB("OFB", Family.STREAM, 16, false);

    enum Family { AEAD, BLOCK, STREAM }

    final String transformation;
    final Family family;
    final int ivLength;
    final boolean requiresBc;

    AesMode(String transformation, Family family, int ivLength, boolean requiresBc) {
        this.transformation = transformation;
        this.family = family;
        this.ivLength = ivLength;
        this.requiresBc = requiresBc;
    }
}
```

- [ ] **Step 4: 实现 `Padding`**

```java
package io.ituknown.crypto.aes;

/**
 * AES 填充模式：转换串片段与是否必须依赖 BouncyCastle。
 * 对 AES（16 字节分组）而言，PKCS5 与 PKCS7 完全等价。
 */
public enum Padding {

    NONE("NoPadding", false),
    PKCS5("PKCS5Padding", false),
    PKCS7("PKCS7Padding", true),
    ISO7816("ISO7816-4Padding", true),
    ANSI_X9_23("X9.23Padding", true),
    ISO10126("ISO10126-2Padding", true),
    ZERO("ZeroBytePadding", true);

    final String transformation;
    final boolean requiresBc;

    Padding(String transformation, boolean requiresBc) {
        this.transformation = transformation;
        this.requiresBc = requiresBc;
    }
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: PASS（2 个测试）。

- [ ] **Step 6: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesMode.java \
        cookbook-crypto/src/main/java/io/ituknown/crypto/aes/Padding.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java
git commit -m "feat(crypto): 新增 AES 模式与填充枚举"
```

## Task 2: `Require` 提升为 public 并泛型返回

**Files:**
- Modify: `cookbook-crypto/src/main/java/io/ituknown/crypto/Require.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/RequireTest.java`（新建）

**Interfaces:**
- Consumes: 无
- Produces: `public static <T> T requireNonNull(T value, String name)`——返回受检值本身（便于 fluent 赋值）；`public final class Require`。对既有「语句式」调用方（`Hex`/`Base64`）向后兼容（忽略返回值即可）。

- [ ] **Step 1: 写失败测试（新建 `RequireTest`）**

```java
package io.ituknown.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequireTest {

    @Test
    void requireNonNullReturnsValueAndRejectsNull() {
        assertEquals("x", Require.requireNonNull("x", "name"));
        assertThrows(IllegalArgumentException.class,
                () -> Require.requireNonNull(null, "name"));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -q -pl cookbook-crypto test -Dtest=RequireTest`
Expected: 编译失败——当前 `requireNonNull` 返回 `void`，`assertEquals("x", requireNonNull(...))` 类型不匹配。

- [ ] **Step 3: 改造 `Require`**

```java
package io.ituknown.crypto;

/**
 * 参数校验工具：非空校验并原样返回受检值，便于链式赋值。
 */
public final class Require {

    private Require() {
    }

    /**
     * 校验 value 非 null，否则抛 {@link IllegalArgumentException}，并原样返回 value。
     *
     * @param value 待校验值
     * @param name  参数名（写入异常消息，便于定位）
     * @return 受检值本身
     */
    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn -q -pl cookbook-crypto test -Dtest=RequireTest`
Expected: PASS。

- [ ] **Step 5: 跑全模块确认未破坏既有调用方**

Run: `mvn -q -pl cookbook-crypto test`
Expected: PASS（`Hex`/`Base64` 等语句式调用不受影响）。

- [ ] **Step 6: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/Require.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/RequireTest.java
git commit -m "refactor(crypto): Require 提升为 public 并返回受检值"
```

## Task 3: `AesEngine` 原生模式加解密

**Files:**
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesEngine.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java`（追加引擎测试与 key 辅助方法）

**Interfaces:**
- Consumes: T1 的 `AesMode`/`Padding`；根包 `Require`。
- Produces: `static byte[] generateIv(AesMode)`；`static byte[] encrypt(AesMode, Padding, SecretKey key, byte[] iv, int tagBits, byte[] plaintext)`；`static byte[] decrypt(AesMode, Padding, SecretKey key, int tagBits, byte[] combined)`——均声明全局约束中的 6 个受检异常。本任务的 `createCipher` 只走原生路径（不引用 `requiresBc`）。

- [ ] **Step 1: 写失败测试（在 `AesTest` 追加辅助方法与引擎测试）**

在 `AesTest` 顶部加导入与 key 辅助方法：

```java
import javax.crypto.AEADBadTagException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

// 类内加：
private static SecretKey newKey(int bits) throws NoSuchAlgorithmException {
    KeyGenerator kg = KeyGenerator.getInstance("AES");
    kg.init(bits);
    return kg.generateKey();
}
```

追加测试方法：

```java
@Test
void engineGcmRoundTrip() throws Exception {
    SecretKey key = newKey(256);
    byte[] plain = "hello, world".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] iv = AesEngine.generateIv(AesMode.GCM);
    byte[] combined = AesEngine.encrypt(AesMode.GCM, Padding.NONE, key, iv, 128, plain);
    // IV 前置：组合长度 = 12(IV) + 明文 + 16(标签)
    assertEquals(12 + plain.length + 16, combined.length);
    byte[] decrypted = AesEngine.decrypt(AesMode.GCM, Padding.NONE, key, 128, combined);
    assertArrayEquals(plain, decrypted);
}

@Test
void engineGcmTamperThrows() throws Exception {
    SecretKey key = newKey(128);
    byte[] plain = "secret".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] combined = AesEngine.encrypt(AesMode.GCM, Padding.NONE, key,
            AesEngine.generateIv(AesMode.GCM), 128, plain);
    combined[combined.length - 1] ^= 0x01; // 篡改认证标签
    assertThrows(AEADBadTagException.class,
            () -> AesEngine.decrypt(AesMode.GCM, Padding.NONE, key, 128, combined));
}

@Test
void engineCbcPkcs5RoundTrip() throws Exception {
    SecretKey key = newKey(128);
    byte[] plain = "cbc payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] iv = AesEngine.generateIv(AesMode.CBC);
    byte[] combined = AesEngine.encrypt(AesMode.CBC, Padding.PKCS5, key, iv, 128, plain);
    assertArrayEquals(plain, AesEngine.decrypt(AesMode.CBC, Padding.PKCS5, key, 128, combined));
}

@Test
void engineStreamModesRoundTrip() throws Exception {
    SecretKey key = newKey(128);
    byte[] plain = "stream payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    for (AesMode mode : new AesMode[]{AesMode.CTR, AesMode.CFB, AesMode.OFB}) {
        byte[] iv = AesEngine.generateIv(mode);
        byte[] combined = AesEngine.encrypt(mode, Padding.NONE, key, iv, 128, plain);
        assertArrayEquals(plain, AesEngine.decrypt(mode, Padding.NONE, key, 128, combined),
                "round-trip failed for " + mode);
    }
}

@Test
void engineCbcNoPaddingNonAlignedThrows() throws Exception {
    SecretKey key = newKey(128);
    byte[] plain = "not-aligned".getBytes(java.nio.charset.StandardCharsets.UTF_8); // 11 字节
    byte[] iv = AesEngine.generateIv(AesMode.CBC);
    assertThrows(IllegalBlockSizeException.class,
            () -> AesEngine.encrypt(AesMode.CBC, Padding.NONE, key, iv, 128, plain));
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: 编译失败——`AesEngine` 不存在。

- [ ] **Step 3: 实现 `AesEngine`（原生路径）**

```java
package io.ituknown.crypto.aes;

import io.ituknown.crypto.Require;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * AES 共用加解密引擎：拼转换串、按家族选参数对象、产出「IV + 密文(+AEAD 标签)」组合输出。
 */
final class AesEngine {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesEngine() {
    }

    static byte[] generateIv(AesMode mode) {
        byte[] iv = new byte[mode.ivLength];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    static byte[] encrypt(AesMode mode, Padding padding, SecretKey key, byte[] iv, int tagBits, byte[] plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Require.requireNonNull(key, "key");
        Require.requireNonNull(iv, "iv");
        Require.requireNonNull(plaintext, "plaintext");
        Cipher cipher = createCipher(mode, padding);
        cipher.init(Cipher.ENCRYPT_MODE, key, paramsOf(mode, iv, tagBits));
        byte[] ciphertext = cipher.doFinal(plaintext);
        return concat(iv, ciphertext);
    }

    static byte[] decrypt(AesMode mode, Padding padding, SecretKey key, int tagBits, byte[] combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Require.requireNonNull(key, "key");
        Require.requireNonNull(combined, "combined");
        if (combined.length < mode.ivLength) {
            throw new IllegalArgumentException("Invalid ciphertext: shorter than IV length");
        }
        byte[] iv = Arrays.copyOfRange(combined, 0, mode.ivLength);
        byte[] ciphertext = Arrays.copyOfRange(combined, mode.ivLength, combined.length);
        Cipher cipher = createCipher(mode, padding);
        cipher.init(Cipher.DECRYPT_MODE, key, paramsOf(mode, iv, tagBits));
        return cipher.doFinal(ciphertext);
    }

    private static Cipher createCipher(AesMode mode, Padding padding)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        String transformation = "AES/" + mode.transformation + "/" + padding.transformation;
        return Cipher.getInstance(transformation);
    }

    private static AlgorithmParameterSpec paramsOf(AesMode mode, byte[] iv, int tagBits) {
        if (mode.family == AesMode.Family.AEAD) {
            return new GCMParameterSpec(tagBits, iv);
        }
        return new IvParameterSpec(iv);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: PASS（含 5 个引擎测试 + T1 的 2 个）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesEngine.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java
git commit -m "feat(crypto): 新增 AesEngine 原生模式加解密"
```

## Task 4: BouncyCastle 接入（CCM/OCB + BC 填充）

**Files:**
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/BouncyCastleSupport.java`
- Modify: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesEngine.java`（`createCipher` 增加 BC 路由分支）
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java`（追加 BC 测试）

**Interfaces:**
- Consumes: T1 元数据；T3 引擎。
- Produces: `BouncyCastleSupport.ensureRegistered()`（幂等线程安全）；`AesEngine.createCipher` 在 `mode.requiresBc || padding.requiresBc` 时走 `Cipher.getInstance(transformation, "BC")`，首次调用前懒注册。
- 关键假设：CCM/OCB 经 BC 的 JCE 包装用 `GCMParameterSpec(128, iv)` 传参（与 GCM 一致）。若所装 BC 版本对 CCM/OCB 拒绝 `GCMParameterSpec`，本任务 Step 3 的测试会失败——此时改用 BC 底层 `AEADBlockCipher`（`CCMBlockCipher`/`OCBBlockCipher`）配 `AEADParameters` 重写 AEAD 分支。

- [ ] **Step 1: 写失败测试（在 `AesTest` 追加）**

```java
import java.security.Security;

// 类内加：

@Test
void bouncyCastleRegisteredAtMostOnce() {
    BouncyCastleSupport.ensureRegistered();
    assertNotNull(Security.getProvider("BC"));
    int before = Security.getProviders().length;
    BouncyCastleSupport.ensureRegistered();
    BouncyCastleSupport.ensureRegistered();
    assertEquals(before, Security.getProviders().length);
}

@Test
void engineCcmRoundTrip() throws Exception {
    SecretKey key = newKey(128);
    byte[] plain = "ccm payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] iv = AesEngine.generateIv(AesMode.CCM);
    byte[] combined = AesEngine.encrypt(AesMode.CCM, Padding.NONE, key, iv, 128, plain);
    assertArrayEquals(plain, AesEngine.decrypt(AesMode.CCM, Padding.NONE, key, 128, combined));
}

@Test
void engineOcbRoundTrip() throws Exception {
    SecretKey key = newKey(128);
    byte[] plain = "ocb payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] iv = AesEngine.generateIv(AesMode.OCB);
    byte[] combined = AesEngine.encrypt(AesMode.OCB, Padding.NONE, key, iv, 128, plain);
    assertArrayEquals(plain, AesEngine.decrypt(AesMode.OCB, Padding.NONE, key, 128, combined));
}

@Test
void engineCbcBcPaddingsRoundTrip() throws Exception {
    SecretKey key = newKey(128);
    byte[] plain = "padding variants".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    for (Padding p : new Padding[]{Padding.ISO7816, Padding.ANSI_X9_23, Padding.ISO10126, Padding.ZERO, Padding.PKCS7}) {
        byte[] iv = AesEngine.generateIv(AesMode.CBC);
        byte[] combined = AesEngine.encrypt(AesMode.CBC, p, key, iv, 128, plain);
        assertArrayEquals(plain, AesEngine.decrypt(AesMode.CBC, p, key, 128, combined),
                "round-trip failed for padding " + p);
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: 编译失败——`BouncyCastleSupport` 不存在。

- [ ] **Step 3: 实现 `BouncyCastleSupport`**

```java
package io.ituknown.crypto.aes;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * BouncyCastle provider 幂等懒注册：仅当尚未注册时加锁添加，避免重复与并发重复注入。
 */
final class BouncyCastleSupport {

    private BouncyCastleSupport() {
    }

    static void ensureRegistered() {
        if (Security.getProvider("BC") == null) {
            synchronized (BouncyCastleSupport.class) {
                if (Security.getProvider("BC") == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
            }
        }
    }
}
```

- [ ] **Step 4: 改造 `AesEngine.createCipher` 增加 BC 路由**

新增导入 `import java.security.NoSuchProviderException;`，整体替换 `createCipher`：

```java
private static Cipher createCipher(AesMode mode, Padding padding)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
    String transformation = "AES/" + mode.transformation + "/" + padding.transformation;
    if (mode.requiresBc || padding.requiresBc) {
        BouncyCastleSupport.ensureRegistered();
        try {
            return Cipher.getInstance(transformation, "BC");
        } catch (NoSuchProviderException e) {
            // BouncyCastle 为硬依赖，缺失属非法状态而非可恢复受检异常
            throw new IllegalStateException("BouncyCastle provider unavailable", e);
        }
    }
    return Cipher.getInstance(transformation);
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: PASS（含 CCM/OCB/BC 填充往返与懒注册幂等）。若 CCM/OCB 抛参数非法，按本任务「关键假设」改用 BC 底层 AEAD API。

- [ ] **Step 6: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/aes/BouncyCastleSupport.java \
        cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesEngine.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java
git commit -m "feat(crypto): AesEngine 接入 BouncyCastle 支持 CCM/OCB 与 BC 填充"
```

## Task 5: `Aes` 类型状态 fluent API 与家族构建器

**Files:**
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesBuilder.java`
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AeadAesBuilder.java`
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/BlockAesBuilder.java`
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/StreamAesBuilder.java`
- Create: `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/Aes.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java`（追加 fluent 测试）

**Interfaces:**
- Consumes: T1 元数据；T3/T4 引擎；根包 `Hex`/`Base64`/`Require`。
- Produces: `Aes.gcm()/ccm()/ocb()` → `AeadAesBuilder`（含 `.tagBits(int)`）；`Aes.cbc()` → `BlockAesBuilder`（含 `.padding(Padding)`）；`Aes.ctr()/cfb()/ofb()` → `StreamAesBuilder`。三者共享 `.key(SecretKey)`、`.iv(byte[])` 与终结方法。终结方法：`byte[] encrypt(byte[])`、`byte[] encrypt(String)`、`String encryptToBase64(String)`、`String encryptToHex(String)`、`byte[] decrypt(byte[])`、`String decryptToString(byte[])`、`String decryptFromBase64(String)`、`String decryptFromHex(String)`（均声明 6 个受检异常）。
- 类型状态保证：`AeadAesBuilder` 无 `padding()`、`BlockAesBuilder` 无 `tagBits()`、`StreamAesBuilder` 两者皆无——非法组合编译期即不可表达（在 `Aes` 类 Javadoc 中以注释示例说明）。

- [ ] **Step 1: 写失败测试（在 `AesTest` 追加）**

```java
import io.ituknown.crypto.Hex;

// 类内加：

@Test
void gcmFluentRoundTrip() throws Exception {
    SecretKey key = newKey(256);
    String plain = "hello, world";
    byte[] combined = Aes.gcm().tagBits(128).key(key).encrypt(plain);
    assertEquals(plain, Aes.gcm().tagBits(128).key(key).decryptToString(combined));
}

@Test
void cbcFluentRoundTrip() throws Exception {
    SecretKey key = newKey(128);
    String plain = "cbc payload";
    byte[] combined = Aes.cbc().padding(Padding.PKCS5).key(key).encrypt(plain);
    assertEquals(plain, Aes.cbc().padding(Padding.PKCS5).key(key).decryptToString(combined));
}

@Test
void ctrFluentRoundTrip() throws Exception {
    SecretKey key = newKey(128);
    String plain = "ctr stream";
    byte[] combined = Aes.ctr().key(key).encrypt(plain);
    assertEquals(plain, Aes.ctr().key(key).decryptToString(combined));
}

@Test
void base64AndHexConvenienceRoundTrip() throws Exception {
    SecretKey key = newKey(128);
    String plain = "convenience";
    String b64 = Aes.gcm().key(key).encryptToBase64(plain);
    assertEquals(plain, Aes.gcm().key(key).decryptFromBase64(b64));
    String hex = Aes.gcm().key(key).encryptToHex(plain);
    assertEquals(plain, Aes.gcm().key(key).decryptFromHex(hex));
}

@Test
void randomIvProducesDistinctCiphertext() throws Exception {
    SecretKey key = newKey(128);
    String plain = "same plaintext";
    String a = Aes.gcm().key(key).encryptToHex(plain);
    String b = Aes.gcm().key(key).encryptToHex(plain);
    assertNotEquals(a, b);
}

@Test
void explicitIvIsDeterministic() throws Exception {
    SecretKey key = newKey(128);
    String plain = "deterministic";
    byte[] iv = Hex.toByteArray("00112233445566778899AABBCCDDEEFF");
    String a = Aes.cbc().padding(Padding.PKCS5).iv(iv).key(key).encryptToHex(plain);
    String b = Aes.cbc().padding(Padding.PKCS5).iv(iv).key(key).encryptToHex(plain);
    assertEquals(a, b);
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: 编译失败——`Aes` 与构建器不存在。

- [ ] **Step 3: 实现 `AesBuilder` 基类**

```java
package io.ituknown.crypto.aes;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Hex;
import io.ituknown.crypto.Require;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * AES 构建器基类：自类型 fluent，持有密钥/IV/标签等状态并提供全部终结方法。
 * 子类按家族暴露各自该有的配置项，实现编译期安全。
 */
abstract class AesBuilder<B extends AesBuilder<B>> {

    final AesMode mode;
    Padding padding = Padding.NONE;
    SecretKey key;
    byte[] iv;
    int tagBits = 128;

    AesBuilder(AesMode mode) {
        this.mode = mode;
    }

    @SuppressWarnings("unchecked")
    final B self() {
        return (B) this;
    }

    public B key(SecretKey key) {
        this.key = Require.requireNonNull(key, "key");
        return self();
    }

    public B iv(byte[] iv) {
        this.iv = Require.requireNonNull(iv, "iv");
        return self();
    }

    public byte[] encrypt(byte[] plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Require.requireNonNull(plaintext, "plaintext");
        byte[] iv = (this.iv != null) ? this.iv : AesEngine.generateIv(mode);
        return AesEngine.encrypt(mode, padding, key, iv, tagBits, plaintext);
    }

    public byte[] encrypt(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public String encryptToBase64(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Base64.toString(encrypt(plaintext));
    }

    public String encryptToHex(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Hex.toHexString(encrypt(plaintext));
    }

    public byte[] decrypt(byte[] combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return AesEngine.decrypt(mode, padding, key, tagBits, combined);
    }

    public String decryptToString(byte[] combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return new String(decrypt(combined), StandardCharsets.UTF_8);
    }

    public String decryptFromBase64(String combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return decryptToString(Base64.toByte(combined));
    }

    public String decryptFromHex(String combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return decryptToString(Hex.toByteArray(combined));
    }
}
```

- [ ] **Step 4: 实现 `AeadAesBuilder` / `BlockAesBuilder` / `StreamAesBuilder`**

```java
package io.ituknown.crypto.aes;

/** AEAD 家族构建器（GCM/CCM/OCB）：有认证标签长度配置，无填充。 */
public final class AeadAesBuilder extends AesBuilder<AeadAesBuilder> {

    AeadAesBuilder(AesMode mode) {
        super(mode);
    }

    public AeadAesBuilder tagBits(int tagBits) {
        this.tagBits = tagBits;
        return this;
    }
}
```

```java
package io.ituknown.crypto.aes;

/** 分组家族构建器（CBC）：需选择填充模式，无认证标签。 */
public final class BlockAesBuilder extends AesBuilder<BlockAesBuilder> {

    BlockAesBuilder(AesMode mode) {
        super(mode);
    }

    public BlockAesBuilder padding(Padding padding) {
        this.padding = padding;
        return this;
    }
}
```

```java
package io.ituknown.crypto.aes;

/** 流式家族构建器（CTR/CFB/OFB）：既无填充也无认证标签。 */
public final class StreamAesBuilder extends AesBuilder<StreamAesBuilder> {

    StreamAesBuilder(AesMode mode) {
        super(mode);
    }
}
```

- [ ] **Step 5: 实现 `Aes` 入口**

```java
package io.ituknown.crypto.aes;

/**
 * AES 对称加解密入口：按家族返回构建器，编译期阻止非法组合。
 * <pre>
 * Aes.gcm().tagBits(128).key(k).encryptToBase64(plain);   // AEAD：有 tagBits，无 padding
 * Aes.cbc().padding(Padding.PKCS5).key(k).encrypt(plain);  // 分组：有 padding，无 tagBits
 * Aes.ctr().key(k).encrypt(plain);                         // 流式：两者皆无
 *
 * // 下列写法编译失败（类型状态保护）：
 * Aes.gcm().padding(...)   // AEAD 构建器无 padding 方法
 * Aes.cbc().tagBits(...)   // 分组构建器无 tagBits 方法
 * </pre>
 */
public final class Aes {

    private Aes() {
    }

    public static AeadAesBuilder gcm() {
        return new AeadAesBuilder(AesMode.GCM);
    }

    public static AeadAesBuilder ccm() {
        return new AeadAesBuilder(AesMode.CCM);
    }

    public static AeadAesBuilder ocb() {
        return new AeadAesBuilder(AesMode.OCB);
    }

    public static BlockAesBuilder cbc() {
        return new BlockAesBuilder(AesMode.CBC);
    }

    public static StreamAesBuilder ctr() {
        return new StreamAesBuilder(AesMode.CTR);
    }

    public static StreamAesBuilder cfb() {
        return new StreamAesBuilder(AesMode.CFB);
    }

    public static StreamAesBuilder ofb() {
        return new StreamAesBuilder(AesMode.OFB);
    }
}
```

- [ ] **Step 6: 跑测试确认通过**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: PASS（含 6 个 fluent 测试）。

- [ ] **Step 7: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesBuilder.java \
        cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AeadAesBuilder.java \
        cookbook-crypto/src/main/java/io/ituknown/crypto/aes/BlockAesBuilder.java \
        cookbook-crypto/src/main/java/io/ituknown/crypto/aes/StreamAesBuilder.java \
        cookbook-crypto/src/main/java/io/ituknown/crypto/aes/Aes.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java
git commit -m "feat(crypto): 新增 Aes 类型状态链式 API 与家族构建器"
```

## Task 6: `AesUtils` 迁入 aes 子包并委托新引擎

**Files:**
- Move: `cookbook-crypto/src/main/java/io/ituknown/crypto/AesUtils.java` → `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesUtils.java`
- Move: `cookbook-crypto/src/test/java/io/ituknown/crypto/AesUtilsTest.java` → `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesUtilsTest.java`
- Test: `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java`（追加门面互通测试）

**Interfaces:**
- Consumes: T1 元数据；T3/T4 引擎；根包 `Hex`/`Base64`/`Require`。
- Produces: `io.ituknown.crypto.aes.AesUtils`（原 `io.ituknown.crypto.AesUtils` 迁包）；静态方法签名与输出格式不变，内部委托 `AesEngine`（GCM / 128 标签 / 随机 12 字节 IV）。`generateKey`/`generateEncodedKey`/`Key` record 原样随迁。

- [ ] **Step 1: 写失败测试（在 `AesTest` 追加门面互通）**

```java
import io.ituknown.crypto.aes.AesUtils;

// 类内加：

@Test
void facadeInteropWithAesGcm() throws Exception {
    javax.crypto.SecretKey key = AesUtils.generateKey(256);
    String plain = "interop check";
    // 门面加密 -> 新 API 解密
    byte[] facadeCt = AesUtils.encrypt(plain, key);
    assertEquals(plain, Aes.gcm().tagBits(128).key(key).decryptToString(facadeCt));
    // 新 API 加密 -> 门面解密
    byte[] aesCt = Aes.gcm().tagBits(128).key(key).encrypt(plain);
    assertEquals(plain, AesUtils.decrypt(aesCt, key));
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest`
Expected: 编译失败——`io.ituknown.crypto.aes.AesUtils` 不存在（仍在旧包）。

- [ ] **Step 3: 迁移并重写 `AesUtils`（新文件 `.../crypto/aes/AesUtils.java`，删除旧文件）**

```java
package io.ituknown.crypto.aes;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Hex;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * AES 对称加解密 GCM 默认门面（AES/GCM/NoPadding，128 位标签，12 字节随机 IV）。
 * 多模式用法见 {@link Aes}；输出格式与新引擎一致：IV + 密文(+认证标签)。
 *
 * @author magicianlib@gmail.com
 */
public final class AesUtils {

    private static final int TAG_BIT_LENGTH = 128;

    private AesUtils() {
    }

    public static SecretKey generateKey(int keySize) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySize);
        return keyGenerator.generateKey();
    }

    public static Key generateEncodedKey(int keySize) throws NoSuchAlgorithmException {
        SecretKey secretKey = generateKey(keySize);
        byte[] encoded = secretKey.getEncoded();
        return new Key(Hex.toHexString(encoded), Base64.toString(encoded));
    }

    public static byte[] encrypt(byte[] plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return AesEngine.encrypt(AesMode.GCM, Padding.NONE, key,
                AesEngine.generateIv(AesMode.GCM), TAG_BIT_LENGTH, plaintext);
    }

    public static byte[] encrypt(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key);
    }

    public static String encryptToBase64String(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Base64.toString(encrypt(plaintext, key));
    }

    public static String encryptToHexString(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Hex.toHexString(encrypt(plaintext, key));
    }

    public static String decrypt(byte[] combined, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return new String(AesEngine.decrypt(AesMode.GCM, Padding.NONE, key, TAG_BIT_LENGTH, combined),
                StandardCharsets.UTF_8);
    }

    public static String decryptFromBase64String(String base64String, SecretKey key)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
                   NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return decrypt(Base64.toByte(base64String), key);
    }

    public static String decryptFromHexString(String hexString, SecretKey key)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
                   NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return decrypt(Hex.toByteArray(hexString), key);
    }

    /**
     * AES 密钥的十六进制与 Base64 双编码形式。
     */
    public record Key(String hexString, String base64String) {

        public SecretKey fromHex() {
            byte[] decodedKey = Hex.toByteArray(hexString);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        }

        public SecretKey fromBase64() {
            byte[] decodedKey = Base64.toByte(base64String);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        }
    }
}
```

删除旧文件：`cookbook-crypto/src/main/java/io/ituknown/crypto/AesUtils.java`（`git rm` 或移动）。

- [ ] **Step 4: 迁移 `AesUtilsTest`（移动到 `.../crypto/aes/AesUtilsTest.java`）**

把测试文件包声明改为 `package io.ituknown.crypto.aes;`，并补上对根包工具的导入（`AesUtils` 现同包，无需导入；`Hex`/`Base64` 跨包需导入）。即在文件导入区追加：

```java
import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Hex;
```

其余测试体（含固定密文 `ciphertext` 的 `testDecrypt`、前导零 key、null 参数等）保持不变。删除旧文件 `cookbook-crypto/src/test/java/io/ituknown/crypto/AesUtilsTest.java`。

- [ ] **Step 5: 跑两个测试类确认通过**

Run: `mvn -q -pl cookbook-crypto test -Dtest=AesTest,AesUtilsTest`
Expected: PASS——`AesUtilsTest` 全绿（门面输出格式逐字节不变，含固定密文解密），`AesTest` 含新增互通测试。

- [ ] **Step 6: 跑全模块回归**

Run: `mvn -q -pl cookbook-crypto test`
Expected: PASS（所有 crypto 测试通过）。

- [ ] **Step 7: 提交**

```bash
git add cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesUtils.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesUtilsTest.java \
        cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java
git rm cookbook-crypto/src/main/java/io/ituknown/crypto/AesUtils.java \
       cookbook-crypto/src/test/java/io/ituknown/crypto/AesUtilsTest.java
git commit -m "refactor(crypto): AesUtils 迁入 aes 子包并委托新引擎"
```

## 完成标准

- `mvn -q -pl cookbook-crypto test` 全绿。
- `Aes.gcm()/.cbc()/.ctr()/.ccm()/.ocb()/.cfb()/.ofb()` 均可往返加解密；BC 填充（ISO7816/ANSI-X9.23/ISO10126/Zero/PKCS7）在 CBC 下往返通过。
- 类型状态：`Aes.gcm().padding(...)`、`Aes.cbc().tagBits(...)` 编译失败（非法组合不可表达）。
- `AesUtils` 门面行为与输出格式不变（固定密文用例仍解出原明文），与 `Aes.gcm()` 双向互通。
- 所有 AES 类型位于 `io.ituknown.crypto.aes`，根包仅留通用工具。

## 非目标（YAGNI）

- ECB、统一 `.mode(...)` 运行期选模式入口、外部无前置 IV 对接解密、AEAD 的 AAD 支持。
