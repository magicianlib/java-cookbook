package io.ituknown.crypto.ed25519;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Pem;
import io.ituknown.crypto.Require;
import io.ituknown.crypto.ecdsa.EcdsaKeys;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Ed25519 密钥加载、解析与生成工具类。本类产出 {@link PublicKey} / {@link PrivateKey}，
 * 拿到密钥后的签名/验签请使用 {@link Ed25519Utils}。
 * <p>
 * <b>密钥来源</b>（与 {@link EcdsaKeys} 对齐）：
 * <ul>
 *   <li><b>文件</b>（PEM 或 DER，自动识别）：{@link #loadPublicKey(Path)} / {@link #loadPrivateKey(Path)}，
 *       亦有 {@code InputStream} 重载（读取后内部关闭流）</li>
 *   <li><b>内存字符串</b>：
 *     <ul>
 *       <li>PEM 文本（含 {@code -----BEGIN-----} 头）：{@link #parsePublicKeyPem(String)} / {@link #parsePrivateKeyPem(String)}</li>
 *       <li>裸 Base64（DER 的 Base64，无 PEM 头，允许含换行）：{@link #parsePublicKeyBase64(String)} / {@link #parsePrivateKeyBase64(String)}</li>
 *     </ul>
 *   </li>
 *   <li><b>新生成</b>：{@link #generateKeyPair()} / {@link #generateBase64KeyPair()}</li>
 * </ul>
 * <p>
 * <b>支持的密钥编码</b>：私钥 {@code PKCS#8}，公钥 {@code X.509 SubjectPublicKeyInfo}，各有 PEM（文本）与 DER（二进制）两种形式。
 * 加载方法按内容<b>自动识别</b> PEM 与 DER：内容以 ASCII {@code -----BEGIN } 开头视为 PEM，否则视为 DER。
 * 与 openssl（{@code ssh-keygen -t ed25519} 产物需先行转换）等外部工具产出的标准编码密钥互操作。
 * <p>
 * <b>Ed25519 特点</b>（相对 {@link EcdsaKeys} 的 ECDSA）：参数集唯一——无曲线选择、无密钥长度选择，
 * 公钥恒为 32 字节、私钥（种子）恒为 32 字节，密钥更短、签名更快且恒定 64 字节，
 * 无需像 ECDSA 那样挑选哈希组合与曲线组合。
 * <p>
 * <b>实现说明</b>：JDK 自带的 SunEC 自 15 起完整支持 Ed25519 的生成/解析（不同于 ECDSA 需借助
 * BouncyCastle 才能支持部分曲线），故本类<b>零第三方依赖</b>、直接走 JDK 默认 provider，
 * 产出的密钥与签名与 openssl / BouncyCastle 等生态完全互操作。
 *
 * @see Ed25519Utils
 * @see EcdsaKeys
 */
public final class Ed25519Keys {

    private static final String ED25519 = "Ed25519";

    private Ed25519Keys() {
    }

    /**
     * 用 PKCS#8 DER 字节构建 Ed25519 私钥。
     *
     * @param derKey PKCS#8 编码的私钥字节
     * @return Ed25519 私钥
     * @throws GeneralSecurityException 密钥格式非法时抛出
     */
    static PrivateKey buildPrivateKey(byte[] derKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(ED25519).generatePrivate(new PKCS8EncodedKeySpec(derKey));
    }

    /**
     * 用 X.509 SubjectPublicKeyInfo DER 字节构建 Ed25519 公钥。
     *
     * @param derKey X.509 编码的公钥字节
     * @return Ed25519 公钥
     * @throws GeneralSecurityException 密钥格式非法时抛出
     */
    static PublicKey buildPublicKey(byte[] derKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(ED25519).generatePublic(new X509EncodedKeySpec(derKey));
    }

    /**
     * 从文件加载 Ed25519 私钥（PKCS#8）。自动识别 PEM/DER。
     *
     * @param path 私钥文件路径
     * @return Ed25519 私钥
     * @throws IOException              读取文件失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey loadPrivateKey(Path path) throws IOException, GeneralSecurityException {
        Require.requireNonNull(path, "path");
        return buildPrivateKey(Pem.extractPemContent(Files.readAllBytes(path)));
    }

    /**
     * 从输入流加载 Ed25519 私钥（PKCS#8）。自动识别 PEM/DER。
     * <p>
     * <b>注意：本方法会在读取后关闭传入的流，调用方无需自行 close。</b>
     *
     * @param in 私钥输入流
     * @return Ed25519 私钥
     * @throws IOException              读取流失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey loadPrivateKey(InputStream in) throws IOException, GeneralSecurityException {
        Require.requireNonNull(in, "in");
        return buildPrivateKey(Pem.extractPemContent(readAllBytes(in)));
    }

    /**
     * 从文件加载 Ed25519 公钥（X.509）。自动识别 PEM/DER。
     *
     * @param path 公钥文件路径
     * @return Ed25519 公钥
     * @throws IOException              读取文件失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey loadPublicKey(Path path) throws IOException, GeneralSecurityException {
        Require.requireNonNull(path, "path");
        return buildPublicKey(Pem.extractPemContent(Files.readAllBytes(path)));
    }

    /**
     * 从输入流加载 Ed25519 公钥（X.509）。自动识别 PEM/DER。
     * <p>
     * <b>注意：本方法会在读取后关闭传入的流，调用方无需自行 close。</b>
     *
     * @param in 公钥输入流
     * @return Ed25519 公钥
     * @throws IOException              读取流失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey loadPublicKey(InputStream in) throws IOException, GeneralSecurityException {
        Require.requireNonNull(in, "in");
        return buildPublicKey(Pem.extractPemContent(readAllBytes(in)));
    }

    /**
     * 解析内存中的 Ed25519 私钥 PEM 字符串（含 {@code -----BEGIN-----} 头）。
     *
     * @param pem PEM 文本
     * @return Ed25519 私钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey parsePrivateKeyPem(String pem) throws GeneralSecurityException {
        Require.requireNonNull(pem, "pem");
        return buildPrivateKey(Pem.extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * 解析内存中的 Ed25519 公钥 PEM 字符串（含 {@code -----BEGIN-----} 头）。
     *
     * @param pem PEM 文本
     * @return Ed25519 公钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey parsePublicKeyPem(String pem) throws GeneralSecurityException {
        Require.requireNonNull(pem, "pem");
        return buildPublicKey(Pem.extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * 解析裸 Base64 编码的 Ed25519 私钥（DER 的 Base64，无 PEM 头）。
     * <p>
     * 允许含换行/空白（{@link Base64#toByte(String)} 会先去除）。
     *
     * @param base64 裸 Base64 私钥
     * @return Ed25519 私钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey parsePrivateKeyBase64(String base64) throws GeneralSecurityException {
        Require.requireNonNull(base64, "base64");
        return buildPrivateKey(Base64.toByte(base64));
    }

    /**
     * 解析裸 Base64 编码的 Ed25519 公钥（DER 的 Base64，无 PEM 头）。
     * <p>
     * 允许含换行/空白（{@link Base64#toByte(String)} 会先去除）。
     *
     * @param base64 裸 Base64 公钥
     * @return Ed25519 公钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey parsePublicKeyBase64(String base64) throws GeneralSecurityException {
        Require.requireNonNull(base64, "base64");
        return buildPublicKey(Base64.toByte(base64));
    }

    /**
     * 生成 Ed25519 密钥对。Ed25519 参数集唯一，无需（也不存在）曲线等参数选择。
     *
     * @return Ed25519 密钥对
     * @throws NoSuchAlgorithmException 当前环境不支持 Ed25519
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance(ED25519).generateKeyPair();
    }

    /**
     * 生成 Ed25519 密钥对，返回 Base64 编码的公私钥。
     *
     * @return Base64 密钥对
     * @throws NoSuchAlgorithmException 当前环境不支持 Ed25519
     */
    public static Ed25519KeyPair generateBase64KeyPair() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();
        return new Ed25519KeyPair(
                Base64.toString(keyPair.getPrivate().getEncoded()),
                Base64.toString(keyPair.getPublic().getEncoded()));
    }

    /**
     * Base64 编码的 Ed25519 公私钥对。
     *
     * @param privateKeyBase64 PKCS#8 私钥的 Base64
     * @param publicKeyBase64  X.509 公钥的 Base64
     */
    public record Ed25519KeyPair(String privateKeyBase64, String publicKeyBase64) {
    }

    /** 把输入流排空为字节数组，并在读取后关闭流。 */
    private static byte[] readAllBytes(InputStream in) throws IOException {
        try (in) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
