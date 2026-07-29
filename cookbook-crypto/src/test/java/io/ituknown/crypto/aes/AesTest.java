package io.ituknown.crypto.aes;

import io.ituknown.crypto.Hex;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

public class AesTest {

    @Test
    public void testAesModeMetadata() {
        assertEquals("GCM", AesMode.GCM.transformation);
        assertEquals(AesMode.Family.AEAD, AesMode.GCM.family);
        assertEquals(12, AesMode.GCM.ivLength);
        assertFalse(AesMode.GCM.requiresBc);

        assertEquals("CBC", AesMode.CBC.transformation);
        assertEquals(AesMode.Family.BLOCK, AesMode.CBC.family);
        assertEquals(16, AesMode.CBC.ivLength);
        assertFalse(AesMode.CBC.requiresBc);

        assertEquals("CTR", AesMode.CTR.transformation);
        assertEquals(AesMode.Family.STREAM, AesMode.CTR.family);
        assertEquals(16, AesMode.CTR.ivLength);

        assertTrue(AesMode.CCM.requiresBc);
        assertTrue(AesMode.OCB.requiresBc);
        assertEquals(12, AesMode.OCB.ivLength);
        assertEquals(16, AesMode.CFB.ivLength);
        assertEquals(16, AesMode.OFB.ivLength);
    }

    @Test
    public void testPaddingMetadata() {
        assertEquals("NoPadding", Padding.NONE.transformation);
        assertFalse(Padding.NONE.requiresBc);
        assertEquals("PKCS5Padding", Padding.PKCS5.transformation);
        assertFalse(Padding.PKCS5.requiresBc);
        assertTrue(Padding.PKCS7.requiresBc);
        assertEquals("ISO7816-4Padding", Padding.ISO7816.transformation);
        assertEquals("X9.23Padding", Padding.ANSI_X9_23.transformation);
        assertEquals("ISO10126-2Padding", Padding.ISO10126.transformation);
        assertEquals("ZeroBytePadding", Padding.ZERO.transformation);
    }

    private static SecretKey newKey(int bits) throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(bits);
        return kg.generateKey();
    }

    @Test
    public void testEngineGcmRoundTrip() throws Exception {
        SecretKey key = newKey(256);
        byte[] plain = "hello, world".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] iv = AesEngine.generateIv(AesMode.GCM);
        byte[] combined = AesEngine.encrypt(AesMode.GCM, Padding.NONE, key, iv, 128, plain);
        // IV 前置：组合长度 = 12(IV) + 明文 + 16(标签)
        assertEquals(12 + plain.length + 16, combined.length);
        byte[] decrypted = AesEngine.decrypt(AesMode.GCM, Padding.NONE, key, 128, combined);
        assertArrayEquals(plain, decrypted);
    }

    @Test
    public void testEngineGcmTamperThrows() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "secret".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] combined = AesEngine.encrypt(AesMode.GCM, Padding.NONE, key,
                AesEngine.generateIv(AesMode.GCM), 128, plain);
        combined[combined.length - 1] ^= 0x01; // 篡改认证标签
        assertThrows(AEADBadTagException.class,
                () -> AesEngine.decrypt(AesMode.GCM, Padding.NONE, key, 128, combined));
    }

    @Test
    public void testEngineCbcPkcs5RoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "cbc payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] iv = AesEngine.generateIv(AesMode.CBC);
        byte[] combined = AesEngine.encrypt(AesMode.CBC, Padding.PKCS5, key, iv, 128, plain);
        assertArrayEquals(plain, AesEngine.decrypt(AesMode.CBC, Padding.PKCS5, key, 128, combined));
    }

    @Test
    public void testEngineStreamModesRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "stream payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (AesMode mode : new AesMode[]{AesMode.CTR, AesMode.CFB, AesMode.OFB}) {
            byte[] iv = AesEngine.generateIv(mode);
            byte[] combined = AesEngine.encrypt(mode, Padding.NONE, key, iv, 128, plain);
            assertArrayEquals(plain, AesEngine.decrypt(mode, Padding.NONE, key, 128, combined),
                    "round-trip failed for " + mode);
        }
    }

    @Test
    public void testEngineCbcNoPaddingNonAlignedThrows() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "not-aligned".getBytes(java.nio.charset.StandardCharsets.UTF_8); // 11 字节
        byte[] iv = AesEngine.generateIv(AesMode.CBC);
        assertThrows(IllegalBlockSizeException.class,
                () -> AesEngine.encrypt(AesMode.CBC, Padding.NONE, key, iv, 128, plain));
    }

    @Test
    public void testBouncyCastleRegisteredAtMostOnce() {
        BouncyCastleSupport.ensureRegistered();
        assertNotNull(Security.getProvider("BC"));
        int before = Security.getProviders().length;
        BouncyCastleSupport.ensureRegistered();
        BouncyCastleSupport.ensureRegistered();
        assertEquals(before, Security.getProviders().length);
    }

    @Test
    public void testEngineCcmRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "ccm payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] iv = AesEngine.generateIv(AesMode.CCM);
        byte[] combined = AesEngine.encrypt(AesMode.CCM, Padding.NONE, key, iv, 128, plain);
        assertArrayEquals(plain, AesEngine.decrypt(AesMode.CCM, Padding.NONE, key, 128, combined));
    }

    @Test
    public void testEngineOcbRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "ocb payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] iv = AesEngine.generateIv(AesMode.OCB);
        byte[] combined = AesEngine.encrypt(AesMode.OCB, Padding.NONE, key, iv, 128, plain);
        assertArrayEquals(plain, AesEngine.decrypt(AesMode.OCB, Padding.NONE, key, 128, combined));
    }

    @Test
    public void testEngineCbcBcPaddingsRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "padding variants".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (Padding p : new Padding[]{Padding.ISO7816, Padding.ANSI_X9_23, Padding.ISO10126, Padding.ZERO, Padding.PKCS7}) {
            byte[] iv = AesEngine.generateIv(AesMode.CBC);
            byte[] combined = AesEngine.encrypt(AesMode.CBC, p, key, iv, 128, plain);
            assertArrayEquals(plain, AesEngine.decrypt(AesMode.CBC, p, key, 128, combined),
                    "round-trip failed for padding " + p);
        }
    }

    @Test
    public void testGcmFluentRoundTrip() throws Exception {
        SecretKey key = newKey(256);
        String plain = "hello, world";
        byte[] combined = Aes.gcm().tagBits(128).key(key).encrypt(plain);
        assertEquals(plain, Aes.gcm().tagBits(128).key(key).decryptToString(combined));
    }

    @Test
    public void testCbcFluentRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        String plain = "cbc payload";
        byte[] combined = Aes.cbc().padding(Padding.PKCS5).key(key).encrypt(plain);
        assertEquals(plain, Aes.cbc().padding(Padding.PKCS5).key(key).decryptToString(combined));
    }

    @Test
    public void testCtrFluentRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        String plain = "ctr stream";
        byte[] combined = Aes.ctr().key(key).encrypt(plain);
        assertEquals(plain, Aes.ctr().key(key).decryptToString(combined));
    }

    @Test
    public void testBase64AndHexConvenienceRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        String plain = "convenience";
        String b64 = Aes.gcm().key(key).encryptToBase64(plain);
        assertEquals(plain, Aes.gcm().key(key).decryptFromBase64(b64));
        String hex = Aes.gcm().key(key).encryptToHex(plain);
        assertEquals(plain, Aes.gcm().key(key).decryptFromHex(hex));
    }

    @Test
    public void testRandomIvProducesDistinctCiphertext() throws Exception {
        SecretKey key = newKey(128);
        String plain = "same plaintext";
        String a = Aes.gcm().key(key).encryptToHex(plain);
        String b = Aes.gcm().key(key).encryptToHex(plain);
        assertNotEquals(a, b);
    }

    @Test
    public void testExplicitIvIsDeterministic() throws Exception {
        SecretKey key = newKey(128);
        String plain = "deterministic";
        byte[] iv = Hex.toByteArray("00112233445566778899AABBCCDDEEFF");
        String a = Aes.cbc().padding(Padding.PKCS5).iv(iv).key(key).encryptToHex(plain);
        String b = Aes.cbc().padding(Padding.PKCS5).iv(iv).key(key).encryptToHex(plain);
        assertEquals(a, b);
    }
}
