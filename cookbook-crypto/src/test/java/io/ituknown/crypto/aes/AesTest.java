package io.ituknown.crypto.aes;

import io.ituknown.crypto.Hex;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
        byte[] plain = "hello, world".getBytes(StandardCharsets.UTF_8);
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
        byte[] plain = "secret".getBytes(StandardCharsets.UTF_8);
        byte[] combined = AesEngine.encrypt(AesMode.GCM, Padding.NONE, key, AesEngine.generateIv(AesMode.GCM), 128, plain);
        combined[combined.length - 1] ^= 0x01; // 篡改认证标签
        assertThrows(AEADBadTagException.class, () -> AesEngine.decrypt(AesMode.GCM, Padding.NONE, key, 128, combined));
    }

    @Test
    public void testEngineCcmTamperThrows() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "secret".getBytes(StandardCharsets.UTF_8);
        byte[] combined = AesEngine.encrypt(AesMode.CCM, Padding.NONE, key, AesEngine.generateIv(AesMode.CCM), 128, plain);
        combined[combined.length - 1] ^= 0x01; // 篡改认证标签
        assertThrows(AEADBadTagException.class, () -> AesEngine.decrypt(AesMode.CCM, Padding.NONE, key, 128, combined));
    }

    @Test
    public void testEngineOcbTamperThrows() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "secret".getBytes(StandardCharsets.UTF_8);
        byte[] combined = AesEngine.encrypt(AesMode.OCB, Padding.NONE, key, AesEngine.generateIv(AesMode.OCB), 128, plain);
        combined[combined.length - 1] ^= 0x01; // 篡改认证标签
        assertThrows(AEADBadTagException.class, () -> AesEngine.decrypt(AesMode.OCB, Padding.NONE, key, 128, combined));
    }

    @Test
    public void testEngineCbcPkcs5RoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "cbc payload".getBytes(StandardCharsets.UTF_8);
        byte[] iv = AesEngine.generateIv(AesMode.CBC);
        byte[] combined = AesEngine.encrypt(AesMode.CBC, Padding.PKCS5, key, iv, 128, plain);
        assertArrayEquals(plain, AesEngine.decrypt(AesMode.CBC, Padding.PKCS5, key, 128, combined));
    }

    @Test
    public void testEngineStreamModesRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "stream payload".getBytes(StandardCharsets.UTF_8);
        for (AesMode mode : new AesMode[]{AesMode.CTR, AesMode.CFB, AesMode.OFB}) {
            byte[] iv = AesEngine.generateIv(mode);
            byte[] combined = AesEngine.encrypt(mode, Padding.NONE, key, iv, 128, plain);
            assertArrayEquals(plain, AesEngine.decrypt(mode, Padding.NONE, key, 128, combined), "round-trip failed for " + mode);
        }
    }

    @Test
    public void testEngineCbcNoPaddingNonAlignedThrows() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "not-aligned".getBytes(StandardCharsets.UTF_8); // 11 字节
        byte[] iv = AesEngine.generateIv(AesMode.CBC);
        assertThrows(IllegalBlockSizeException.class, () -> AesEngine.encrypt(AesMode.CBC, Padding.NONE, key, iv, 128, plain));
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
        byte[] plain = "ccm payload".getBytes(StandardCharsets.UTF_8);
        byte[] iv = AesEngine.generateIv(AesMode.CCM);
        byte[] combined = AesEngine.encrypt(AesMode.CCM, Padding.NONE, key, iv, 128, plain);
        assertArrayEquals(plain, AesEngine.decrypt(AesMode.CCM, Padding.NONE, key, 128, combined));
    }

    @Test
    public void testEngineOcbRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "ocb payload".getBytes(StandardCharsets.UTF_8);
        byte[] iv = AesEngine.generateIv(AesMode.OCB);
        byte[] combined = AesEngine.encrypt(AesMode.OCB, Padding.NONE, key, iv, 128, plain);
        assertArrayEquals(plain, AesEngine.decrypt(AesMode.OCB, Padding.NONE, key, 128, combined));
    }

    @Test
    public void testEngineCbcBcPaddingsRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        byte[] plain = "padding variants".getBytes(StandardCharsets.UTF_8);
        for (Padding p : new Padding[]{Padding.ISO7816, Padding.ANSI_X9_23, Padding.ISO10126, Padding.ZERO, Padding.PKCS7}) {
            byte[] iv = AesEngine.generateIv(AesMode.CBC);
            byte[] combined = AesEngine.encrypt(AesMode.CBC, p, key, iv, 128, plain);
            assertArrayEquals(plain, AesEngine.decrypt(AesMode.CBC, p, key, 128, combined), "round-trip failed for padding " + p);
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

    /**
     * 门面与新引擎 API 双向互通：同一密钥下密文格式逐字节一致。
     */
    @Test
    public void testFacadeInteropWithAesGcm() throws Exception {
        SecretKey key = AesUtils.generateKey(256);
        String plain = "interop check";
        // 门面加密 -> 新 API 解密
        byte[] facadeCt = AesUtils.encrypt(plain, key);
        assertEquals(plain, Aes.gcm().tagBits(128).key(key).decryptToString(facadeCt));
        // 新 API 加密 -> 门面解密
        byte[] aesCt = Aes.gcm().tagBits(128).key(key).encrypt(plain);
        assertEquals(plain, AesUtils.decrypt(aesCt, key));
    }

    /** 按模式返回配好 padding 的新构建器（CBC 需 PKCS5，其余 NONE）。 */
    private static AesBuilder<?> builderFor(AesMode mode) {
        return switch (mode) {
            case GCM -> Aes.gcm();
            case CCM -> Aes.ccm();
            case OCB -> Aes.ocb();
            case CBC -> Aes.cbc().padding(Padding.PKCS5);
            case CTR -> Aes.ctr();
            case CFB -> Aes.cfb();
            case OFB -> Aes.ofb();
        };
    }

    /** 七个 fluent 入口（Aes.ccm/ocb/cfb/ofb 此前从未走 builder 链）均应完成加解密往返。 */
    @Test
    public void testFluentBuildersForAllModes() throws Exception {
        SecretKey key = newKey(128);
        String plain = "all modes round-trip";
        for (AesMode mode : AesMode.values()) {
            byte[] combined = builderFor(mode).key(key).encrypt(plain);
            assertEquals(plain, builderFor(mode).key(key).decryptToString(combined),
                    "round-trip failed for " + mode);
        }
    }

    /** builder 必须拒绝 null 密钥。 */
    @Test
    public void testBuilderRejectsNullKey() {
        assertThrows(IllegalArgumentException.class, () -> Aes.gcm().key(null));
    }

    /** builder 必须拒绝 null 初始化向量。 */
    @Test
    public void testBuilderRejectsNullIv() {
        assertThrows(IllegalArgumentException.class, () -> Aes.gcm().iv(null));
    }

    /** builder 必须拒绝 null 明文。 */
    @Test
    public void testBuilderRejectsNullPlaintext() throws Exception {
        SecretKey key = newKey(128);
        assertThrows(IllegalArgumentException.class, () -> Aes.gcm().key(key).encrypt((byte[]) null));
    }

    /** 解密时密文短于 IV 长度应抛非法参数异常（覆盖引擎短密文分支）。 */
    @Test
    public void testDecryptRejectsShortCiphertext() throws Exception {
        SecretKey key = newKey(128);
        // 4 字节短于 GCM 的 12 字节 IV
        assertThrows(IllegalArgumentException.class, () -> Aes.gcm().key(key).decrypt(new byte[4]));
    }

    /** GCM 96 位认证标签同样应完成往返（覆盖 tagBits 非默认值路径）。 */
    @Test
    public void testGcm96BitTagRoundTrip() throws Exception {
        SecretKey key = newKey(128);
        String plain = "ninety-six bit tag";
        byte[] combined = Aes.gcm().tagBits(96).key(key).encrypt(plain);
        assertEquals(plain, Aes.gcm().tagBits(96).key(key).decryptToString(combined));
    }

    /** iv(byte[]) 防御性 clone：设入后原地篡改原数组不应改变同一构建器的加密结果。 */
    @Test
    public void testExplicitIvDefensivelyCloned() throws Exception {
        SecretKey key = newKey(128);
        String plain = "defensive clone";
        byte[] iv = Hex.toByteArray("00112233445566778899AABBCCDDEEFF");
        BlockAesBuilder b = Aes.cbc().padding(Padding.PKCS5).key(key).iv(iv);
        String first = b.encryptToHex(plain);
        iv[0] ^= (byte) 0xFF; // 原地篡改原数组
        String second = b.encryptToHex(plain);
        assertEquals(first, second, "builder 应防御性 clone，外部篡改不应影响加密结果");
    }
}