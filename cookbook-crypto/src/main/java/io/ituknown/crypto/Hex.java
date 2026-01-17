package io.ituknown.crypto;

import java.math.BigInteger;
import java.util.Arrays;

public final class Hex {

    /**
     * Convert each byte to a hexadecimal string
     */
    public static String toHexString(byte[] data) {
        StringBuilder builder = new StringBuilder(2 * data.length);

        for (byte b : data) {
            builder.append(String.format("%02X", b));
        }

        return builder.toString();
    }

    public static byte[] toByteArray(String hexString) {
        byte[] bytes = new BigInteger(hexString, 16).toByteArray();

        // The BigInteger.toByteArray() method includes a leading zero for positive values.
        // If the first byte is zero, remove it.
        if (bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }

        return bytes;
    }
}