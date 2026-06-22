package io.ituknown.crypto;

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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA 密钥加载、解析与生成工具类。本类产出 {@link PublicKey} / {@link PrivateKey}，
 * 拿到密钥后的加解密请使用 {@link RsaUtils}。
 * <p>
 * <b>密钥来源</b>：
 * <ul>
 *   <li><b>文件</b>（PEM 或 DER，自动识别）：{@link #loadPublicKey(Path)} / {@link #loadPrivateKey(Path)}，
 *       亦有 {@code InputStream} 重载（读取后内部关闭流）</li>
 *   <li><b>内存字符串</b>：
 *     <ul>
 *       <li>PEM 文本（含 {@code -----BEGIN-----} 头）：{@link #parsePublicKeyPem(String)} / {@link #parsePrivateKeyPem(String)}</li>
 *       <li>裸 Base64（DER 的 Base64，无 PEM 头，允许含换行）：{@link #parsePublicKeyBase64(String)} / {@link #parsePrivateKeyBase64(String)}</li>
 *     </ul>
 *   </li>
 *   <li><b>新生成</b>：{@link #generateKeyPair()} / {@link #generateBase64KeyPair()}（强制 {@code >= 2048} 位）</li>
 * </ul>
 * <p>
 * <b>支持的密钥编码</b>：私钥 {@code PKCS#8}，公钥 {@code X.509 SubjectPublicKeyInfo}，各有 PEM（文本）与 DER（二进制）两种形式。
 * 加载方法按内容<b>自动识别</b> PEM 与 DER：内容以 ASCII {@code -----BEGIN } 开头视为 PEM，否则视为 DER。
 * <p>
 * <b>不支持 PKCS#1</b>（{@code -----BEGIN RSA PRIVATE KEY-----}），该格式需要 BouncyCastle，本期不在范围内。
 */
public final class RsaKeys {

    private RsaKeys() {
    }

    /** PEM 头部前缀（ASCII）。 */
    static final String PEM_BEGIN = "-----BEGIN ";

    /**
     * 判断给定内容是否为 PEM 文本。
     *
     * @param content 原始字节
     * @return 内容以 {@code -----BEGIN } 开头时返回 true
     */
    static boolean isPem(byte[] content) {
        byte[] marker = PEM_BEGIN.getBytes(StandardCharsets.US_ASCII);
        if (content.length < marker.length) {
            return false;
        }
        for (int i = 0; i < marker.length; i++) {
            if (content[i] != marker[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从 PEM 文本中提取 DER 字节：去除 {@code -----BEGIN-----} / {@code -----END-----} 行，
     * 拼接其余行后 Base64 解码。非 PEM 内容原样返回。
     *
     * @param content 原始字节（可能为 PEM 或 DER）
     * @return DER 字节
     */
    static byte[] extractPemContent(byte[] content) {
        if (!isPem(content)) {
            return content;
        }
        String text = new String(content, StandardCharsets.US_ASCII);
        StringBuilder base64 = new StringBuilder();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-----") || trimmed.isEmpty()) {
                continue; // 跳过头尾行与空行
            }
            base64.append(trimmed);
        }
        return Base64.toByte(base64.toString());
    }

    private static final String RSA = "RSA";

    /**
     * 用 PKCS#8 DER 字节构建 RSA 私钥。
     *
     * @param derKey PKCS#8 编码的私钥字节
     * @return RSA 私钥
     * @throws GeneralSecurityException 密钥格式非法时抛出
     */
    static PrivateKey buildPrivateKey(byte[] derKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(derKey));
    }

    /**
     * 用 X.509 SubjectPublicKeyInfo DER 字节构建 RSA 公钥。
     *
     * @param derKey X.509 编码的公钥字节
     * @return RSA 公钥
     * @throws GeneralSecurityException 密钥格式非法时抛出
     */
    static PublicKey buildPublicKey(byte[] derKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(derKey));
    }

    /**
     * 从文件加载 RSA 私钥（PKCS#8）。自动识别 PEM/DER。
     *
     * @param path 私钥文件路径
     * @return RSA 私钥
     * @throws IOException             读取文件失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey loadPrivateKey(Path path) throws IOException, GeneralSecurityException {
        Require.requireNonNull(path, "path");
        return buildPrivateKey(extractPemContent(Files.readAllBytes(path)));
    }

    /**
     * 从输入流加载 RSA 私钥（PKCS#8）。自动识别 PEM/DER。
     * <p>
     * <b>注意：本方法会在读取后关闭传入的流，调用方无需自行 close。</b>
     *
     * @param in 私钥输入流
     * @return RSA 私钥
     * @throws IOException             读取流失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey loadPrivateKey(InputStream in) throws IOException, GeneralSecurityException {
        Require.requireNonNull(in, "in");
        return buildPrivateKey(extractPemContent(readAllBytes(in)));
    }

    /**
     * 从文件加载 RSA 公钥（X.509）。自动识别 PEM/DER。
     *
     * @param path 公钥文件路径
     * @return RSA 公钥
     * @throws IOException             读取文件失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey loadPublicKey(Path path) throws IOException, GeneralSecurityException {
        Require.requireNonNull(path, "path");
        return buildPublicKey(extractPemContent(Files.readAllBytes(path)));
    }

    /**
     * 从输入流加载 RSA 公钥（X.509）。自动识别 PEM/DER。
     * <p>
     * <b>注意：本方法会在读取后关闭传入的流，调用方无需自行 close。</b>
     *
     * @param in 公钥输入流
     * @return RSA 公钥
     * @throws IOException             读取流失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey loadPublicKey(InputStream in) throws IOException, GeneralSecurityException {
        Require.requireNonNull(in, "in");
        return buildPublicKey(extractPemContent(readAllBytes(in)));
    }

    /**
     * 解析内存中的 RSA 私钥 PEM 字符串（含 {@code -----BEGIN-----} 头）。
     *
     * @param pem PEM 文本
     * @return RSA 私钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey parsePrivateKeyPem(String pem) throws GeneralSecurityException {
        Require.requireNonNull(pem, "pem");
        return buildPrivateKey(extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * 解析内存中的 RSA 公钥 PEM 字符串（含 {@code -----BEGIN-----} 头）。
     *
     * @param pem PEM 文本
     * @return RSA 公钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey parsePublicKeyPem(String pem) throws GeneralSecurityException {
        Require.requireNonNull(pem, "pem");
        return buildPublicKey(extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * 解析裸 Base64 编码的 RSA 私钥（DER 的 Base64，无 PEM 头）。兼容旧 API 迁移。
     * <p>
     * 允许含换行/空白（{@link Base64#toByte(String)} 会先去除）。
     *
     * @param base64 裸 Base64 私钥
     * @return RSA 私钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey parsePrivateKeyBase64(String base64) throws GeneralSecurityException {
        Require.requireNonNull(base64, "base64");
        return buildPrivateKey(Base64.toByte(base64));
    }

    /**
     * 解析裸 Base64 编码的 RSA 公钥（DER 的 Base64，无 PEM 头）。兼容旧 API 迁移。
     * <p>
     * 允许含换行/空白（{@link Base64#toByte(String)} 会先去除）。
     *
     * @param base64 裸 Base64 公钥
     * @return RSA 公钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey parsePublicKeyBase64(String base64) throws GeneralSecurityException {
        Require.requireNonNull(base64, "base64");
        return buildPublicKey(Base64.toByte(base64));
    }

    /** 默认密钥长度（位）。 */
    public static final int DEFAULT_KEY_SIZE = 2048;

    /** 安全密钥长度下限（位）。低于此值视为弱密钥并拒绝。 */
    public static final int MIN_KEY_SIZE = 2048;

    /**
     * 生成默认长度（2048 位）的 RSA 密钥对。
     *
     * @return RSA 密钥对
     * @throws NoSuchAlgorithmException 不支持 RSA 算法
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPair(DEFAULT_KEY_SIZE);
    }

    /**
     * 生成指定长度的 RSA 密钥对。
     *
     * @param keySize 密钥长度（位），必须 {@code >= 2048}
     * @return RSA 密钥对
     * @throws IllegalArgumentException keySize 小于 2048
     * @throws NoSuchAlgorithmException 不支持 RSA 算法
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        if (keySize < MIN_KEY_SIZE) {
            throw new IllegalArgumentException("keySize must be >= " + MIN_KEY_SIZE + ", but was " + keySize);
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }

    /**
     * 生成默认长度（2048 位）的 RSA 密钥对，返回 Base64 编码的公私钥。
     *
     * @return Base64 密钥对
     * @throws NoSuchAlgorithmException 不支持 RSA 算法
     */
    public static RsaKeyPair generateBase64KeyPair() throws NoSuchAlgorithmException {
        return generateBase64KeyPair(DEFAULT_KEY_SIZE);
    }

    /**
     * 生成指定长度的 RSA 密钥对，返回 Base64 编码的公私钥。
     *
     * @param keySize 密钥长度（位），必须 {@code >= 2048}
     * @return Base64 密钥对
     * @throws IllegalArgumentException keySize 小于 2048
     * @throws NoSuchAlgorithmException 不支持 RSA 算法
     */
    public static RsaKeyPair generateBase64KeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair(keySize);
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey pri = (RSAPrivateKey) keyPair.getPrivate();
        return new RsaKeyPair(Base64.toString(pri.getEncoded()), Base64.toString(pub.getEncoded()));
    }

    /**
     * Base64 编码的 RSA 公私钥对。
     *
     * @param privateKeyBase64 PKCS#8 私钥的 Base64
     * @param publicKeyBase64  X.509 公钥的 Base64
     */
    public record RsaKeyPair(String privateKeyBase64, String publicKeyBase64) {
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
