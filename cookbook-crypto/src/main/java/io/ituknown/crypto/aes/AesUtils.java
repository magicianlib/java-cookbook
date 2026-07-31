package io.ituknown.crypto.aes;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Hex;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * AES 对称加解密 GCM 默认门面（AES/GCM/NoPadding，128 位标签，12 字节随机 IV）。
 * 多模式用法见同包的 Aes 入口类；输出格式与新引擎一致：IV + 密文(+认证标签)。
 *
 * @author magicianlib@gmail.com
 */
public final class AesUtils {

    private static final int TAG_BIT_LENGTH = 128;

    private AesUtils() {
    }

    /**
     * 生成指定长度的随机对称密钥。长度仅支持 128、192、256 位，其它取值将抛出参数非法异常。
     */
    public static SecretKey generateKey(int keySize) throws NoSuchAlgorithmException {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new IllegalArgumentException("密钥长度仅支持 128、192、256 位");
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySize);
        return keyGenerator.generateKey();
    }

    /**
     * 生成指定长度的随机对称密钥，同时给出其十六进制与 Base64 双编码形式。长度仅支持 128、192、256 位，其它取值将抛出参数非法异常。
     */
    public static Key generateEncodedKey(int keySize) throws NoSuchAlgorithmException {
        SecretKey secretKey = generateKey(keySize);
        byte[] encoded = secretKey.getEncoded();
        return new Key(Hex.toHexString(encoded), Base64.toString(encoded));
    }

    /**
     * 用默认认证加密模式加密明文，输出含随机初始化向量的密文。
     */
    public static byte[] encrypt(byte[] plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return AesEngine.encrypt(AesMode.GCM, Padding.NONE, key,
                AesEngine.generateIv(AesMode.GCM), TAG_BIT_LENGTH, plaintext);
    }

    /**
     * 用默认认证加密模式加密文本明文，输出含随机初始化向量的密文。
     */
    public static byte[] encrypt(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key);
    }

    /**
     * 加密文本明文，并以 Base64 编码返回密文。
     */
    public static String encryptToBase64String(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Base64.toString(encrypt(plaintext, key));
    }

    /**
     * 加密文本明文，并以十六进制编码返回密文。
     */
    public static String encryptToHexString(String plaintext, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Hex.toHexString(encrypt(plaintext, key));
    }

    /**
     * 用默认认证加密模式解密密文，返回文本明文。
     */
    public static String decrypt(byte[] combined, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return new String(AesEngine.decrypt(AesMode.GCM, Padding.NONE, key, TAG_BIT_LENGTH, combined),
                StandardCharsets.UTF_8);
    }

    /**
     * 解密 Base64 编码的密文，返回文本明文。
     */
    public static String decryptFromBase64String(String base64String, SecretKey key)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return decrypt(Base64.toByte(base64String), key);
    }

    /**
     * 解密十六进制编码的密文，返回文本明文。
     */
    public static String decryptFromHexString(String hexString, SecretKey key)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return decrypt(Hex.toByteArray(hexString), key);
    }

    /**
     * AES 密钥的十六进制与 Base64 双编码形式。
     */
    public record Key(String hexString, String base64String) {

        /**
         * 由十六进制编码还原对称密钥。
         */
        public static SecretKey ofHex(String hexString) {
            return new SecretKeySpec(Hex.toByteArray(hexString), "AES");
        }

        /**
         * 由 Base64 编码还原对称密钥。
         */
        public static SecretKey ofBase64(String base64String) {
            return new SecretKeySpec(Base64.toByte(base64String), "AES");
        }

        public SecretKey fromHex() {
            return ofHex(hexString);
        }

        public SecretKey fromBase64() {
            return ofBase64(base64String);
        }
    }
}