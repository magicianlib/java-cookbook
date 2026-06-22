package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HmacTest {
    static final String SECRET = "f$s1f@9.";
    static final String PLAINTEXT = "hello,world";

    @SuppressWarnings("deprecation")
    @Test
    public void testHmacMD5() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacMD5.hmacHex(SECRET, PLAINTEXT);
        Assertions.assertNotNull(hex);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testHmacSHA1() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacSHA1.hmacHex(SECRET, PLAINTEXT);
        Assertions.assertNotNull(hex);
    }

    @Test
    public void testHmacSHA256() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacSHA256.hmacHex(SECRET, PLAINTEXT);
        Assertions.assertNotNull(hex);
    }

    @Test
    public void testHmacSHA384() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacSHA384.hmacHex(SECRET, PLAINTEXT);
        Assertions.assertNotNull(hex);
    }

    @Test
    public void testHmacSHA512() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacSHA512.hmacHex(SECRET, PLAINTEXT);
        Assertions.assertNotNull(hex);
    }

    /** 相同输入两次计算结果一致（确定性）。 */
    @Test
    public void testHmacDeterministic() throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] a = Hmac.HmacSHA256.hmac(SECRET.getBytes(StandardCharsets.UTF_8), PLAINTEXT.getBytes(StandardCharsets.UTF_8));
        byte[] b = Hmac.HmacSHA256.hmac(SECRET, PLAINTEXT);
        Assertions.assertArrayEquals(a, b);
    }

    @Test
    public void testHmacBase64() throws NoSuchAlgorithmException, InvalidKeyException {
        String b64 = Hmac.HmacSHA256.hmacBase64(SECRET, PLAINTEXT);
        Assertions.assertNotNull(b64);
        Assertions.assertEquals(
                Hex.toHexString(Hmac.HmacSHA256.hmac(SECRET, PLAINTEXT)),
                Hex.toHexString(Base64.toByte(b64)));
    }

    @Test
    public void testNullArgsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Hmac.HmacSHA256.hmac(null, (byte[]) null));
    }
}
