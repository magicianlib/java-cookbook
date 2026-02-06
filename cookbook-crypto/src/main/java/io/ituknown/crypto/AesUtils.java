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
     * 初始化向量长度
     * <p>
     * GCM 模式通常使用 12 字节的随机 IV
     */
    private static final int IV_BYTE_LENGTH = 12;
    /**
     * 认证标签，用于校验数据完整性
     * <p>
     * 从安全想考虑，最常用且推荐的值是为 128-bit。也可以选择使用 120-bit、96-bit、64-bit（但是不推荐）
     */
    private static final int TAG_BIT_LENGTH = 128;

    /**
     * 生成随机密钥
     * <p>
     * Aes 支持 3 种密钥长度：
     * <p>
     * 1. {@code AES-128}：循环 10 轮处理，最常用，平衡了安全与速度<br/>
     * 2. {@code AES-192}：循环 12 轮处理<br/>
     * 3. {@code AES-256}：循环 14 轮处理，安全性最高，军事级加密（推荐）
     *
     * @param keySize 密钥长度
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

    public static Key generateEncodedKey(int keySize) throws NoSuchAlgorithmException {
        SecretKey secretKey = generateKey(keySize);
        byte[] encoded = secretKey.getEncoded();
        String hexKey = Hex.toHexString(encoded);
        String base64Key = Base64.toString(secretKey.getEncoded());
        return new Key(hexKey, base64Key);
    }

    /**
     * 加密
     *
     * @param plaintext  待加密明文
     * @param key        密钥
     * @param production 处理 byte 流
     */
    public static <R> R encrypt(String plaintext, SecretKey key, Production<R> production) throws Exception {
        if (plaintext == null || key == null) {
            LOGGER.error("Encryption failed: Plaintext or Key is null");
            throw new IllegalArgumentException("Plaintext and Key must not be null");
        }

        try {
            byte[] iv = new byte[IV_BYTE_LENGTH];
            new SecureRandom().nextBytes(iv);

            // IV 是公开的，可以直接输出
            LOGGER.info("Encrypting data using {}, IV: {}", ALGORITHM, Hex.toHexString(iv));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BIT_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 将 IV 和 密文 拼接在一起，方便解密时读取
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            LOGGER.info("Encryption successful, total length: {} bytes", combined.length);
            return production.apply(combined);
        } catch (Exception e) {
            LOGGER.error("AES encryption failed: {}", e.getMessage());
            throw e;
        }
    }

    public static String encryptToBase64String(String plaintext, SecretKey key) throws Exception {
        return encrypt(plaintext, key, Base64::toString);
    }

    public static String encryptToHexString(String plaintext, SecretKey key) throws Exception {
        return encrypt(plaintext, key, Hex::toHexString);
    }

    /**
     * 解密
     *
     * @param combined 密文
     * @param key      密钥
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
            return new String(decryptedText);
        } catch (AEADBadTagException e) {
            // GCM 模式特有的异常：说明数据被篡改或密钥错误
            LOGGER.error("AES decryption failed: Tag mismatch! Data may be tampered or wrong key.");
            throw e;
        } catch (Exception e) {
            LOGGER.error("AES decryption failed: {}", e.getMessage());
            throw e;
        }
    }

    public static String decryptFromBase64String(String base64String, SecretKey key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] combined = Base64.toByte(base64String);
        return decrypt(combined, key);
    }

    public static String decryptFromHexString(String hexString, SecretKey key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] combined = Hex.toByteArray(hexString);
        return decrypt(combined, key);
    }

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