package io.ituknown.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Hmac（Hash-based Message Authentication Code，基于哈希的消息认证码）。
 * <p>
 * 在哈希的基础上混入一个<b>共享密钥</b>，产出只有持密钥的一方才能计算/校验的摘要，用于同时保证
 * <b>完整性</b>（内容未被篡改）与<b>身份认证</b>（来自持密钥的一方）。
 *
 * <h3>典型场景</h3>
 * <ul>
 * <li>API 请求 / Webhook 签名验签：双方约定 secret，发送方用 HMAC 签名、接收方验签</li>
 * <li>会话 Cookie、防篡改令牌（如 JWT 的 HS256 签名段）</li>
 * <li>密钥派生（如 HKDF / PBKDF2 的底层构造）</li>
 * </ul>
 *
 * <h3>与 {@link Hash} 的区别</h3>
 * {@link Hash} <b>无密钥</b>：任何人都能算出相同摘要，只能验证完整性（如文件指纹、口令加盐存储）；
 * HMAC <b>需要密钥</b>：没有密钥就无法伪造，额外提供身份认证。
 *
 * <h3>与 {@link HashWithRsa} 的区别</h3>
 * HMAC 用<b>对称</b>共享密钥，速度快、双方需提前共享 secret；
 * {@link HashWithRsa} 用<b>非对称</b>密钥（私钥签名 / 公钥验签），适合无法共享 secret、或需要抗抵赖（非否认）的场景。
 *
 * @author magicianlib@gmail.com
 * @see Hash
 * @see HashWithRsa
 */
public enum Hmac {
    /** 推荐。 */
    HmacSHA256,
    /** 推荐。 */
    HmacSHA384,
    /** 推荐。 */
    HmacSHA512,
    /**
     * @deprecated MD5 已被密码学攻破，仅保留用于兼容已有数据；新场景请使用 {@link #HmacSHA256} 及以上。
     */
    @Deprecated
    HmacMD5,
    /**
     * @deprecated SHA-1 已被密码学攻破，仅保留用于兼容已有数据；新场景请使用 {@link #HmacSHA256} 及以上。
     */
    @Deprecated
    HmacSHA1,
    ;

    /**
     * 计算 HMAC。
     *
     * @param secret    密钥字节
     * @param plaintext 原文字节
     * @return HMAC 字节数组
     * @throws IllegalArgumentException secret 或 plaintext 为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      密钥非法
     */
    public byte[] hmac(byte[] secret, byte[] plaintext) throws NoSuchAlgorithmException, InvalidKeyException {
        Require.requireNonNull(secret, "secret");
        Require.requireNonNull(plaintext, "plaintext");
        Mac mac = Mac.getInstance(this.name());
        mac.init(new SecretKeySpec(secret, this.name()));
        return mac.doFinal(plaintext);
    }

    /**
     * 计算 HMAC（UTF-8 密钥与明文）。
     *
     * @param secret    密钥（UTF-8 字符串）
     * @param plaintext 原文（UTF-8 字符串）
     * @return HMAC 字节数组
     */
    public byte[] hmac(String secret, String plaintext) throws NoSuchAlgorithmException, InvalidKeyException {
        return hmac(secret.getBytes(StandardCharsets.UTF_8), plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 计算 HMAC 并返回十六进制字符串。
     *
     * @param secret    密钥（UTF-8 字符串）
     * @param plaintext 原文（UTF-8 字符串）
     * @return 十六进制 HMAC
     */
    public String hmacHex(String secret, String plaintext) throws NoSuchAlgorithmException, InvalidKeyException {
        return Hex.toHexString(hmac(secret, plaintext));
    }

    /**
     * 计算 HMAC 并返回 Base64 字符串。
     *
     * @param secret    密钥（UTF-8 字符串）
     * @param plaintext 原文（UTF-8 字符串）
     * @return Base64 HMAC
     */
    public String hmacBase64(String secret, String plaintext) throws NoSuchAlgorithmException, InvalidKeyException {
        return Base64.toString(hmac(secret, plaintext));
    }
}
