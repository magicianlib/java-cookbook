package io.ituknown.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * AES 对称加解密工具类（AES/GCM/NoPadding）。
 *
 * <h3>作用与应用场景</h3>
 * AES（Advanced Encryption Standard，高级加密标准）是一种<b>对称</b>分组加密算法：加密与解密使用<b>同一把密钥</b>，
 * 加解密速度快、安全性高，是目前最通用的数据加密标准。本类默认采用 GCM 模式，除加密外还附带完整性校验（防篡改）。
 * <p>
 * 典型应用场景：
 * <ul>
 *   <li>数据库中敏感字段加密（如手机号、身份证号、银行卡号等）</li>
 *   <li>接口 / 网络传输中载荷数据的加解密</li>
 *   <li>配置文件、缓存、会话 Token 等机密内容的保护</li>
 *   <li>前后端约定的对称加密通道（通信双方安全共享同一把密钥）</li>
 * </ul>
 *
 * <h3>核心参数说明</h3>
 * 本类涉及三类核心参数，各自承担不同职责，相互独立又协同保障安全：
 * <ul>
 *   <li><b>密钥长度（Key Size）</b>：取 128 / 192 / 256 位，决定加解密的<b>安全强度</b>与处理轮数（10 / 12 / 14 轮）。
 *       长度越长越安全、速度略慢。由 {@link #generateKey(int)} 按需生成，推荐 {@code AES-256}。</li>
 *   <li><b>初始化向量（IV）</b>：本类使用 12 字节（96 位）随机 IV（见 {@link #IV_BYTE_LENGTH}）。
 *       其作用是<b>引入随机性</b>——使「同一密钥 + 同一明文」每次加密都产生不同密文；
 *       IV 本身<b>无需保密</b>，可与密文一并传输（解密方需要它）。
 *       允许长度：GCM 下 IV 理论可为任意长度，但 12 字节（96 位）是 NIST 推荐的标准长度，
 *       性能与安全性最优，故本类固定采用。约束：同一密钥下 IV 必须唯一，不可重复复用。</li>
 *   <li><b>认证标签长度（TAG_BIT_LENGTH）</b>：默认 128 位（见 {@link #TAG_BIT_LENGTH}）。
 *       其作用是提供<b>完整性 / 认证</b>校验——解密时若密文被篡改或密钥不匹配会抛出
 *       {@link AEADBadTagException}，据此识别篡改与错误密钥。</li>
 * </ul>
 *
 * <h3>三者关系</h3>
 * <ul>
 *   <li>职责互补：密钥管「机密性」、IV 管「随机性 / 抗重放」、TAG 管「完整性 / 认证」，缺一不可。</li>
 *   <li>互不绑定：密钥长度与 IV 长度、TAG 长度<b>没有数值上的耦合</b>，可独立选择；
 *       GCM 模式下 IV 固定 12 字节、TAG 上限 128 位（受分组大小限制）是算法本身的约束，而非密钥长度决定。</li>
 *   <li>协同计算：GCM 的认证标签由<b>密钥 + IV + 密文</b>共同计算得到，故密钥或 IV 变化都会导致标签变化。</li>
 * </ul>
 *
 * @author magicianlib@gmail.com
 */
public final class AesUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(AesUtils.class);

    /**
     * 工作模式
     * <p>
     * 由于 AES 每次只能处理 16 字节，所以在处理长内容时需要选择具体的工作模式：
     * <p>
     * ECB（电子密码本）：最简单，但相同明文块会产生相同密文块，不安全，容易被看出模式<br/>
     * CBC（密码分组链接）：每个块都与前一个块关联，安全性高，但不支持并行计算<br/>
     * GCM（伽罗瓦/计数器模式）：目前最推荐。它不仅加密，还能验证数据是否被篡改（具备完整性校验）。另外 GCM 模式不需要填充
     */
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    /**
     * 初始化向量长度（单位：字节）
     * <p>
     * GCM 模式允许 IV 取任意长度，但 NIST 标准推荐 <b>96 位（即 12 字节）</b>：当 IV 恰为 96 位时，
     * 可直接作为计数器初值，性能最佳、安全性分析最清晰；其他长度需经 GHASH 运算折算，
     * 产生额外开销且无额外安全收益。故本类固定使用 12 字节随机 IV。
     */
    private static final int IV_BYTE_LENGTH = 12;
    /**
     * 认证标签，用于校验数据完整性
     * <p>
     * 从安全想考虑，最常用且推荐的值是为 128-bit。也可以选择使用 120-bit、96-bit、64-bit（但是不推荐）
     */
    private static final int TAG_BIT_LENGTH = 128;

    private AesUtils() {
    }

    /**
     * 生成随机密钥
     * <p>
     * Aes 支持 3 种密钥长度：
     * <p>
     * 1. {@code AES-128}：循环 10 轮处理，最常用，平衡了安全与速度<br/>
     * 2. {@code AES-192}：循环 12 轮处理<br/>
     * 3. {@code AES-256}：循环 14 轮处理，安全性最高，军事级加密（推荐）
     *
     * @param keySize 密钥长度（位）
     * @return AES 密钥
     * @throws NoSuchAlgorithmException 不支持 AES 算法
     */
    public static SecretKey generateKey(int keySize) throws NoSuchAlgorithmException {
        LOGGER.debug("Generating AES key with size: {} bits", keySize);
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(keySize);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to generate AES key: Algorithm not found", e);
            throw e;
        }
    }

    /**
     * 生成随机密钥并返回其十六进制与 Base64 编码形式。
     *
     * @param keySize 密钥长度（位）
     * @return 编码后的密钥（含 hex 与 base64 两种形式）
     * @throws NoSuchAlgorithmException 不支持 AES 算法
     */
    public static Key generateEncodedKey(int keySize) throws NoSuchAlgorithmException {
        SecretKey secretKey = generateKey(keySize);
        byte[] encoded = secretKey.getEncoded();
        return new Key(Hex.toHexString(encoded), Base64.toString(encoded));
    }

    /**
     * 加密（核心方法）。返回「IV + 密文 + 认证标签」拼接的字节数组（IV 公开，无需保密）。
     *
     * @param plaintext 明文字节
     * @param key       密钥
     * @return IV + 密文 拼接的字节数组
     * @throws IllegalArgumentException plaintext 或 key 为 null
     * @throws NoSuchAlgorithmException 不支持 AES/GCM
     * @throws NoSuchPaddingException   不支持 GCM 填充
     * @throws InvalidAlgorithmParameterException GCM 参数非法
     * @throws InvalidKeyException      密钥非法
     * @throws IllegalBlockSizeException 加密块大小非法
     * @throws BadPaddingException      填充非法
     */
    public static byte[] encrypt(byte[] plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Require.requireNonNull(plaintext, "plaintext");
        Require.requireNonNull(key, "key");

        byte[] iv = new byte[IV_BYTE_LENGTH];
        new SecureRandom().nextBytes(iv);

        // IV 是公开的，可以直接输出
        LOGGER.info("Encrypting data using {}, IV: {}", ALGORITHM, Hex.toHexString(iv));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BIT_LENGTH, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        // 将 IV 和 密文 拼接在一起，方便解密时读取
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        LOGGER.info("Encryption successful, total length: {} bytes", combined.length);
        return combined;
    }

    /**
     * 加密（UTF-8 明文）。
     *
     * @param plaintext 明文（UTF-8 字符串）
     * @param key       密钥
     * @return IV + 密文 拼接的字节数组
     */
    public static byte[] encrypt(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key);
    }

    /**
     * 加密便捷方法：UTF-8 明文 → Base64 密文。
     *
     * @param plaintext 明文（UTF-8 字符串）
     * @param key       密钥
     * @return Base64 密文
     */
    public static String encryptToBase64String(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Base64.toString(encrypt(plaintext, key));
    }

    /**
     * 加密便捷方法：UTF-8 明文 → 十六进制密文。
     *
     * @param plaintext 明文（UTF-8 字符串）
     * @param key       密钥
     * @return 十六进制密文
     */
    public static String encryptToHexString(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Hex.toHexString(encrypt(plaintext, key));
    }

    /**
     * 解密。输入为 {@link #encrypt(byte[], SecretKey)} 产出的「IV + 密文」拼接字节数组，返回 UTF-8 明文。
     *
     * @param combined IV + 密文 拼接的字节数组
     * @param key      密钥
     * @return UTF-8 明文
     * @throws IllegalArgumentException combined 为 null 或长度小于 IV 长度
     * @throws NoSuchAlgorithmException 不支持 AES/GCM
     * @throws NoSuchPaddingException   不支持 GCM 填充
     * @throws InvalidAlgorithmParameterException GCM 参数非法
     * @throws InvalidKeyException      密钥非法
     * @throws IllegalBlockSizeException 密文块大小非法
     * @throws BadPaddingException      密钥不匹配或密文被篡改（GCM tag 校验失败）
     */
    public static String decrypt(byte[] combined, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (combined == null || combined.length < IV_BYTE_LENGTH) {
            LOGGER.error("Decryption failed: Invalid ciphertext length");
            throw new IllegalArgumentException("Invalid ciphertext");
        }
        try {
            // 分离出 IV
            byte[] iv = new byte[IV_BYTE_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // 分离出 密文
            int ciphertextLen = combined.length - IV_BYTE_LENGTH;
            byte[] ciphertext = new byte[ciphertextLen];
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            LOGGER.info("Decrypting data, IV: {}, Ciphertext length: {}", Hex.toHexString(iv), ciphertextLen);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BIT_LENGTH, iv));

            byte[] decryptedText = cipher.doFinal(ciphertext);
            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            // GCM 模式特有的异常：说明数据被篡改或密钥错误
            LOGGER.error("AES decryption failed: Tag mismatch! Data may be tampered or wrong key.");
            throw e;
        } catch (Exception e) {
            LOGGER.error("AES decryption failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 解密便捷方法：Base64 密文 → UTF-8 明文。
     *
     * @param base64String Base64 密文
     * @param key          密钥
     * @return UTF-8 明文
     */
    public static String decryptFromBase64String(String base64String, SecretKey key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return decrypt(Base64.toByte(base64String), key);
    }

    /**
     * 解密便捷方法：十六进制密文 → UTF-8 明文。
     *
     * @param hexString 十六进制密文
     * @param key       密钥
     * @return UTF-8 明文
     */
    public static String decryptFromHexString(String hexString, SecretKey key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return decrypt(Hex.toByteArray(hexString), key);
    }

    /**
     * AES 密钥的十六进制与 Base64 双编码形式。
     *
     * @param hexString   密钥的十六进制编码
     * @param base64String 密钥的 Base64 编码
     */
    public record Key(String hexString, String base64String) {
        /**
         * 由十六进制编码还原 AES 密钥。
         *
         * @return AES 密钥
         */
        public SecretKey fromHex() {
            byte[] decodedKey = Hex.toByteArray(hexString);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        }

        /**
         * 由 Base64 编码还原 AES 密钥。
         *
         * @return AES 密钥
         */
        public SecretKey fromBase64() {
            byte[] decodedKey = Base64.toByte(base64String);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        }
    }
}
