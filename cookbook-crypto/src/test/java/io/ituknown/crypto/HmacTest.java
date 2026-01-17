package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HmacTest {
    static final String SECRET = "f$s1f@9.";
    static final String PLAINTEXT = "hello,world";

    @Test
    public void testHmacMD5() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacMD5.hmacHex(SECRET, PLAINTEXT);
        System.out.printf("%-12s%s%n", Hmac.HmacMD5.name(), hex);
        Assertions.assertNotNull(hex);
    }

    @Test
    public void testHmacSHA1() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacSHA1.hmacHex(SECRET, PLAINTEXT);
        System.out.printf("%-12s%s%n", Hmac.HmacSHA1.name(), hex);
        Assertions.assertNotNull(hex);
    }

    @Test
    public void testHmacSHA256() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacSHA256.hmacHex(SECRET, PLAINTEXT);
        System.out.printf("%-12s%s%n", Hmac.HmacSHA256.name(), hex);
        Assertions.assertNotNull(hex);
    }

    @Test
    public void testHmacSHA384() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacSHA384.hmacHex(SECRET, PLAINTEXT);
        System.out.printf("%-12s%s%n", Hmac.HmacSHA384.name(), hex);
        Assertions.assertNotNull(hex);
    }

    @Test
    public void testHmacSHA512() throws NoSuchAlgorithmException, InvalidKeyException {
        String hex = Hmac.HmacSHA512.hmacHex(SECRET, PLAINTEXT);
        System.out.printf("%-12s%s%n", Hmac.HmacSHA512.name(), hex);
        Assertions.assertNotNull(hex);
    }
}