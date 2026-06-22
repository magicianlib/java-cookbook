package io.ituknown.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * 常见的 HashWithRsa 组合算法，用于数据签名与验签。
 * <p>
 * HashWithRsa = RSA + 哈希函数：用私钥对消息摘要签名、用公钥验签，用于防篡改与身份认证。
 * <b>密钥</b>请通过 {@link RsaKeys} 获取（PEM/DER 文件、PEM/裸 Base64 字符串、或生成密钥对），
 * 再传入本类的 {@link #signature} / {@link #verify} 方法。
 * <p>
 * 加密与签名都是为了安全，但侧重不同：加密（{@link RsaUtils}，公钥加密/私钥解密）用于<b>防泄露</b>；
 * 签名（本类，私钥签名/公钥验签）用于<b>防篡改与身份认证</b>。
 *
 * <h3>加密过程（防泄露）</h3>
 * <ul>
 * <li>A 生成一对密钥（公钥和私钥）。私钥不公开，A 自己保留。公钥为公开的，任何人可以获取。</li>
 * <li>A 传递自己的公钥给 B，B 使用 A 的公钥对消息进行加密。</li>
 * <li>A 接收到 B 加密的消息，利用 A 自己的私钥对消息进行解密。</li>
 * </ul>
 * <p>整个过程中，只用 A 的私钥才能对消息进行解密，防止消息被泄露。</p>
 *
 * <h3>签名过程（防篡改）</h3>
 * <ul>
 * <li>A 生成一对密钥（公钥和私钥）。私钥不公开，A 自己保留。公钥为公开的，任何人可以获取。</li>
 * <li>A 用自己的私钥对消息进行加签，形成签名，并将签名和消息本身一起传递给 B。</li>
 * <li>B 收到消息后，通过 A 的公钥进行验签。如果验签成功，则证明消息是 A 发送的。</li>
 * </ul>
 * <p>整个过程，只有使用 A 私钥签名的消息才能被验签成功。即使知道了消息内容，也无法伪造签名，防止消息被篡改。</p>
 *
 * @see RsaKeys
 * @see RsaUtils
 */
public enum HashWithRsa {
    /** 推荐。 */
    SHA256withRSA,
    /** 推荐。 */
    SHA384withRSA,
    /** 推荐。 */
    SHA512withRSA,
    /**
     * @deprecated MD5 已被密码学攻破，仅保留用于兼容已有数据；新场景请使用 {@link #SHA256withRSA} 及以上。
     */
    @Deprecated
    MD5withRSA,
    /**
     * @deprecated SHA-1 已被密码学攻破，仅保留用于兼容已有数据；新场景请使用 {@link #SHA256withRSA} 及以上。
     */
    @Deprecated
    SHA1withRSA,
    ;

    /**
     * 私钥签名。
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
        Require.requireNonNull(priKey, "priKey");
        Require.requireNonNull(plaintext, "plaintext");
        Signature spi = Signature.getInstance(name());
        spi.initSign(priKey);
        spi.update(plaintext);
        return spi.sign();
    }

    /**
     * 私钥签名（UTF-8 明文）。
     *
     * @param priKey    私钥
     * @param plaintext 原文（UTF-8 字符串）
     * @return 签名字节
     */
    public byte[] signature(PrivateKey priKey, String plaintext)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return signature(priKey, plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 私钥签名便捷方法：UTF-8 明文 → Base64 签名（最常用，便于放入 Header / JSON 等场景传输）。
     *
     * @param priKey    私钥
     * @param plaintext 原文（UTF-8 字符串）
     * @return Base64 签名
     */
    public String signatureToBase64(PrivateKey priKey, String plaintext)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return Base64.toString(signature(priKey, plaintext));
    }

    /**
     * 验签。
     *
     * @param pubKey    公钥
     * @param plaintext 原文字节
     * @param signature 签名字节（非 Base64）
     * @return true 表示验签通过（消息未被篡改且确为对应私钥所签）
     * @throws IllegalArgumentException 任一参数为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      公钥非法
     * @throws SignatureException       验签引擎异常
     */
    public boolean verify(PublicKey pubKey, byte[] plaintext, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(pubKey, "pubKey");
        Require.requireNonNull(plaintext, "plaintext");
        Require.requireNonNull(signature, "signature");
        Signature spi = Signature.getInstance(name());
        spi.initVerify(pubKey);
        spi.update(plaintext);
        return spi.verify(signature);
    }

    /**
     * 验签（UTF-8 明文）。
     *
     * @param pubKey    公钥
     * @param plaintext 原文（UTF-8 字符串）
     * @param signature 签名字节（非 Base64）
     * @return true 表示验签通过
     */
    public boolean verify(PublicKey pubKey, String plaintext, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return verify(pubKey, plaintext.getBytes(StandardCharsets.UTF_8), signature);
    }

    /**
     * 验签便捷方法：UTF-8 明文 + Base64 签名。与 {@link #signatureToBase64} 配对，是最常用的往返组合。
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
        Require.requireNonNull(base64Signature, "base64Signature");
        return verify(pubKey, plaintext, Base64.toByte(base64Signature));
    }
}
