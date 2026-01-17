package io.ituknown.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Hmac（Hash-based Message Authentication Code）是一种使用哈希函数和秘密密钥来生成消息认证码的算法。
 * <p>
 * HMAC 通过将密钥混入哈希运算中，提供了一种更强大的消息认证方法，可用于防范一些针对普通哈希的攻击。
 *
 * @author magicianlib@gmail.com
 */
public enum Hmac {
    HmacMD5,
    HmacSHA1,
    HmacSHA256,
    HmacSHA384,
    HmacSHA512,
    ;

    /**
     * @param secret    密码
     * @param plaintext 待加密明文
     */
    public byte[] hmac(byte[] secret, byte[] plaintext) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(this.name());
        SecretKeySpec keySpec = new SecretKeySpec(secret, this.name());
        mac.init(keySpec);
        return mac.doFinal(plaintext);
    }

    public byte[] hmac(String secret, String plaintext) throws NoSuchAlgorithmException, InvalidKeyException {
        return hmac(secret.getBytes(StandardCharsets.UTF_8), plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public <R> R hmac(byte[] secret, byte[] plaintext, Production<R> production) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] bytes = hmac(secret, plaintext);
        return production.apply(bytes);
    }

    public <R> R hmac(String secret, String plaintext, Production<R> production) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] bytes = hmac(secret, plaintext);
        return production.apply(bytes);
    }

    public String hmacHex(String secret, String plaintext) throws NoSuchAlgorithmException, InvalidKeyException {
        return hmac(secret, plaintext, Hex::toHexString);
    }

    public String hmacBase64(String secret, String plaintext) throws NoSuchAlgorithmException, InvalidKeyException {
        return hmac(secret, plaintext, data -> Base64.getEncoder().encodeToString(data));
    }
}