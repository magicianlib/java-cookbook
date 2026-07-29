package io.ituknown.crypto.aes;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AesUtilsTest {
    final AesUtils.Key KEY = new AesUtils.Key(
            "E430CC9B2E50C128DB01D5B3DDCB909AA64C494992120CF35150274D09EFECE1",
            "5DDMmy5QwSjbAdWz3cuQmqZMSUmSEgzzUVAnTQnv7OE="
    );

    final String plaintext = "hello, world";
    final String ciphertext = "6jnoRfDK2wy6VWrvL/kFrsFYEcQrBZ3AY34ECa6jRy84vGUoGwkElg==";

    @Test
    public void testGenerateEncodedKey() {
        System.out.println("testGenerateEncodedKey:");
        System.out.println("hexString: " + KEY.hexString());
        System.out.println("base64String: " + KEY.base64String());
        Assertions.assertTrue(StringUtils.isNotBlank(KEY.base64String()));
    }

    @Test
    public void testEncrypt() throws Exception {
        String ciphertext = AesUtils.encryptToBase64String(plaintext, KEY.fromBase64());
        Assertions.assertTrue(StringUtils.isNotBlank(ciphertext));
    }

    @Test
    public void testDecrypt() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String plaintext = AesUtils.decryptFromBase64String(ciphertext, KEY.fromBase64());
        Assertions.assertEquals(plaintext, this.plaintext);
    }

    @Test
    public void testEncryptBytesRoundTrip() throws Exception {
        byte[] data = plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] combined = AesUtils.encrypt(data, KEY.fromBase64());
        Assertions.assertEquals(plaintext, AesUtils.decrypt(combined, KEY.fromBase64()));
    }

    /** 十六进制密文往返（覆盖 Hex 前导零修复在 AES 路径的正确性）。 */
    @Test
    public void testHexRoundTrip() throws Exception {
        String hex = AesUtils.encryptToHexString(plaintext, KEY.fromBase64());
        Assertions.assertEquals(plaintext, AesUtils.decryptFromHexString(hex, KEY.fromBase64()));
    }

    /** 首字节为 0x00 的密钥经 Hex 往返必须保留长度（旧 Hex 实现会丢字节）。 */
    @Test
    public void testFromHexPreservesLeadingZeroKey() {
        byte[] raw = new byte[16];
        raw[0] = 0x00;
        raw[15] = (byte) 0xFF;
        String hex = Hex.toHexString(raw);
        Assertions.assertEquals(16, Hex.toByteArray(hex).length);
        javax.crypto.SecretKey original = new javax.crypto.spec.SecretKeySpec(raw, 0, 16, "AES");
        Assertions.assertEquals(original, new AesUtils.Key(hex, Base64.toString(raw)).fromHex());
    }

    @Test
    public void testNullArgsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AesUtils.encrypt((byte[]) null, null));
    }

    @Test
    public void testGenerateKeyRejectsInvalidSize() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AesUtils.generateKey(100));
    }

    /** 静态工厂从两种编码还原的密钥应彼此等价。 */
    @Test
    public void testOfFactoriesProduceEqualKey() {
        javax.crypto.SecretKey fromHex = AesUtils.Key.ofHex(KEY.hexString());
        javax.crypto.SecretKey fromBase64 = AesUtils.Key.ofBase64(KEY.base64String());
        Assertions.assertEquals(fromHex, fromBase64);
    }

    /** 仅持有单一编码时，可直接经静态工厂还原密钥并完成加解密往返。 */
    @Test
    public void testOfHexRoundTrip() throws Exception {
        javax.crypto.SecretKey key = AesUtils.Key.ofHex(KEY.hexString());
        byte[] combined = AesUtils.encrypt(plaintext, key);
        Assertions.assertEquals(plaintext, AesUtils.decrypt(combined, key));
    }
}
