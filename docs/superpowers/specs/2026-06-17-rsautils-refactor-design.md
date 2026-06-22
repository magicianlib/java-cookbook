# cookbook-crypto RsaUtils 完全重构设计

> **背景**：现有 `RsaUtils` 存在多处硬伤——不支持 PEM/DER 文件加载、
> `Cipher.getInstance("RSA")` 依赖 provider 默认填充（PKCS1Padding，有 ROBOT 攻击风险）、
> 全方法 `throws Exception`、无 null 校验与日志、超长明文直接抛 `IllegalBlockSizeException`、
> 工具类未 `final` 无私有构造、测试缺 round-trip。本次对其**完全重构**。
>
> 已确认：除 `RsaUtils` 与其测试外，**无外部调用方**，可放心做不兼容变更。

## 目标

- 把 RSA 能力按职责拆为 `RsaKeys`（密钥）与 `RsaUtils`（加解密）两个 `final` 工具类。
- 密钥支持 **PKCS#8 私钥 / X.509 公钥**，**PEM 文本与 DER 二进制**两种文件形式，
  入参支持 `Path`（文件）与 `InputStream`（文件流），由调用方按场景选用。
- 加解密统一使用安全的 **OAEP（SHA-256 + MGF1-SHA256）**，移除 PKCS1。
- 超长明文**不做分段**，改为显式限长 + 文档引导（大数据应使用混合加密/AES）。
- 精确异常、SLF4J 日志、null 校验，对齐同包 `AesUtils` 的健壮性标准。
- 测试补齐 round-trip 与各类加载/错误路径。

## 约束

- 纯 Java、Java 21、slf4j-api、JUnit 5。仅用 JDK 内置 `java.security` / `javax.crypto`，**不引入 BouncyCastle**。
- 填充只保留 OAEP；PKCS1 路径删除（完全重构、无外部调用方）。
- 不做分段加密（自定义多块密文格式不通用，属反模式）。
- 不支持 PKCS#1 格式（`-----BEGIN RSA PRIVATE KEY-----`），显式标注为 out-of-scope。
- `Production<R>` 保留（`AesUtils`/`HashWithRsa` 仍依赖），仅 `RsaUtils` 不再使用。
- `HashWithRsa` 本期不动。

## 核心决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 职责拆分 | `RsaKeys`（密钥）+ `RsaUtils`（加解密） | 单一职责，密钥加载与加解密解耦 |
| 密钥加载入口 | `Path` + `InputStream` | 文件与流两种，调用方自选 |
| 格式识别 | 自动识别 PEM/DER | 按内容（`-----BEGIN `）判断，体验好且无歧义 |
| 私钥规格 | `PKCS8EncodedKeySpec` | 标准，`KeyPairGenerator` 产出格式 |
| 公钥规格 | `X509EncodedKeySpec` | 标准 SubjectPublicKeyInfo |
| 填充 | OAEP（SHA-256 + MGF1-SHA256） | PKCS1 有 ROBOT/Bleichenbacher 风险，OAEP 为现代推荐 |
| OAEP 参数 | 显式 `OAEPParameterSpec` | 转换字符串默认 MGF1=SHA-1，显式指定避免互操作意外 |
| 超长明文 | 限长 + 文档，不分段 | 分段为非标自定义格式，大数据应混合加密 |
| keySize | 强制 `>= 2048` | 弱密钥不安全，cookbook 应示范良好实践 |
| 返回值 | 核心返回 `byte[]` + Base64/String 便捷重载 | 去掉 `Production<R>`，对齐 `AesUtils` 主流写法 |
| 异常 | 精确声明（`GeneralSecurityException`/`IOException` 等） | 不再用裸 `throws Exception` |
| 密钥生成归属 | `RsaKeys` | 密钥相关全部内聚到 `RsaKeys`，`RsaUtils` 纯加解密 |

## 架构

```
调用方
  │  RsaKeys.loadPublicKey(Path/InputStream)        ──┐
  │  RsaKeys.parsePublicKeyPem(String)                 │ 密钥加载/解析/生成
  │  RsaKeys.parsePublicKeyBase64(String)              │（自动识别 PEM/DER）
  │  RsaKeys.generateKeyPair()                         │
  │                                                    │
  │  ───────────────►  PublicKey / PrivateKey 对象  ◄──┘
  │
  │  RsaUtils.encryptToBase64(plaintext, pubKey)
  │  RsaUtils.decryptFromBase64ToString(ciphertext, priKey)
  ▼
RsaUtils（OAEP 加解密 + 限长 + 安全常量）
```

## 组件

### 1. `RsaKeys`（新增，`final` + 私有构造）

密钥加载、解析、生成的唯一入口。

**加载（文件/流）**：

```java
PrivateKey loadPrivateKey(Path path)
PrivateKey loadPrivateKey(InputStream in)   // 读取后内部 close，调用方无需自行 close
PublicKey  loadPublicKey(Path path)
PublicKey  loadPublicKey(InputStream in)
```

**解析（内存字符串）**：

```java
PrivateKey parsePrivateKeyPem(String pem)       // 含 -----BEGIN----- 头
PublicKey  parsePublicKeyPem(String pem)
PrivateKey parsePrivateKeyBase64(String base64) // 裸 DER 的 Base64（兼容旧 API 迁移）
PublicKey  parsePublicKeyBase64(String base64)
```

**生成**：

```java
KeyPair    generateKeyPair()                    // 默认 2048
KeyPair    generateKeyPair(int keySize)         // < 2048 抛 IllegalArgumentException
RsaKeyPair generateBase64KeyPair()              // 默认 2048
RsaKeyPair generateBase64KeyPair(int keySize)

record RsaKeyPair(String privateKeyBase64, String publicKeyBase64)
```

**自动识别流程**：

1. 读入全部字节（`Path` 用 `Files.readAllBytes`，`InputStream` drain 到缓冲区后**内部 close**）。`InputStream` 重载的 Javadoc 必须明确标注「本方法会关闭传入的流」。
2. `isPem`：内容以 ASCII `-----BEGIN ` 开头 → PEM，否则视为 DER。
3. PEM：去除 `-----BEGIN-----` / `-----END-----` 行，拼接其余行后 Base64 解码 → DER 字节；DER：直接使用。
4. 私钥 → `PKCS8EncodedKeySpec` + `KeyFactory.getInstance("RSA")`；公钥 → `X509EncodedKeySpec`。

**支持格式（Javadoc 明列）**：

- 私钥：**PKCS#8**，PEM（`-----BEGIN PRIVATE KEY-----`）或 DER（二进制）。
- 公钥：**X.509 SubjectPublicKeyInfo**，PEM（`-----BEGIN PUBLIC KEY-----`）或 DER（二进制）。
- 不支持 PKCS#1（`-----BEGIN RSA PRIVATE KEY-----`），需 BouncyCastle，本期 out-of-scope。

**异常**：读文件 `IOException`；密钥解析 `GeneralSecurityException`（覆盖 `NoSuchAlgorithmException`/`InvalidKeySpecException` 等）。

### 2. `RsaUtils`（重写，`final` + 私有构造）

只负责加解密、长度校验、安全常量。

**常量**：

```java
String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
OAEPParameterSpec OAEP_SPEC = new OAEPParameterSpec(
    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
int DEFAULT_KEY_SIZE = 2048;
```

> 注意：`OAEPWithSHA-256AndMGF1Padding` 字符串本身默认 MGF1 仍用 SHA-1，
> 故 `init` 时必须显式传 `OAEP_SPEC`，使 hash 与 MGF1 都用 SHA-256，保证互操作正确。

**长度上限**：OAEP-SHA256 下 `maxInput = keySizeBytes − 2·32 − 2 = keySizeBytes − 66`。
2048 位密钥 → **190 字节**。加密前从 `((RSAKey) key).getModulus().bitLength()` 取实际密钥长度校验；
超限抛 `IllegalArgumentException`，消息给出上限并引导「大数据请用混合加密 / AES」。

**API**（去 `Production<R>`，核心返回 `byte[]`）：

```java
// 加密：核心 + 最常用便捷
byte[] encrypt(byte[] plaintext, PublicKey pubKey);
String encryptToBase64(String plaintext, PublicKey pubKey);   // UTF-8 明文 → Base64 密文

// 解密：核心 + Base64 入 + String 出
byte[] decrypt(byte[] ciphertext, PrivateKey priKey);
byte[] decryptFromBase64(String base64Ciphertext, PrivateKey priKey);
String decryptToString(byte[] ciphertext, PrivateKey priKey);
String decryptFromBase64ToString(String base64Ciphertext, PrivateKey priKey);  // 最常用
```

加解密重载非对称是合理的：加密输入由调用方掌控（有字节），解密输入来自外部（常是 Base64）。

**健壮性**：SLF4J 日志；null 校验抛 `IllegalArgumentException`；`throws` 精确声明
（`NoSuchAlgorithmException`/`NoSuchPaddingException`/`InvalidKeyException`/`IllegalBlockSizeException`/`BadPaddingException`），
对齐 `AesUtils`；解密 `BadPadding` 时不在异常消息里泄露敏感细节。

## 错误处理

| 场景 | 处理 |
|---|---|
| 明文/密文/密钥为 null | `IllegalArgumentException`（带消息）+ ERROR 日志 |
| 明文超长 | `IllegalArgumentException`，消息含上限与引导 |
| keySize < 2048 | `IllegalArgumentException` |
| 密钥格式非法 | `GeneralSecurityException` |
| 读文件失败 | `IOException` |
| 解密密钥不匹配/密文损坏 | 抛 `BadPaddingException`，日志记录，消息不泄露明文 |

## 测试

- **`RsaKeysTest`（新增）**：动态生成密钥对 → 写 PEM/DER 临时文件 → 经 `Path`/`InputStream` 各回读校验；
  inline PEM 字符串、Base64 字符串解析；keySize 校验（`< 2048` 拒绝，默认 2048）。
- **`RsaUtilsTest`（重写）**：
  - **round-trip**（补齐缺失）：`decrypt(encrypt(x)) == x`，覆盖加密/解密各重载组合。
  - **长度上限**：超限明文抛清晰异常。
  - **错误路径**：密钥不匹配 → `BadPaddingException`；null 入参 → `IllegalArgumentException`。
  - 用一个「真实 PEM 字符串常量」额外冒烟测 `parsePrivateKeyPem`/`parsePublicKeyPem` 解析器。
- 删除旧硬编码 `BASE64_CIPHERTEXT`（OAEP 下已失效）。

## 文件清单

- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaKeys.java`
- 重写 `cookbook-crypto/src/main/java/io/ituknown/crypto/RsaUtils.java`
- 新增 `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaKeysTest.java`
- 重写 `cookbook-crypto/src/test/java/io/ituknown/crypto/RsaUtilsTest.java`

## 迁移说明（旧 → 新）

| 旧 API | 新 API |
|---|---|
| `encrypt(s, base64PubKey, Production)` | `RsaKeys.parsePublicKeyBase64(base64)` → `RsaUtils.encryptToBase64(s, key)` |
| `encrypt(s, pubKey, Production)` | `RsaUtils.encryptToBase64(s, pubKey)`（或 `encrypt(byte[], pubKey)`） |
| `decryptBase64(b64, base64PriKey, Production)` | `RsaKeys.parsePrivateKeyBase64(base64)` → `RsaUtils.decryptFromBase64ToString(b64, key)` |
| `generateBase64KeyPair(n)` | `RsaKeys.generateBase64KeyPair(n)` |
| 仅 Base64 字符串密钥 | 现支持 PEM/DER 文件（Path/Stream）+ PEM/Base64 字符串 |

## 非目标（YAGNI）

- 分段加密 / 自定义多块密文格式。
- 混合加密（RSA+AES）helper（可作为后续独立 recipe）。
- PKCS#1 格式支持（需 BouncyCastle）。
- RSA 密文的 Hex 输出（Base64 是标准做法，非必要）。
