package io.ituknown.crypto;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PemTest {

    /** 应把 PEM（含头尾、含换行）还原为 DER 字节。 */
    @Test
    public void testExtractPemContentStripsHeaders() {
        String pem = """
                -----BEGIN PRIVATE KEY-----
                AAAB
                CCDE
                -----END PRIVATE KEY-----
                """;
        Assertions.assertArrayEquals(Base64.toByte("AAABCCDE"),
                Pem.extractPemContent(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    /** 非 PEM（裸 DER 二进制）原样返回。 */
    @Test
    public void testExtractPemContentPassesThroughDer() {
        byte[] der = {0x30, (byte) 0x82, 0x01, 0x22, 0x02, 0x01, 0x00};
        Assertions.assertArrayEquals(der, Pem.extractPemContent(der));
    }

    @Test
    public void testIsPemDetectsHeader() {
        Assertions.assertTrue(Pem.isPem("-----BEGIN PRIVATE KEY-----\n".getBytes(StandardCharsets.US_ASCII)));
        Assertions.assertFalse(Pem.isPem(new byte[]{0x30, (byte) 0x82}));
    }

    /** 输入比 -----BEGIN 标记还短：必须返回 false 而非越界。 */
    @Test
    public void testIsPemRejectsShortInput() {
        Assertions.assertFalse(Pem.isPem(new byte[]{0x2D, 0x2D})); // "--"
        Assertions.assertFalse(Pem.isPem(new byte[]{}));
    }

    /** CRLF 行尾应被 \R 正确切分。 */
    @Test
    public void testExtractPemContentHandlesCrlf() {
        byte[] crlf = "-----BEGIN PRIVATE KEY-----\r\nAAAB\r\nCCDE\r\n-----END PRIVATE KEY-----\r\n"
                .getBytes(StandardCharsets.US_ASCII);
        Assertions.assertArrayEquals(Base64.toByte("AAABCCDE"), Pem.extractPemContent(crlf));
    }
}
