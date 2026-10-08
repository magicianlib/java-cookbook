package io.ituknown.crypto.ed25519;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Require;
import io.ituknown.crypto.ecdsa.HashWithEcdsa;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Ed25519 数据签名与验签工具类：用私钥对消息签名、用公钥验签，用于防篡改与身份认证。
 * <p>
 * <b>密钥</b>请通过 {@link Ed25519Keys} 获取（PEM/DER 文件、PEM/裸 Base64 字符串、或生成密钥对），
 * 再传入本类的签名/验签方法。
 * <p>
 * 与 {@link HashWithEcdsa} 相比，Ed25519 算法完全定型：无哈希组合选择、签名恒为 64 字节且只有一种
 * 编码（不存在 ECDSA 的 DER/RAW 之分），故本类为静态方法工具类而非算法枚举；
 * 签名过程可确定复现（同一私钥对同一消息签名结果恒定），而 ECDSA 每次签名含随机数、结果不同。
 * <p>
 * 对应 JWT 的 {@code EdDSA}（Ed25519）算法即本类的签名形态。
 * <p>
 * <b>签名过程（防篡改）</b>：A 用自己的私钥对消息加签，把签名和消息一起传递给 B；
 * B 收到后通过 A 的公钥验签，验签成功即证明消息是 A 发送的且未被篡改。
 * 只有 A 私钥签名的消息才能验签成功，即使知道消息内容也无法伪造签名。
 * <p>
 * <b>实现说明</b>：JDK 自带的 SunEC 自 15 起完整支持 Ed25519 签名引擎，本类零第三方依赖；
 * 签名与 openssl / BouncyCastle 等生态互操作。
 *
 * @see Ed25519Keys
 * @see HashWithEcdsa
 */
public final class Ed25519Utils {

    private static final String ED25519 = "Ed25519";

    private Ed25519Utils() {
    }

    /**
     * 私钥签名。
     *
     * @param priKey    私钥
     * @param plaintext 原文字节（非 Base64）
     * @return 签名字节（恒 64 字节）
     * @throws IllegalArgumentException priKey 或 plaintext 为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      私钥非法
     * @throws SignatureException       签名引擎异常
     */
    public static byte[] signature(PrivateKey priKey, byte[] plaintext)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(priKey, "priKey");
        Require.requireNonNull(plaintext, "plaintext");
        Signature spi = Signature.getInstance(ED25519);
        spi.initSign(priKey);
        spi.update(plaintext);
        return spi.sign();
    }

    /**
     * 私钥签名（UTF-8 明文）。
     *
     * @param priKey    私钥
     * @param plaintext 原文（UTF-8 字符串）
     * @return 签名字节（恒 64 字节）
     * @throws IllegalArgumentException priKey 或 plaintext 为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      私钥非法
     * @throws SignatureException       签名引擎异常
     */
    public static byte[] signature(PrivateKey priKey, String plaintext)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(priKey, "priKey");
        Require.requireNonNull(plaintext, "plaintext");
        return signature(priKey, plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 私钥签名便捷方法：UTF-8 明文 → Base64 签名。便于放入 Header / JSON 等场景传输。
     *
     * @param priKey    私钥
     * @param plaintext 原文（UTF-8 字符串）
     * @return Base64 签名
     * @throws IllegalArgumentException priKey 或 plaintext 为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      私钥非法
     * @throws SignatureException       签名引擎异常
     */
    public static String signatureToBase64(PrivateKey priKey, String plaintext)
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
    public static boolean verify(PublicKey pubKey, byte[] plaintext, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(pubKey, "pubKey");
        Require.requireNonNull(plaintext, "plaintext");
        Require.requireNonNull(signature, "signature");
        Signature spi = Signature.getInstance(ED25519);
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
     * @throws IllegalArgumentException 任一参数为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      公钥非法
     * @throws SignatureException       验签引擎异常
     */
    public static boolean verify(PublicKey pubKey, String plaintext, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(pubKey, "pubKey");
        Require.requireNonNull(plaintext, "plaintext");
        return verify(pubKey, plaintext.getBytes(StandardCharsets.UTF_8), signature);
    }

    /**
     * 验签便捷方法：UTF-8 明文 + Base64 签名。与 {@link #signatureToBase64} 配对。
     * <p>
     * 输入的 Base64 签名会先去除所有空白与换行符，因此单行或多行（例如直接复制带换行的签名）均可正确解析。
     *
     * @param pubKey          公钥
     * @param plaintext       原文（UTF-8 字符串）
     * @param base64Signature Base64 签名（允许含换行/空白）
     * @return true 表示验签通过
     * @throws IllegalArgumentException 任一参数为 null
     * @throws NoSuchAlgorithmException 当前环境不支持该算法
     * @throws InvalidKeyException      公钥非法
     * @throws SignatureException       验签引擎异常
     */
    public static boolean verifyFromBase64(PublicKey pubKey, String plaintext, String base64Signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Require.requireNonNull(base64Signature, "base64Signature");
        return verify(pubKey, plaintext, Base64.toByte(base64Signature));
    }
}
