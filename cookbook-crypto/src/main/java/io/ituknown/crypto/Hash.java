package io.ituknown.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

/**
 * 单向散列（Hash）工具类，提供 MD5、SHA 家族与国密 SM3。
 * <p>
 * 单向散列用于将任意长度输入压缩为定长摘要，常用于完整性校验、指纹、口令存储等。
 * 与 {@link Hmac} 的区别：本类<b>无密钥</b>（任何人都能算出相同摘要）；HMAC 需要密钥参与，提供身份认证。
 * <p>
 * SM3 由 <a href="https://www.bouncycastle.org/">BouncyCastle</a> 提供支持（JDK 标准库不含 SM3）。
 *
 * @author magicianlib@gmail.com
 * @see Hmac
 */
public enum Hash {
    /** 推荐。 */
    SHA256("SHA-256"),
    /** 推荐。 */
    SHA384("SHA-384"),
    /** 推荐。 */
    SHA512("SHA-512"),
    /** 国密 SM3（256 位）。 */
    SM3("SM3"),
    /**
     * @deprecated MD5 已被密码学攻破，仅保留用于兼容已有数据；新场景请使用 {@link #SHA256} 及以上。
     */
    @Deprecated
    MD5("MD5"),
    /**
     * @deprecated SHA-1 已被密码学攻破，仅保留用于兼容已有数据；新场景请使用 {@link #SHA256} 及以上。
     */
    @Deprecated
    SHA1("SHA-1"),
    ;

    /**
     * BouncyCastle Provider，统一用于所有算法（含 JDK 未内置的 SM3）。
     * 以实例方式持有、按调用显式传入，避免 {@link java.security.Security#addProvider} 全局副作用。
     */
    private static final Provider BC = new BouncyCastleProvider();

    private final String algorithm;

    Hash(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * 计算摘要。
     *
     * @param data 原始字节
     * @return 摘要字节
     * @throws IllegalArgumentException data 为 null
     * @throws NoSuchAlgorithmException  当前 Provider 不支持该算法（正常不会发生）
     */
    public byte[] hash(byte[] data) throws NoSuchAlgorithmException {
        Require.requireNonNull(data, "data");
        return MessageDigest.getInstance(algorithm, BC).digest(data);
    }

    /**
     * 计算摘要（UTF-8 输入）。
     *
     * @param data 原文（UTF-8 字符串）
     * @return 摘要字节
     */
    public byte[] hash(String data) throws NoSuchAlgorithmException {
        return hash(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 计算摘要并返回十六进制字符串（大写）。
     *
     * @param data 原文（UTF-8 字符串）
     * @return 大写十六进制摘要
     */
    public String hashHex(String data) throws NoSuchAlgorithmException {
        return Hex.toHexString(hash(data));
    }

    /**
     * 计算摘要并返回 Base64 字符串。
     *
     * @param data 原文（UTF-8 字符串）
     * @return Base64 摘要
     */
    public String hashBase64(String data) throws NoSuchAlgorithmException {
        return Base64.toString(hash(data));
    }
}
