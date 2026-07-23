package io.ituknown.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 常见的 HashWithEcdsa 组合算法，用于数据签名与验签。
 * <p>
 * HashWithEcdsa = ECDSA + 哈希函数：用私钥对消息摘要签名、用公钥验签，用于防篡改与身份认证。
 * <b>密钥</b>请通过 {@link EcdsaKeys} 获取（PEM/DER 文件、PEM/裸 Base64 字符串、或生成密钥对），
 * 再传入本类的 {@link #signature} / {@link #verify} 方法。
 * <p>
 * 与 {@link HashWithRsa} 相比，ECDSA 基于椭圆曲线：<b>密钥强度由曲线决定</b>（见 {@link EcdsaKeys.Curve}），
 * 在同等安全强度下密钥更短、签名更小。两者签名语义一致——私钥签名、公钥验签。
 *
 * <h3>签名编码 {@link Encoding}</h3>
 * <ul>
 *   <li>{@link Encoding#DER DER}（默认）：ASN.1 编码（{@code SEQUENCE{INTEGER r, INTEGER s}}），变长。
 *       JDK/openssl {@code dgst -sign} 等经典场景使用。</li>
 *   <li>{@link Encoding#RAW RAW}（IEEE P1363）：{@code r} 与 {@code s} 按域长度零填充后拼接，<b>定长</b>
 *       （P-256 为 64 字节）。<b>JWT 的 ES256 / ES384 / ES512 采用此编码</b>，与 Go / Node / Python / Rust 等互操作时通常需要。</li>
 * </ul>
 * 不带 {@link Encoding} 参数的便捷方法默认使用 {@link Encoding#DER}；JWT 等场景请显式传入 {@link Encoding#RAW}。
 *
 * <h3>签名过程（防篡改）</h3>
 * <ul>
 * <li>A 生成一对密钥（公钥和私钥）。私钥不公开，A 自己保留。公钥为公开的，任何人可以获取。</li>
 * <li>A 用自己的私钥对消息进行加签，形成签名，并将签名和消息本身一起传递给 B。</li>
 * <li>B 收到消息后，通过 A 的公钥进行验签。如果验签成功，则证明消息是 A 发送的。</li>
 * </ul>
 * <p>整个过程，只有使用 A 私钥签名的消息才能被验签成功。即使知道了消息内容，也无法伪造签名，防止消息被篡改。</p>
 *
 * <h3>实现说明</h3>
 * JDK 自带的 SunEC <b>不支持 secp256k1</b>，故本类统一通过 BouncyCastle provider 驱动签名引擎；
 * RAW 编码直接对应 BC 的 {@code ...withPLAIN-ECDSA} 算法名，无需手工 ASN.1 解析。
 *
 * @see EcdsaKeys
 * @see HashWithRsa
 */
public enum HashWithEcdsa {
    SHA256withECDSA("SHA256"),
    SHA384withECDSA("SHA384"),
    SHA512withECDSA("SHA512"),
    ;

    /**
     * BouncyCastle Provider，统一用于 ECDSA 签名引擎（含 JDK SunEC 不支持的 secp256k1 曲线）。
     * 以实例方式持有、按调用显式传入，避免 {@link java.security.Security#addProvider} 全局副作用。
     */
    private static final Provider BC = new BouncyCastleProvider();

    private final String hash;

    HashWithEcdsa(String hash) {
        this.hash = hash;
    }

    private static final Map<String, HashWithEcdsa> CACHE;

    static {
        Map<String, HashWithEcdsa> map = new HashMap<>(values().length);
        for (HashWithEcdsa alg : values()) {
            map.put(alg.name().toLowerCase(Locale.ROOT), alg);
        }
        CACHE = map;
    }

    /**
     * 按算法名称解析为对应的 {@link HashWithEcdsa} 枚举（忽略大小写）。
     * <p>
     * 匹配对象即<b>枚举常量名</b>（如 {@code "SHA256withECDSA"}），也就是 {@link #name()} 的返回值。
     * 例如 {@code HashWithEcdsa.of("sha256withecdsa")} 返回 {@link #SHA256withECDSA}。
     * <p>
     * 注意：{@link Encoding}（DER/RAW）是独立维度，{@code of} 仅解析哈希算法，与编码无关。
     * <p>
     * 未找到匹配项时返回 {@code null}，<b>不抛出异常</b>；入参为 {@code null} 同样返回 {@code null}。
     *
     * @param algorithm 算法名称（忽略大小写，即枚举常量名）
     * @return 匹配的枚举；不存在则返回 {@code null}
     */
    public static HashWithEcdsa of(String algorithm) {
        if (algorithm == null) {
            return null;
        }
        return CACHE.get(algorithm.toLowerCase(Locale.ROOT));
    }

    /**
     * 签名编码格式。
     */
    public enum Encoding {
        /** ASN.1 DER：{@code SEQUENCE{INTEGER r, INTEGER s}}，变长。JDK/openssl 经典默认格式。 */
        DER,
        /** IEEE P1363：{@code r} 与 {@code s} 按域长度零填充后拼接，定长（P-256 为 64 字节）。JWT ES256/384/512 采用。 */
        RAW,
    }

    /** 根据 hash 与编码拼接 BouncyCastle 算法名：DER 为 {@code SHA256withECDSA}，RAW 为 {@code SHA256withPLAIN-ECDSA}。 */
    private String algorithmName(Encoding encoding) {
        return encoding == Encoding.RAW ? hash + "withPLAIN-ECDSA" : hash + "withECDSA";
    }

    /**
     * 私钥签名（指定编码）。
     *
     * @param priKey    私钥
     * @param plaintext 原文字节（非 Base64）
     * @param encoding  签名编码
     * @return 签名字节
     * @throws IllegalArgumentException priKey / plaintext / encoding 为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      私钥非法
     * @throws SignatureException       签名引擎异常
     */
    public byte[] signature(PrivateKey priKey, byte[] plaintext, Encoding encoding)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(priKey, "priKey");
        Require.requireNonNull(plaintext, "plaintext");
        Require.requireNonNull(encoding, "encoding");
        Signature spi = Signature.getInstance(algorithmName(encoding), BC);
        spi.initSign(priKey);
        spi.update(plaintext);
        return spi.sign();
    }

    /**
     * 私钥签名（默认 {@link Encoding#DER}）。
     *
     * @param priKey    私钥
     * @param plaintext 原文字节（非 Base64）
     * @return 签名字节
     * @throws IllegalArgumentException priKey 或 plaintext 为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      私钥非法
     * @throws SignatureException       签名引擎异常
     */
    public byte[] signature(PrivateKey priKey, byte[] plaintext)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return signature(priKey, plaintext, Encoding.DER);
    }

    /**
     * 私钥签名（UTF-8 明文，指定编码）。
     *
     * @param priKey    私钥
     * @param plaintext 原文（UTF-8 字符串）
     * @param encoding  签名编码
     * @return 签名字节
     */
    public byte[] signature(PrivateKey priKey, String plaintext, Encoding encoding)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return signature(priKey, plaintext.getBytes(StandardCharsets.UTF_8), encoding);
    }

    /**
     * 私钥签名（UTF-8 明文，默认 {@link Encoding#DER}）。
     *
     * @param priKey    私钥
     * @param plaintext 原文（UTF-8 字符串）
     * @return 签名字节
     */
    public byte[] signature(PrivateKey priKey, String plaintext)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return signature(priKey, plaintext, Encoding.DER);
    }

    /**
     * 私钥签名便捷方法：UTF-8 明文 → Base64 签名（指定编码）。便于放入 Header / JSON 等场景传输。
     *
     * @param priKey    私钥
     * @param plaintext 原文（UTF-8 字符串）
     * @param encoding  签名编码
     * @return Base64 签名
     */
    public String signatureToBase64(PrivateKey priKey, String plaintext, Encoding encoding)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return Base64.toString(signature(priKey, plaintext, encoding));
    }

    /**
     * 私钥签名便捷方法：UTF-8 明文 → Base64 签名（默认 {@link Encoding#DER}）。
     *
     * @param priKey    私钥
     * @param plaintext 原文（UTF-8 字符串）
     * @return Base64 签名
     */
    public String signatureToBase64(PrivateKey priKey, String plaintext)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return signatureToBase64(priKey, plaintext, Encoding.DER);
    }

    /**
     * 验签（指定编码）。
     *
     * @param pubKey    公钥
     * @param plaintext 原文字节
     * @param signature 签名字节（非 Base64）
     * @param encoding  签名编码
     * @return true 表示验签通过（消息未被篡改且确为对应私钥所签）
     * @throws IllegalArgumentException 任一参数为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      公钥非法
     * @throws SignatureException       验签引擎异常
     */
    public boolean verify(PublicKey pubKey, byte[] plaintext, byte[] signature, Encoding encoding)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(pubKey, "pubKey");
        Require.requireNonNull(plaintext, "plaintext");
        Require.requireNonNull(signature, "signature");
        Require.requireNonNull(encoding, "encoding");
        Signature spi = Signature.getInstance(algorithmName(encoding), BC);
        spi.initVerify(pubKey);
        spi.update(plaintext);
        return spi.verify(signature);
    }

    /**
     * 验签（默认 {@link Encoding#DER}）。
     *
     * @param pubKey    公钥
     * @param plaintext 原文字节
     * @param signature 签名字节（非 Base64）
     * @return true 表示验签通过
     * @throws IllegalArgumentException 任一参数为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      公钥非法
     * @throws SignatureException       验签引擎异常
     */
    public boolean verify(PublicKey pubKey, byte[] plaintext, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return verify(pubKey, plaintext, signature, Encoding.DER);
    }

    /**
     * 验签（UTF-8 明文，指定编码）。
     *
     * @param pubKey    公钥
     * @param plaintext 原文（UTF-8 字符串）
     * @param signature 签名字节（非 Base64）
     * @param encoding  签名编码
     * @return true 表示验签通过
     */
    public boolean verify(PublicKey pubKey, String plaintext, byte[] signature, Encoding encoding)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return verify(pubKey, plaintext.getBytes(StandardCharsets.UTF_8), signature, encoding);
    }

    /**
     * 验签（UTF-8 明文，默认 {@link Encoding#DER}）。
     *
     * @param pubKey    公钥
     * @param plaintext 原文（UTF-8 字符串）
     * @param signature 签名字节（非 Base64）
     * @return true 表示验签通过
     */
    public boolean verify(PublicKey pubKey, String plaintext, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return verify(pubKey, plaintext, signature, Encoding.DER);
    }

    /**
     * 验签便捷方法：UTF-8 明文 + Base64 签名（指定编码）。
     * <p>
     * 输入的 Base64 签名会先去除所有空白与换行符，因此单行或多行均可正确解析。
     *
     * @param pubKey         公钥
     * @param plaintext      原文（UTF-8 字符串）
     * @param base64Signature Base64 签名（允许含换行/空白）
     * @param encoding       签名编码
     * @return true 表示验签通过
     * @throws IllegalArgumentException 任一参数为 null
     */
    public boolean verifyFromBase64(PublicKey pubKey, String plaintext, String base64Signature, Encoding encoding)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(base64Signature, "base64Signature");
        return verify(pubKey, plaintext, Base64.toByte(base64Signature), encoding);
    }

    /**
     * 验签便捷方法：UTF-8 明文 + Base64 签名（默认 {@link Encoding#DER}）。与 {@link #signatureToBase64} 配对。
     * <p>
     * 输入的 Base64 签名会先去除所有空白与换行符，因此单行或多行（例如直接复制带换行的签名）均可正确解析。
     *
     * @param pubKey         公钥
     * @param plaintext      原文（UTF-8 字符串）
     * @param base64Signature Base64 签名（允许含换行/空白）
     * @return true 表示验签通过
     * @throws IllegalArgumentException 任一参数为 null
     */
    public boolean verifyFromBase64(PublicKey pubKey, String plaintext, String base64Signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return verifyFromBase64(pubKey, plaintext, base64Signature, Encoding.DER);
    }
}
