# cookbook-crypto AES 多模式类型状态重构设计

> **背景**：现有 `AesUtils` 把工作模式写死为 `AES/GCM/NoPadding`，IV 固定 12 字节、认证标签 128 位，
> 解密里写死「切前 12 字节当 IV」。模式与填充无法选择，无法演示 CBC/CTR/CCM/OCB 等主流模式，
> 也无法体现「不同模式对应不同能力」的密码学事实。
>
> 现实约束：AES 模式按行为分三大家族——**AEAD**（GCM/CCM/OCB，带认证标签、强制 NoPadding）、
> **分组**（CBC/ECB，需填充、有 IV）、**流式**（CTR/CFB/OFB，强制 NoPadding、有 IV）。三者参数对象、
> IV 长度、是否需要填充各不相同，单一链式无法通吃。此外 JDK 自带 SunJCE 仅支持 GCM + NoPadding/PKCS5Padding，
> **CCM、OCB 以及 PKCS7/ISO7816/ANSI-X9.23/ISO10126/Zero 等填充必须走 BouncyCastle**（已是 `1.78.1` 依赖）。
>
> 已确认方向：以**类型状态链式**为主推 API（每家族一条链，编译期拦截非法组合）；**分层**——
> SunJCE 原生模式默认可用，CCM/OCB/BC 填充自动懒注册 BouncyCastle；**保留 `AesUtils` 为 GCM 默认门面**
> 向后兼容。

## 目标

- 用**类型状态 fluent builder** 取代写死的 GCM：`Aes.gcm()/.cbc()/.ctr()/.ccm()/.ocb()` 等家族入口
  返回各自构建器，`.gcm()` 的构建器**根本没有** `padding()`、`.cbc()` 的构建器**根本没有** `tagBits()`，
  非法组合编译期即失败。
- **分层支持多模式**：原生（GCM/CBC/CTR/CFB/OFB）开箱即用；BC 专属（CCM/OCB/BC 填充）首次使用时
  幂等懒注册 BouncyCastle，使用者无需手动管 provider。
- **IV 策略可显式可控**：默认 `SecureRandom` 生成模式对应长度的随机 IV 并前置；提供 `.iv(byte[])`
  显式入口供确定性演示 / 自带 IV 场景。
- **向后兼容**：`AesUtils` 现有静态方法保留为 GCM 默认门面，输出字节格式不变，现有测试保持绿色。

## 约束

- 纯 Java、Java 21、`javax.crypto`（JCE）、BouncyCastle `bcprov-jdk18on 1.78.1`、slf4j-api、JUnit 5。
- 仅对称 AES；密钥生成（`generateKey`/`generateEncodedKey`）保持不变。
- **不含 ECB**（模式泄露、不安全）。
- 类型状态链式为主推 API；**不建**统一 `.mode(...)` 运行期选模式入口（非目标，可后续加）。
- **不做**「外部密文无前置 IV」的对接解密（非目标，可后续加 `.decrypt(ciphertext, iv)`）。
- 所有 AES 相关类型集中到独立子包 `io.ituknown.crypto.aes`（见「包结构」），不污染根包。

## 核心决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| API 形态 | 类型状态 fluent，家族入口 | 编译期挡非法组合；API 形状本身即教材（打 `.cbc().` IDE 只列 padding/iv/key）|
| 家族划分 | AEAD / 分组 / 流式 三类构建器 | 密码学行为天然分三类；同类模式共享链形状，避免类爆炸 |
| 入口归属 | 顶层 `Aes` 静态工厂 `.gcm()/.cbc()/...` | 与 `Hex`/`Base64` 同级顶层类型，便于发现 |
| 模式范围 | 分层：原生默认 + BC 可选 | 原生零配置；BC 模式按需懒注册，不污染默认路径 |
| BC 接入 | 工具类自动懒注册（幂等、线程安全）| 使用者无需关心 provider；选 CCM/OCB/BC 填充即自动走 BC |
| IV 策略 | 默认 `SecureRandom` 随机 + 前置；`.iv(byte[])` 显式 | 默认安全可用；显式入口支撑确定性演示与自带 IV |
| 认证标签 | AEAD 构建器 `.tagBits(int)`，默认 128 | AEAD 模式特有；标签由 `Cipher` 自动拼入密文末尾 |
| 现有 API | `AesUtils` 保留为 GCM 门面，委托新引擎 | 向后兼容，输出格式不变，测试不破 |

## 包结构

AES 相关类型集中到独立子包 `io.ituknown.crypto.aes`，避免与 `Argon2Utils`/`RsaUtils`/`Hash`/`Hmac`
等平级类混在 `io.ituknown.crypto` 根包影响理解：

- `io.ituknown.crypto.aes`（新增子包）：`Aes`、`AesBuilder`（包级基类）、`AeadAesBuilder`、`BlockAesBuilder`、
  `StreamAesBuilder`、`Padding`、`AesMode`（包级）、`AesEngine`（包级）、`BouncyCastleSupport`（包级）、`AesUtils`（GCM 门面）。
- `io.ituknown.crypto`（根包，不变）：`Hex`、`Base64`（public，子包直接 import）、`Hash`/`Hmac`/
  `RsaUtils`/`EcdsaKeys`/`Argon2Utils` 等。
- `Require`（根包）由包级私有**提升为 public**：子包需复用以保持 `AesUtils` 门面抛
  `IllegalArgumentException` 的既有契约不变；与 `Hex`/`Base64` 同为 public 通用工具，最小且合理。

依赖方向单向无环：子包 → 根包 public 工具（`Hex`/`Base64`/`Require`）。

## 架构

```
调用方
  │  Aes.gcm()/.ccm()/.ocb()  ──┐  返回 AeadAesBuilder  （.tagBits / .iv / .key）
  │  Aes.cbc()                ──┤  返回 BlockAesBuilder （.padding / .iv / .key）
  │  Aes.ctr()/.cfb()/.ofb()  ──┘  返回 StreamAesBuilder（.iv / .key）
  │
  │  .encrypt*/.decrypt*  终结 → AesEngine
  ▼
AesEngine（共用加密引擎）
  │  拼 AES/<模式>/<填充> → 按 requiresBc 选 provider → 按家族选参数对象 → Cipher.doFinal
  │  组合输出 = IV + 密文（AEAD 标签由 Cipher 自动拼入末尾）
  │
  ├─ 原生路径：Cipher.getInstance(transformation)
  └─ BC 路径： BouncyCastleSupport.ensureRegistered() → Cipher.getInstance(transformation, "BC")
  ▼
BouncyCastleSupport：幂等懒注册 Security.addProvider(new BouncyCastleProvider())
  ▼
AesUtils（GCM 门面，不变签名）：静态方法委托 AesEngine，GCM/128 标签/12 IV，输出格式与现状一致
```

## 组件

> 以下所有类均在 `io.ituknown.crypto.aes` 子包；`AesMode`/`AesEngine`/`BouncyCastleSupport` 为包级私有。

### 1. `Aes`（新增，入口顶层 final 类）

按家族返回构建器的静态工厂，构建器构造时绑定具体 `AesMode`：

```java
public static AeadAesBuilder  gcm() { return new AeadAesBuilder(AesMode.GCM); }  // 原生
public static AeadAesBuilder  ccm() { return new AeadAesBuilder(AesMode.CCM); }  // BC
public static AeadAesBuilder  ocb() { return new AeadAesBuilder(AesMode.OCB); }  // BC
public static BlockAesBuilder cbc() { return new BlockAesBuilder(AesMode.CBC); } // 原生
public static StreamAesBuilder ctr() { return new StreamAesBuilder(AesMode.CTR); }// 原生
public static StreamAesBuilder cfb() { return new StreamAesBuilder(AesMode.CFB); }// 原生
public static StreamAesBuilder ofb() { return new StreamAesBuilder(AesMode.OFB); }// 原生
```

无 `ecb()`（不安全，不提供）。无统一 `.mode(...)`（非目标）。

### 2. 三个构建器（新增，顶层 final 类）

共享同一套终结方法签名（见下），差异仅在各自该有的配置项——**这正是类型状态的体现**：

**`AeadAesBuilder`**（GCM/CCM/OCB 共用）：

```java
AeadAesBuilder tagBits(int bits)   // 认证标签位数，默认 128
AeadAesBuilder iv(byte[] iv)       // 显式 IV；不调则 SecureRandom 随机生成
AeadAesBuilder key(SecretKey key)  // 必填
// 终结
byte[] encrypt(byte[] plaintext)
byte[] encrypt(String plaintext)              // UTF-8
String encryptToBase64(String plaintext)
String encryptToHex(String plaintext)
byte[] decrypt(byte[] combined)               // 返回明文字节
String decrypt(String combined)               // UTF-8 解码
String decryptFromBase64(String combined)
String decryptFromHex(String combined)
```

**`BlockAesBuilder`**（CBC）：

```java
BlockAesBuilder padding(Padding padding)      // 必填（分组模式需填充）
BlockAesBuilder iv(byte[] iv)                 // 显式 IV；不调则随机 16 字节
BlockAesBuilder key(SecretKey key)
// 终结方法同上
```

**`StreamAesBuilder`**（CTR/CFB/OFB）：

```java
StreamAesBuilder iv(byte[] iv)                // 显式 IV；不调则随机 16 字节
StreamAesBuilder key(SecretKey key)
// 终结方法同上（无 padding、无 tagBits）
```

> 终结方法返回值：加密返回 `IV + 密文(+AEAD标签)` 的字节数组/编码串；解密核心 `decrypt(byte[])`
> 返回明文 `byte[]`，`String`/Base64/Hex 变体按 UTF-8 解码明文。三构建器终结签名一致，统一由包级基类
> `AesBuilder` 持有全部状态（mode/padding/key/iv/tagBits）并实现终结方法，三个 public 子类仅暴露各自
> 该有的配置项（类型状态），其余逻辑零重复。

### 3. `Padding`（新增枚举）

```java
public enum Padding {
    NONE("NoPadding", false),        // AEAD/流式必选；分组可选（明文需对齐 16 字节）
    PKCS5("PKCS5Padding", false),    // 分组家族，SunJCE 原生
    PKCS7("PKCS7Padding", true),     // BC（AES 下与 PKCS5 等价，Javadoc 注明）
    ISO7816("ISO7816-4Padding", true),
    ANSI_X9_23("X9.23Padding", true),
    ISO10126("ISO10126-2Padding", true),
    ZERO("ZeroBytePadding", true);

    final String transformation;   // 转换串片段
    final boolean requiresBc;      // 是否必须走 BouncyCastle
}
```

合法组合约束：`padding` 仅对分组家族生效；AEAD/流式恒为 `NONE`（由类型状态保证——它们的构建器
没有 `padding()` 方法）。

### 4. `AesMode`（新增，包级枚举，模式元数据）

```java
enum AesMode {
    GCM ("GCM", Family.AEAD,   12, false),
    CCM ("CCM", Family.AEAD,   12, true),
    OCB ("OCB", Family.AEAD,   12, true),
    CBC ("CBC", Family.BLOCK,  16, false),
    CTR ("CTR", Family.STREAM, 16, false),
    CFB ("CFB", Family.STREAM, 16, false),
    OFB ("OFB", Family.STREAM, 16, false);

    enum Family { AEAD, BLOCK, STREAM }

    final String transformation;   // 转换串片段
    final Family family;
    final int ivLength;            // 推荐 IV 字节长度
    final boolean requiresBc;      // 模式本身是否必须走 BC
}
```

### 5. `AesEngine`（新增，包级共用引擎）

无状态工具方法，三构建器与 `AesUtils` 门面共用：

```java
static byte[] encrypt(AesMode mode, Padding padding, SecretKey key, byte[] iv, int tagBits, byte[] plaintext)
static byte[] decrypt(AesMode mode, Padding padding, SecretKey key, int tagBits, byte[] combined)
```

`encrypt` 流程：

1. `transformation = "AES/" + mode.transformation + "/" + padding.transformation`
2. 若 `mode.requiresBc || padding.requiresBc`：`BouncyCastleSupport.ensureRegistered()`，
   `Cipher.getInstance(transformation, "BC")`；否则 `Cipher.getInstance(transformation)`
3. 按家族选参数对象：
   - GCM → `new GCMParameterSpec(tagBits, iv)`
   - CCM/OCB → `new AEADParameters(new KeyParameter(key.getEncoded()), tagBits, iv)`
   - BLOCK/STREAM → `new IvParameterSpec(iv)`
4. `cipher.init(ENCRYPT_MODE, key, params)` → `ciphertext = cipher.doFinal(plaintext)`
   （AEAD 标签由 `Cipher` 自动拼入 `ciphertext` 末尾，无需特殊处理）
5. 返回 `concat(iv, ciphertext)`

`decrypt` 流程：从 `combined` 头部切 `mode.ivLength` 字节作 IV，余下为密文(+标签)，按同法选参数对象、
`DECRYPT_MODE` 调 `doFinal`。因加密/解密用同一家族构建器，IV 长度自洽。

### 6. `BouncyCastleSupport`（新增，包级）

```java
static void ensureRegistered() {
    if (Security.getProvider("BC") == null) {
        synchronized (BouncyCastleSupport.class) {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        }
    }
}
```

双重检查锁，幂等，线程安全。仅在 BC 路径首次调用时触发。

### 7. `AesUtils`（改造为门面）

现有全部静态方法签名**保留**，实现改为委托 `AesEngine`（GCM / 128 标签 / 12 字节随机 IV，
IV 前置 + 标签拼入，输出格式与现状**逐字节一致**）：

```java
public static byte[] encrypt(byte[] plaintext, SecretKey key)            // → AesEngine.encrypt(GCM, NONE, key, randIv, 128, plain)
public static byte[] encrypt(String plaintext, SecretKey key)
public static String encryptToBase64String(String plaintext, SecretKey key)
public static String encryptToHexString(String plaintext, SecretKey key)
public static String decrypt(byte[] combined, SecretKey key)
public static String decryptFromBase64String(String base64String, SecretKey key)
public static String decryptFromHexString(String hexString, SecretKey key)
```

`generateKey` / `generateEncodedKey` / `Key` record 不变。类级 Javadoc 更新为「GCM 默认门面，
更多模式见 `Aes`」。

## 模式覆盖清单

| 家族 | 模式 | IV 长度 | 参数对象 | 填充选项 | Provider |
|---|---|---|---|---|---|
| AEAD | GCM | 12 | `GCMParameterSpec` | NoPadding | SunJCE 原生 |
| AEAD | CCM | 12 | BC `AEADParameters` | NoPadding | BC |
| AEAD | OCB | 12 | BC `AEADParameters` | NoPadding | BC |
| 分组 | CBC | 16 | `IvParameterSpec` | NoPadding/PKCS5（原生）；PKCS7/ISO7816/ANSI_X9_23/ISO10126/Zero（BC） | SunJCE / BC |
| 流式 | CTR | 16 | `IvParameterSpec` | NoPadding | SunJCE 原生 |
| 流式 | CFB | 16 | `IvParameterSpec` | NoPadding | SunJCE 原生 |
| 流式 | OFB | 16 | `IvParameterSpec` | NoPadding | SunJCE 原生 |

## 错误处理

| 场景 | 处理 |
|---|---|
| `plaintext` / `key` 为 null | `IllegalArgumentException`（`Require.requireNonNull`）|
| AEAD 解密标签校验失败 / 密钥错 | GCM 透传 `AEADBadTagException`；CCM/OCB 由 BC 抛对应异常并记 ERROR 日志 |
| CBC + NoPadding 明文非 16 倍数 | 透传 `IllegalBlockSizeException` |
| 显式 IV 长度不符模式要求 | 由 `Cipher` 抛 `InvalidAlgorithmParameterException`（引擎不预校验，避免重复规则）|
| BC 模式/填充但 classpath 无 BC | `NoSuchProviderException`（BC 为硬依赖，正常不触发；懒注册兜底）|
| 解密输入短于 IV 长度 | `IllegalArgumentException("Invalid ciphertext")`（沿用现状）|

## 测试

- **`AesTest`（新增）**：每个家族 × 代表模式的加密→解密往返——GCM、CBC+PKCS5、CBC+ISO7816(BC)、
  CTR、CFB、OFB、CCM(BC)、OCB(BC)。
- **BC 填充往返**：CBC + PKCS7 / ANSI_X9_23 / ISO10126 / Zero 各一轮。
- **确定性演示**：同一密钥 + 同一明文 + `.iv(fixed)` → 密文逐字节相同（讲清 IV 作用）；
  不指定 IV 时两次加密密文不同（随机性）。
- **类型状态安全**：以代码片段/Javadoc 示例展示 `Aes.gcm().padding(...)`、`Aes.cbc().tagBits(...)`
  根本无法编译（非法组合在编译期消失）。
- **`AesUtilsTest`（保留）**：门面回归，断言不变（证明 GCM 路径行为与输出格式不变）。
- **懒注册幂等**：并发/重复调用 `BouncyCastleSupport.ensureRegistered()` 后 `Security.getProviders()`
  仅含一个 `BC`。

## 文件清单

- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/Aes.java`
- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesBuilder.java`（包级基类）
- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AeadAesBuilder.java`
- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/BlockAesBuilder.java`
- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/StreamAesBuilder.java`
- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/Padding.java`
- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesMode.java`（包级）
- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/AesEngine.java`（包级）
- 新增 `cookbook-crypto/src/main/java/io/ituknown/crypto/aes/BouncyCastleSupport.java`（包级）
- 迁移 `AesUtils.java`：`.../crypto/AesUtils.java` → `.../crypto/aes/AesUtils.java`（瘦身为 GCM 门面，签名与输出格式不变）
- 改 `cookbook-crypto/src/main/java/io/ituknown/crypto/Require.java`：包级私有 → public（供子包复用，保持门面异常契约）
- 迁移测试 `AesUtilsTest.java`：`.../crypto/` → `.../crypto/aes/`（更新 import，断言不变）
- 新增 `cookbook-crypto/src/test/java/io/ituknown/crypto/aes/AesTest.java`

## 非目标（YAGNI）

- **ECB**（不安全，不提供）。
- 统一 `.mode(...)` 运行期选模式入口（类型状态为主；运行期选模式可后续加「逃生舱」）。
- 「外部密文无前置 IV」的对接解密 `.decrypt(ciphertext, iv)`（当前默认自包含往返已覆盖教学；
  纯对接场景出现时再加）。
- AEAD 的 AAD（附加认证数据）支持（可后续按需加 `.aad(byte[])`）。
- 密钥派生（KDF）、密钥轮换、IV 复用检测等密钥管理议题。

## 迁移说明（旧 → 新）

> 注：`AesUtils` 已迁至 `io.ituknown.crypto.aes` 包，旧 import `io.ituknown.crypto.AesUtils` 需改为
> `io.ituknown.crypto.aes.AesUtils`；门面签名与输出格式不变。新代码推荐直接用 `Aes`。

| 旧 API（`AesUtils`，保留可用） | 等价新 API（`Aes`） |
|---|---|
| `AesUtils.encrypt(plain, key)` | `Aes.gcm().tagBits(128).key(key).encrypt(plain)` |
| `AesUtils.encryptToBase64String(plain, key)` | `Aes.gcm().key(key).encryptToBase64(plain)` |
| `AesUtils.decrypt(combined, key)` | `Aes.gcm().tagBits(128).key(key).decrypt(combined)` |
| （无）CBC | `Aes.cbc().padding(Padding.PKCS5).key(key).encrypt(plain)` |
| （无）CTR | `Aes.ctr().key(key).encrypt(plain)` |
| （无）CCM/OCB | `Aes.ccm().tagBits(128).key(key).encrypt(plain)` / `Aes.ocb()...` |
