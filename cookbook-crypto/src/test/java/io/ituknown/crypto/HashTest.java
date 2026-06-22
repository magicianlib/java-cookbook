package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

public class HashTest {

    /** 标准已知答案测试（Known-Answer Test），锁定各算法正确性，含国密 SM3 官方向量。 */
    @Test
    public void testKnownAnswerVectors() throws NoSuchAlgorithmException {
        Assertions.assertEquals("900150983CD24FB0D6963F7D28E17F72", Hash.MD5.hashHex("abc"));
        Assertions.assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", Hash.SHA1.hashHex("abc"));
        Assertions.assertEquals("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD", Hash.SHA256.hashHex("abc"));
        Assertions.assertEquals("CB00753F45A35E8BB5A03D699AC65007272C32AB0EDED1631A8B605A43FF5BED8086072BA1E7CC2358BAECA134C825A7", Hash.SHA384.hashHex("abc"));
        Assertions.assertEquals("DDAF35A193617ABACC417349AE20413112E6FA4E89A97EA20A9EEEE64B55D39A2192992A274FC1A836BA3C23A3FEEBBD454D4423643CE80E2A9AC94FA54CA49F", Hash.SHA512.hashHex("abc"));
        Assertions.assertEquals("66C7F0F462EEEDD9D1F2D46BDC10E4E24167C4875CF2F7A2297DA02B8F4BA8E0", Hash.SM3.hashHex("abc"));
    }

    /** 各算法摘要长度（字节）。 */
    @Test
    public void testDigestLength() throws NoSuchAlgorithmException {
        Assertions.assertEquals(16, Hash.MD5.hash("abc").length);
        Assertions.assertEquals(20, Hash.SHA1.hash("abc").length);
        Assertions.assertEquals(32, Hash.SHA256.hash("abc").length);
        Assertions.assertEquals(48, Hash.SHA384.hash("abc").length);
        Assertions.assertEquals(64, Hash.SHA512.hash("abc").length);
        Assertions.assertEquals(32, Hash.SM3.hash("abc").length);
    }

    /** 相同输入两次计算结果一致（确定性）。 */
    @Test
    public void testDeterministic() throws NoSuchAlgorithmException {
        Assertions.assertArrayEquals(Hash.SM3.hash("abc"), Hash.SM3.hash("abc"));
        Assertions.assertEquals(Hash.SHA256.hashHex("abc"), Hash.SHA256.hashHex("abc"));
    }

    /** Base64 输出与字节结果一致。 */
    @Test
    public void testHashBase64() throws NoSuchAlgorithmException {
        String b64 = Hash.SHA256.hashBase64("abc");
        Assertions.assertArrayEquals(Hash.SHA256.hash("abc"), Base64.toByte(b64));
    }

    /** 不同算法对同一输入产出不同摘要。 */
    @Test
    public void testAlgorithmsProduceDistinctDigests() throws NoSuchAlgorithmException {
        Assertions.assertNotEquals(Hash.SHA256.hashHex("abc"), Hash.SM3.hashHex("abc"));
        Assertions.assertNotEquals(Hash.SHA256.hashHex("abc"), Hash.SHA512.hashHex("abc"));
    }

    @Test
    public void testNullRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Hash.SHA256.hash((byte[]) null));
    }

    /** @Deprecated 算法仍可工作（兼容旧数据）。 */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedAlgorithmsStillWork() throws NoSuchAlgorithmException {
        Assertions.assertEquals(16, Hash.MD5.hash("abc").length);
        Assertions.assertEquals(20, Hash.SHA1.hash("abc").length);
    }
}
