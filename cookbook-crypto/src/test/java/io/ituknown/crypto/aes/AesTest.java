package io.ituknown.crypto.aes;

import org.junit.jupiter.api.Test;

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
        assertEquals("ISO7816d4Padding", Padding.ISO7816.transformation);
        assertEquals("X9.23Padding", Padding.ANSI_X9_23.transformation);
        assertEquals("ISO10126d2Padding", Padding.ISO10126.transformation);
        assertEquals("ZeroBytePadding", Padding.ZERO.transformation);
    }
}
