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
import java.security.spec.MGF1ParameterSpec;

/**
 * RSA 非对称加解密工具类（公钥加密 / 私钥解密）。
 * <p>
 * 使用 {@code RSA/ECB/OAEPWithSHA-256AndMGF1Padding}，并显式指定 hash 与 MGF1 均为 SHA-256
 * （该转换字符串默认 MGF1 用 SHA-1，显式指定以避免互操作意外）。私钥签名/验签请使用 {@link HashWithRsa}。
 * <p>
 * <b>长度限制</b>：OAEP-SHA256 下单块明文上限为 {@code keySizeBytes - 66}（2048 位密钥为 190 字节）。
 * 超长明文会抛 {@link IllegalArgumentException}。RSA 不适合加密大数据，请改用混合加密（RSA 包 AES 密钥）。
 * <p>
 * 密钥请通过 {@link RsaKeys} 获取：支持 PEM/DER 文件、PEM/裸 Base64 字符串，或直接生成密钥对。
 */
public final class RsaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RsaUtils.class);

    /** 转换名（仅用于日志，实际 init 使用 {@link #OAEP_SPEC}）。 */
    public static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /** OAEP 参数：hash=SHA-256，MGF1=SHA-256，PSource 默认。 */
    static final OAEPParameterSpec OAEP_SPEC = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

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
        Require.requireNonNull(plaintext, "plaintext");
        Require.requireNonNull(pubKey, "pubKey");
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
        Require.requireNonNull(ciphertext, "ciphertext");
        Require.requireNonNull(priKey, "priKey");
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
}
