package io.ituknown.crypto.aes;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Hex;
import io.ituknown.crypto.Require;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * AES 构建器基类：自类型 fluent，持有密钥/IV/标签等状态并提供全部终结方法。
 * 子类按家族暴露各自该有的配置项，实现编译期安全。
 */
abstract class AesBuilder<B extends AesBuilder<B>> {

    final AesMode mode;
    Padding padding = Padding.NONE;
    SecretKey key;
    byte[] iv;
    int tagBits = 128;

    AesBuilder(AesMode mode) {
        this.mode = mode;
    }

    @SuppressWarnings("unchecked")
    final B self() {
        return (B) this;
    }

    /**
     * 设置对称密钥。
     */
    public B key(SecretKey key) {
        this.key = Require.requireNonNull(key, "key");
        return self();
    }

    /**
     * 显式指定初始化向量。
     *
     * <p>
     * 初始化向量 {@code iv} 是一段用于引入随机性的数据，保证同一密钥加密同一明文时产出不同密文。</p>
     * <p>
     * {@code iv} 无需保密，但同一密钥下每次加密都必须独一无二。构建器内部保留副本，调用方此后修改原数组不影响加密结果。</p>
     * <p>
     * 加密时默认会自动生成 {@code iv}，不过也可以使用该方法显示指定 {@code iv}。
     * 除非有特殊要求，否则不推荐调用该方法显示指定 {@code iv}。</p>
     * <p>
     * <b>安全警告：</b>一旦显式设定初始化向量，本构建器不得用于重复加密多段明文——重复使用会在带认证加密或流式模式下严重破坏机密性与完整性。</p>
     *
     * @param iv 初始化向量
     * @return 当前构建器，用于链式配置
     */
    public B iv(byte[] iv) {
        this.iv = Require.requireNonNull(iv, "iv").clone();
        return self();
    }

    /**
     * 加密明文字节，返回由初始化向量与密文（带认证标签时附后）拼接而成的密文；未显式指定初始化向量时每次随机生成。
     */
    public byte[] encrypt(byte[] plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Require.requireNonNull(plaintext, "plaintext");
        byte[] iv = (this.iv != null) ? this.iv : AesEngine.generateIv(mode); // iv 缺省时默认自动生成
        return AesEngine.encrypt(mode, padding, key, iv, tagBits, plaintext);
    }

    /**
     * 按 UTF-8 将文本转为字节后加密，返回含初始化向量的密文。
     */
    public byte[] encrypt(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 加密文本明文，并将含初始化向量的密文以 Base64 编码返回。
     */
    public String encryptToBase64(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Base64.toString(encrypt(plaintext));
    }

    /**
     * 加密文本明文，并将含初始化向量的密文以十六进制编码返回。
     */
    public String encryptToHex(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Hex.toHexString(encrypt(plaintext));
    }

    /**
     * 解密由本族加密产出的密文（须含前置的初始化向量），返回明文字节；解密始终从密文首部读取初始化向量，显式指定的初始化向量仅作用于加密。
     */
    public byte[] decrypt(byte[] combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return AesEngine.decrypt(mode, padding, key, tagBits, combined);
    }

    /**
     * 解密密文并按 UTF-8 还原为文本。
     */
    public String decryptToString(byte[] combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return new String(decrypt(combined), StandardCharsets.UTF_8);
    }

    /**
     * 解码 Base64 密文后解密，按 UTF-8 还原为文本。
     */
    public String decryptFromBase64(String combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return decryptToString(Base64.toByte(combined));
    }

    /**
     * 解码十六进制密文后解密，按 UTF-8 还原为文本。
     */
    public String decryptFromHex(String combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return decryptToString(Hex.toByteArray(combined));
    }
}