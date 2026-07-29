package io.ituknown.crypto.aes;

import io.ituknown.crypto.Require;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * AES 共用加解密引擎：拼转换串、按家族选参数对象、产出「IV + 密文(+AEAD 标签)」组合输出。
 */
final class AesEngine {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesEngine() {
    }

    static byte[] generateIv(AesMode mode) {
        byte[] iv = new byte[mode.ivLength];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    static byte[] encrypt(AesMode mode, Padding padding, SecretKey key, byte[] iv, int tagBits, byte[] plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Require.requireNonNull(key, "key");
        Require.requireNonNull(iv, "iv");
        Require.requireNonNull(plaintext, "plaintext");
        Cipher cipher = createCipher(mode, padding);
        cipher.init(Cipher.ENCRYPT_MODE, key, paramsOf(mode, iv, tagBits));
        byte[] ciphertext = cipher.doFinal(plaintext);
        return concat(iv, ciphertext);
    }

    static byte[] decrypt(AesMode mode, Padding padding, SecretKey key, int tagBits, byte[] combined)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                   InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Require.requireNonNull(key, "key");
        Require.requireNonNull(combined, "combined");
        if (combined.length < mode.ivLength) {
            throw new IllegalArgumentException("Invalid ciphertext: shorter than IV length");
        }
        byte[] iv = Arrays.copyOfRange(combined, 0, mode.ivLength);
        byte[] ciphertext = Arrays.copyOfRange(combined, mode.ivLength, combined.length);
        Cipher cipher = createCipher(mode, padding);
        cipher.init(Cipher.DECRYPT_MODE, key, paramsOf(mode, iv, tagBits));
        return cipher.doFinal(ciphertext);
    }

    private static Cipher createCipher(AesMode mode, Padding padding)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        String transformation = "AES/" + mode.transformation + "/" + padding.transformation;
        return Cipher.getInstance(transformation);
    }

    private static AlgorithmParameterSpec paramsOf(AesMode mode, byte[] iv, int tagBits) {
        if (mode.family == AesMode.Family.AEAD) {
            return new GCMParameterSpec(tagBits, iv);
        }
        return new IvParameterSpec(iv);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
