package io.ituknown.crypto;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 该类提供常见的 HashWithRsa 组合算法，用于提供数据签名和验签。
 * <p>
 * HashWithRsa 指的是使用 RSA 加密算法结合哈希函数进行数字签名或数据加密。
 * RSA（Rivest–Shamir–Adleman）是一种非对称加密算法，而哈希函数则用于生成消息
 * 摘要（即签名）。
 *
 * <h3>加密和签名的区别</h3>
 * 加密和签名都是为了安全性考虑，但有所不同。加密是为了防止信息被泄露，签名是为了防止信息被篡改。
 *
 * <h3>加密过程</h3>
 * <ul>
 * <li>A 生成一对密钥（公钥和私钥）。私钥不公开，A 自己保留。公钥为公开的，任何人可以获取。</li>
 * <li>A 传递自己的公钥给 B，B 使用 A 的公钥对消息进行加密。</li>
 * <li>A 接收到 B 加密的消息，利用 A 自己的私钥对消息进行解密。</li>
 * </ul>
 * <p>整个过程中，只用A的私钥才能对消息进行解密，防止消息被泄露。</p>
 *
 * <h3>签名过程</h3>
 * <ul>
 * <li>A 生成一对密钥（公钥和私钥）。私钥不公开，A 自己保留。公钥为公开的，任何人可以获取。</li>
 * <li>A 用自己的私钥对消息进行加签，形成签名，并将签名和消息本身一起传递给 B。</li>
 * <li>B 收到消息后，通过 A 的公钥进行验签。如果验签成功，则证明消息是 A 发送的。</li>
 * </ul>
 * <p>整个过程，只有使用A私钥签名的消息才能被验签成功。即使知道了消息内容，也无法伪造签名，防止消息被篡改。
 */
public enum HashWithRsa {
    MD5withRSA,
    SHA1withRSA,
    SHA256withRSA,
    SHA384withRSA,
    SHA512withRSA,
    ;

    /**
     * 私钥签名
     *
     * @param priKey    私钥
     * @param plaintext 原文（非 base64）
     */
    public byte[] signature(PrivateKey priKey, byte[] plaintext) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // 签名
        Signature spi = Signature.getInstance(this.name());
        spi.initSign(priKey);
        spi.update(plaintext);
        return spi.sign();
    }

    public byte[] signature(byte[] priKey, byte[] plaintext) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        // 私钥证书
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(priKey);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        return signature(privateKey, plaintext);
    }

    public byte[] signature(PrivateKey priKey, String plaintext) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return signature(priKey, plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] signature(String base64PriKey, byte[] plaintext) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        return signature(Base64.toByte(base64PriKey), plaintext);
    }

    public byte[] signature(String base64PriKey, String plaintext) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        return signature(Base64.toByte(base64PriKey), plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public <R> R signature(PrivateKey priKey, String plaintext, Production<R> production) throws Exception {
        return production.apply(signature(priKey, plaintext));
    }

    public <R> R signature(byte[] priKey, String plaintext, Production<R> production) throws Exception {
        return production.apply(signature(priKey, plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    public <R> R signature(String base64PriKey, String plaintext, Production<R> production) throws Exception {
        return production.apply(signature(base64PriKey, plaintext.getBytes()));
    }

    /**
     * 验签
     *
     * @param pubKey    公钥
     * @param plaintext 原文
     * @param signature 签名（非 base64）
     */
    public boolean verify(PublicKey pubKey, byte[] plaintext, byte[] signature) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature spi = Signature.getInstance(this.name());
        spi.initVerify(pubKey);
        spi.update(plaintext);
        return spi.verify(signature);
    }

    public boolean verify(byte[] pubKey, byte[] plaintext, byte[] signature) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        // 公钥证书
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKey);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

        return verify(publicKey, plaintext, signature);
    }

    public boolean verify(String base64PubKey, byte[] plaintext, byte[] signature) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        return verify(Base64.toByte(base64PubKey), plaintext, signature);
    }

    public boolean verify(String base64PubKey, String plaintext, byte[] signature) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        return verify(Base64.toByte(base64PubKey), plaintext.getBytes(StandardCharsets.UTF_8), signature);
    }

    public boolean verify(String base64PubKey, String plaintext, String signature) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        return verify(Base64.toByte(base64PubKey), plaintext.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }
}