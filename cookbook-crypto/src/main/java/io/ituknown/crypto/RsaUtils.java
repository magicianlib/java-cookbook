package io.ituknown.crypto;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Rsa 非对称加密解密工具类
 */
public class RsaUtils {

    /**
     * 公钥加密
     *
     * @param plaintext 明文
     * @param pubKey    公钥
     */
    public static <R> R encrypt(byte[] plaintext, PublicKey pubKey, Production<R> production) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] encrypted = cipher.doFinal(plaintext);
        return production.apply(encrypted);
    }

    public static <R> R encrypt(byte[] plaintext, String base64PubKey, Production<R> production) throws Exception {
        // 公钥证书
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.toByte(base64PubKey));
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
        return encrypt(plaintext, publicKey, production); // 加密
    }

    public static <R> R encrypt(String plaintext, PublicKey pubKey, Production<R> production) throws Exception {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8), pubKey, production);
    }

    public static <R> R encrypt(String plaintext, String base64PubKey, Production<R> production) throws Exception {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8), base64PubKey, production);
    }

    public static String encryptToBase64String(String plaintext, PublicKey pubKey) throws Exception {
        return encrypt(plaintext, pubKey, Base64::toString);
    }

    public static String encryptToBase64String(String plaintext, String base64PubKey) throws Exception {
        return encrypt(plaintext, base64PubKey, Base64::toString);
    }

    /**
     * 私钥解密
     *
     * @param ciphertext 密文
     * @param priKey     私钥
     */
    public static <R> R decrypt(byte[] ciphertext, PrivateKey priKey, Production<R> production) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        byte[] decrypted = cipher.doFinal(ciphertext);
        return production.apply(decrypted);
    }

    public static <R> R decrypt(byte[] ciphertext, String base64PriKey, Production<R> production) throws Exception {
        // 私钥证书
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.toByte(base64PriKey));
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        return decrypt(ciphertext, privateKey, production);
    }

    public static <R> R decrypt(String ciphertext, PrivateKey priKey, Production<R> production) throws Exception {
        return decrypt(ciphertext.getBytes(StandardCharsets.UTF_8), priKey, production);
    }

    public static <R> R decrypt(String ciphertext, String base64PriKey, Production<R> production) throws Exception {
        return decrypt(ciphertext.getBytes(StandardCharsets.UTF_8), base64PriKey, production);
    }

    public static <R> R decryptBase64(String base64Ciphertext, PrivateKey priKey, Production<R> production) throws Exception {
        return decrypt(Base64.toByte(base64Ciphertext), priKey, production);
    }

    public static <R> R decryptBase64(String base64Ciphertext, String base64PriKey, Production<R> production) throws Exception {
        return decrypt(Base64.toByte(base64Ciphertext), base64PriKey, production);
    }



    /**
     * 生成密匙对
     *
     * @param keySize 密钥大小
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize);
        return keyGen.generateKeyPair();
    }

    public static Pair generateBase64KeyPair(int keySize) throws Exception {
        KeyPair keyPair = generateKeyPair(keySize);
        RSAPublicKey pubKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey priKey = (RSAPrivateKey) keyPair.getPrivate();
        return new Pair(Base64.toString(priKey.getEncoded()), Base64.toString(pubKey.getEncoded()));
    }

    public record Pair(String privateKey, String publicKey) {
    }
}