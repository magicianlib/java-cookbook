package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HexTest {

    @Test
    public void testToHexStringUppercase() {
        Assertions.assertEquals("00FF0A", Hex.toHexString(new byte[]{0x00, (byte) 0xFF, 0x0A}));
    }

    @Test
    public void testToHexStringExplicitCases() {
        byte[] data = {0x00, (byte) 0xAB, 0x5C};
        Assertions.assertEquals("00AB5C", Hex.toHexStringUpperCase(data));
        Assertions.assertEquals("00ab5c", Hex.toHexStringLowerCase(data));
        // 默认 toHexString 等价于大写
        Assertions.assertEquals(Hex.toHexStringUpperCase(data), Hex.toHexString(data));
    }

    /** 前导 0x00 字节必须保留（旧 BigInteger 实现会丢失）。 */
    @Test
    public void testToByteArrayPreservesLeadingZero() {
        Assertions.assertArrayEquals(new byte[]{0x00}, Hex.toByteArray("00"));
        Assertions.assertArrayEquals(new byte[]{0x00, (byte) 0xFF}, Hex.toByteArray("00FF"));
        Assertions.assertArrayEquals(new byte[]{0x00, 0x11, 0x22}, Hex.toByteArray("001122"));
    }

    @Test
    public void testRoundTripWithLeadingZeros() {
        byte[] data = {0x00, 0x00, (byte) 0xAB, 0x5C};
        Assertions.assertArrayEquals(data, Hex.toByteArray(Hex.toHexString(data)));
    }

    @Test
    public void testToByteArrayAcceptsLowercase() {
        Assertions.assertArrayEquals(new byte[]{(byte) 0xAB, (byte) 0xCD}, Hex.toByteArray("abcd"));
    }

    /** 奇数长度 hex 非法，应抛 IllegalArgumentException。 */
    @Test
    public void testToByteArrayRejectsOddLength() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Hex.toByteArray("ABC"));
    }

    @Test
    public void testNullRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Hex.toHexString(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Hex.toByteArray((String) null));
    }
}
