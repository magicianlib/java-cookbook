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

    public B key(SecretKey key) {
        this.key = Require.requireNonNull(key, "key");
        return self();
    }

    /**
     * 显式指定初始化向量（构建器内部保留副本，调用方此后修改原数组不影响加密结果）。
     * <p>
     * <b>安全警告：</b>同一密钥下，每次加密都必须使用独一无二的初始化向量。
     * 一旦显式设定初始化向量，本构建器不得用于重复加密多段明文——
     * 否则在带认证加密或流式加密模式下会严重破坏机密性与完整性。
     *
     * @param iv 初始化向量
     * @return 当前构建器，用于链式配置
     */
    public B iv(byte[] iv) {
        this.iv = Require.requireNonNull(iv, "iv").clone();
        return self();
    }

    public byte[] encrypt(byte[] plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Require.requireNonNull(plaintext, "plaintext");
        byte[] iv = (this.iv != null) ? this.iv : AesEngine.generateIv(mode);
        return AesEngine.encrypt(mode, padding, key, iv, tagBits, plaintext);
    }

    public byte[] encrypt(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public String encryptToBase64(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Base64.toString(encrypt(plaintext));
    }

    public String encryptToHex(String plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Hex.toHexString(encrypt(plaintext));
    }

    public byte[] decrypt(byte[] combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return AesEngine.decrypt(mode, padding, key, tagBits, combined);
    }

    public String decryptToString(byte[] combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return new String(decrypt(combined), StandardCharsets.UTF_8);
    }

    public String decryptFromBase64(String combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return decryptToString(Base64.toByte(combined));
    }

    public String decryptFromHex(String combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return decryptToString(Hex.toByteArray(combined));
    }
}
