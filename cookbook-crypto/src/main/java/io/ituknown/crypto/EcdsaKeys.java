package io.ituknown.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

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
import java.security.Provider;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * ECDSA（椭圆曲线）密钥加载、解析与生成工具类。本类产出 {@link PublicKey} / {@link PrivateKey}，
 * 拿到密钥后的签名/验签请使用 {@link HashWithEcdsa}。
 * <p>
 * <b>密钥来源</b>（与 {@link RsaKeys} 对齐）：
 * <ul>
 *   <li><b>文件</b>（PEM 或 DER，自动识别）：{@link #loadPublicKey(Path)} / {@link #loadPrivateKey(Path)}，
 *       亦有 {@code InputStream} 重载（读取后内部关闭流）</li>
 *   <li><b>内存字符串</b>：
 *     <ul>
 *       <li>PEM 文本（含 {@code -----BEGIN-----} 头）：{@link #parsePublicKeyPem(String)} / {@link #parsePrivateKeyPem(String)}</li>
 *       <li>裸 Base64（DER 的 Base64，无 PEM 头，允许含换行）：{@link #parsePublicKeyBase64(String)} / {@link #parsePrivateKeyBase64(String)}</li>
 *     </ul>
 *   </li>
 *   <li><b>新生成</b>：{@link #generateKeyPair()} / {@link #generateBase64KeyPair()}（默认 {@link Curve#P256}）</li>
 * </ul>
 * <p>
 * <b>支持的密钥编码</b>：私钥 {@code PKCS#8}，公钥 {@code X.509 SubjectPublicKeyInfo}，各有 PEM（文本）与 DER（二进制）两种形式。
 * 加载方法按内容<b>自动识别</b> PEM 与 DER：内容以 ASCII {@code -----BEGIN } 开头视为 PEM，否则视为 DER。
 * <p>
 * <b>曲线</b>：见 {@link Curve}。ECDSA 的密钥强度由曲线决定（不同于 RSA 的 keySize），
 * 所列曲线本身均为安全曲线，故无类似 {@code RsaKeys#MIN_KEY_SIZE} 的下限校验。
 * <p>
 * <b>实现说明</b>：JDK 自带的 SunEC <b>不支持 secp256k1</b>，故本类统一通过 BouncyCastle provider
 * 完成密钥生成与解析——以实例方式持有、按调用显式传入，避免 {@link java.security.Security#addProvider} 全局副作用。
 *
 * @see HashWithEcdsa
 * @see RsaKeys
 */
public final class EcdsaKeys {

    /**
     * BouncyCastle Provider，统一用于 EC 密钥生成与解析（含 JDK SunEC 不支持的 secp256k1）。
     * 以实例方式持有、按调用显式传入，避免 {@link java.security.Security#addProvider} 全局副作用。
     */
    private static final Provider BC = new BouncyCastleProvider();

    private static final String EC = "EC";

    private EcdsaKeys() {
    }

    /** 默认曲线（{@link Curve#P256}）。 */
    public static final Curve DEFAULT_CURVE = Curve.P256;

    /**
     * ECDSA 支持的椭圆曲线，每个枚举值对应一条标准命名曲线。
     */
    public enum Curve {
        /** secp256r1 / prime256v1 / NIST P-256（对应 JWT ES256）。 */
        P256("secp256r1"),
        /** secp384r1 / NIST P-384（对应 JWT ES384）。 */
        P384("secp384r1"),
        /** secp521r1 / NIST P-521（对应 JWT ES512）。 */
        P521("secp521r1"),
        /** secp256k1（比特币 / 以太坊等使用的曲线）。 */
        SECP256K1("secp256k1"),
        ;

        private final String name;

        Curve(String name) {
            this.name = name;
        }

        /**
         * @return 标准曲线名（用于 {@link ECGenParameterSpec}）
         */
        public String getName() {
            return name;
        }
    }

    /**
     * 用 PKCS#8 DER 字节构建 EC 私钥。
     *
     * @param derKey PKCS#8 编码的私钥字节
     * @return EC 私钥
     * @throws GeneralSecurityException 密钥格式非法时抛出
     */
    static PrivateKey buildPrivateKey(byte[] derKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(EC, BC).generatePrivate(new PKCS8EncodedKeySpec(derKey));
    }

    /**
     * 用 X.509 SubjectPublicKeyInfo DER 字节构建 EC 公钥。
     *
     * @param derKey X.509 编码的公钥字节
     * @return EC 公钥
     * @throws GeneralSecurityException 密钥格式非法时抛出
     */
    static PublicKey buildPublicKey(byte[] derKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(EC, BC).generatePublic(new X509EncodedKeySpec(derKey));
    }

    /**
     * 从文件加载 EC 私钥（PKCS#8）。自动识别 PEM/DER。
     *
     * @param path 私钥文件路径
     * @return EC 私钥
     * @throws IOException              读取文件失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey loadPrivateKey(Path path) throws IOException, GeneralSecurityException {
        Require.requireNonNull(path, "path");
        return buildPrivateKey(Pem.extractPemContent(Files.readAllBytes(path)));
    }

    /**
     * 从输入流加载 EC 私钥（PKCS#8）。自动识别 PEM/DER。
     * <p>
     * <b>注意：本方法会在读取后关闭传入的流，调用方无需自行 close。</b>
     *
     * @param in 私钥输入流
     * @return EC 私钥
     * @throws IOException              读取流失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey loadPrivateKey(InputStream in) throws IOException, GeneralSecurityException {
        Require.requireNonNull(in, "in");
        return buildPrivateKey(Pem.extractPemContent(readAllBytes(in)));
    }

    /**
     * 从文件加载 EC 公钥（X.509）。自动识别 PEM/DER。
     *
     * @param path 公钥文件路径
     * @return EC 公钥
     * @throws IOException              读取文件失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey loadPublicKey(Path path) throws IOException, GeneralSecurityException {
        Require.requireNonNull(path, "path");
        return buildPublicKey(Pem.extractPemContent(Files.readAllBytes(path)));
    }

    /**
     * 从输入流加载 EC 公钥（X.509）。自动识别 PEM/DER。
     * <p>
     * <b>注意：本方法会在读取后关闭传入的流，调用方无需自行 close。</b>
     *
     * @param in 公钥输入流
     * @return EC 公钥
     * @throws IOException              读取流失败
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey loadPublicKey(InputStream in) throws IOException, GeneralSecurityException {
        Require.requireNonNull(in, "in");
        return buildPublicKey(Pem.extractPemContent(readAllBytes(in)));
    }

    /**
     * 解析内存中的 EC 私钥 PEM 字符串（含 {@code -----BEGIN-----} 头）。
     *
     * @param pem PEM 文本
     * @return EC 私钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey parsePrivateKeyPem(String pem) throws GeneralSecurityException {
        Require.requireNonNull(pem, "pem");
        return buildPrivateKey(Pem.extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * 解析内存中的 EC 公钥 PEM 字符串（含 {@code -----BEGIN-----} 头）。
     *
     * @param pem PEM 文本
     * @return EC 公钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey parsePublicKeyPem(String pem) throws GeneralSecurityException {
        Require.requireNonNull(pem, "pem");
        return buildPublicKey(Pem.extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * 解析裸 Base64 编码的 EC 私钥（DER 的 Base64，无 PEM 头）。
     * <p>
     * 允许含换行/空白（{@link Base64#toByte(String)} 会先去除）。
     *
     * @param base64 裸 Base64 私钥
     * @return EC 私钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PrivateKey parsePrivateKeyBase64(String base64) throws GeneralSecurityException {
        Require.requireNonNull(base64, "base64");
        return buildPrivateKey(Base64.toByte(base64));
    }

    /**
     * 解析裸 Base64 编码的 EC 公钥（DER 的 Base64，无 PEM 头）。
     * <p>
     * 允许含换行/空白（{@link Base64#toByte(String)} 会先去除）。
     *
     * @param base64 裸 Base64 公钥
     * @return EC 公钥
     * @throws GeneralSecurityException 密钥格式非法
     */
    public static PublicKey parsePublicKeyBase64(String base64) throws GeneralSecurityException {
        Require.requireNonNull(base64, "base64");
        return buildPublicKey(Base64.toByte(base64));
    }

    /**
     * 生成默认曲线（{@link Curve#P256}）的 EC 密钥对。
     *
     * @return EC 密钥对
     * @throws NoSuchAlgorithmException 当前环境不支持 EC 算法
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPair(DEFAULT_CURVE);
    }

    /**
     * 生成指定曲线的 EC 密钥对。
     *
     * @param curve 椭圆曲线
     * @return EC 密钥对
     * @throws IllegalArgumentException curve 为 null
     * @throws NoSuchAlgorithmException 不支持该曲线
     */
    public static KeyPair generateKeyPair(Curve curve) throws NoSuchAlgorithmException {
        Require.requireNonNull(curve, "curve");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(EC, BC);
            generator.initialize(new ECGenParameterSpec(curve.getName()));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new NoSuchAlgorithmException("Failed to generate EC key pair for curve " + curve.getName(), e);
        }
    }

    /**
     * 生成默认曲线（{@link Curve#P256}）的 EC 密钥对，返回 Base64 编码的公私钥。
     *
     * @return Base64 密钥对
     * @throws NoSuchAlgorithmException 当前环境不支持 EC 算法
     */
    public static EcdsaKeyPair generateBase64KeyPair() throws NoSuchAlgorithmException {
        return generateBase64KeyPair(DEFAULT_CURVE);
    }

    /**
     * 生成指定曲线的 EC 密钥对，返回 Base64 编码的公私钥。
     *
     * @param curve 椭圆曲线
     * @return Base64 密钥对
     * @throws IllegalArgumentException curve 为 null
     * @throws NoSuchAlgorithmException 不支持该曲线
     */
    public static EcdsaKeyPair generateBase64KeyPair(Curve curve) throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair(curve);
        return new EcdsaKeyPair(
                Base64.toString(keyPair.getPrivate().getEncoded()),
                Base64.toString(keyPair.getPublic().getEncoded()));
    }

    /**
     * Base64 编码的 EC 公私钥对。
     *
     * @param privateKeyBase64 PKCS#8 私钥的 Base64
     * @param publicKeyBase64  X.509 公钥的 Base64
     */
    public record EcdsaKeyPair(String privateKeyBase64, String publicKeyBase64) {
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
