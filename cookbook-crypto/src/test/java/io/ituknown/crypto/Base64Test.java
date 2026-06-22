package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Base64Test {

    @Test
    public void testRoundTrip() {
        byte[] data = {0x01, 0x02, (byte) 0xFF, 0x10};
        Assertions.assertArrayEquals(data, Base64.toByte(Base64.toString(data)));
    }

    /** toByte 应容忍空白与换行（与各 crypto 消费方需求一致）。 */
    @Test
    public void testToByteToleratesWhitespace() {
        byte[] expected = Base64.toByte("AAEC");
        Assertions.assertArrayEquals(expected, Base64.toByte("AA\nEC"));
        Assertions.assertArrayEquals(expected, Base64.toByte(" AA EC "));
        Assertions.assertArrayEquals(expected, Base64.toByte("AA\r\nEC\n"));
    }

    @Test
    public void testNullRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Base64.toString(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Base64.toByte(null));
    }
}
