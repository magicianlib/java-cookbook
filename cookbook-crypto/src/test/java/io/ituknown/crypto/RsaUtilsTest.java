package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RsaUtilsTest {

    private static java.security.KeyPair newKeyPair() throws Exception {
        java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    @Test
    public void testEncryptDecryptRoundTripBytes() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        byte[] data = "hello,rsa".getBytes(StandardCharsets.UTF_8);
        byte[] cipher = RsaUtils.encrypt(data, kp.getPublic());
        byte[] plain = RsaUtils.decrypt(cipher, kp.getPrivate());
        Assertions.assertArrayEquals(data, plain);
    }

    @Test
    public void testEncryptToBase64AndDecryptFromStringRoundTrip() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        String ct = RsaUtils.encryptToBase64("hello,rsa", kp.getPublic());
        byte[] plain = RsaUtils.decryptFromBase64(ct, kp.getPrivate());
        Assertions.assertArrayEquals("hello,rsa".getBytes(StandardCharsets.UTF_8), plain);
    }

    @Test
    public void testEncryptToBase64AndDecryptFromBase64ToString() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        String ct = RsaUtils.encryptToBase64("hello,rsa", kp.getPublic());
        String pt = RsaUtils.decryptFromBase64ToString(ct, kp.getPrivate());
        Assertions.assertEquals("hello,rsa", pt);
    }

    @Test
    public void testDecryptToString() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        byte[] data = "hello,rsa".getBytes(StandardCharsets.UTF_8);
        byte[] cipher = RsaUtils.encrypt(data, kp.getPublic());
        Assertions.assertEquals("hello,rsa", RsaUtils.decryptToString(cipher, kp.getPrivate()));
    }

    @Test
    public void testNullArgsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaUtils.encrypt(null, null));
    }

    @Test
    public void testDecryptWithWrongKeyThrowsBadPadding() throws Exception {
        java.security.KeyPair encryptKp = newKeyPair();
        java.security.KeyPair decryptKp = newKeyPair();
        byte[] cipher = RsaUtils.encrypt("hello,rsa".getBytes(StandardCharsets.UTF_8), encryptKp.getPublic());
        Assertions.assertThrows(javax.crypto.BadPaddingException.class,
                () -> RsaUtils.decrypt(cipher, decryptKp.getPrivate()));
    }

    /** 2048/OAEP-SHA256 明文上限为 190 字节：正好 190 应成功，191 应被拒绝。 */
    @Test
    public void testPlaintextSizeBoundary() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        // 上限：正好 190 字节，必须可加密并可解密还原
        byte[] atLimit = new byte[190];
        byte[] cipher = RsaUtils.encrypt(atLimit, kp.getPublic());
        Assertions.assertArrayEquals(atLimit, RsaUtils.decrypt(cipher, kp.getPrivate()));
        // 超限：191 字节必须抛 IllegalArgumentException
        byte[] overLimit = new byte[191];
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaUtils.encrypt(overLimit, kp.getPublic()));
    }
}
