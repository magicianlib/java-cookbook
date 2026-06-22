package io.ituknown.crypto;

/**
 * 十六进制与字节数组互转工具类。
 *
 * @author magicianlib@gmail.com
 */
public final class Hex {

    private static final char[] HEX_UPPER = "0123456789ABCDEF".toCharArray();
    private static final char[] HEX_LOWER = "0123456789abcdef".toCharArray();

    private Hex() {
    }

    /**
     * 将字节数组转换为大写十六进制字符串（默认，等价于 {@link #toHexStringUpperCase(byte[])}）。
     *
     * @param data 字节数组
     * @return 大写十六进制字符串（每个字节对应两个字符）
     * @throws IllegalArgumentException data 为 null
     */
    public static String toHexString(byte[] data) {
        return toHexString(data, HEX_UPPER);
    }

    /**
     * 将字节数组转换为大写十六进制字符串。
     *
     * @param data 字节数组
     * @return 大写十六进制字符串
     * @throws IllegalArgumentException data 为 null
     */
    public static String toHexStringUpperCase(byte[] data) {
        return toHexString(data, HEX_UPPER);
    }

    /**
     * 将字节数组转换为小写十六进制字符串。
     *
     * @param data 字节数组
     * @return 小写十六进制字符串
     * @throws IllegalArgumentException data 为 null
     */
    public static String toHexStringLowerCase(byte[] data) {
        return toHexString(data, HEX_LOWER);
    }

    private static String toHexString(byte[] data, char[] table) {
        Require.requireNonNull(data, "data");
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            out[i * 2] = table[v >>> 4];
            out[i * 2 + 1] = table[v & 0x0F];
        }
        return new String(out);
    }

    /**
     * 将十六进制字符串转换为字节数组（大小写不敏感）。
     * <p>
     * 要求字符串长度为偶数：每两个十六进制字符对应一个字节。前导 {@code 0x00} 字节会被完整保留。
     *
     * @param hexString 十六进制字符串
     * @return 字节数组
     * @throws IllegalArgumentException hexString 为 null，或长度为奇数，或含非十六进制字符
     */
    public static byte[] toByteArray(String hexString) {
        Require.requireNonNull(hexString, "hexString");
        int len = hexString.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("Hex string must have an even length, but was " + len);
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            int high = hexCharToNibble(hexString.charAt(i * 2));
            int low = hexCharToNibble(hexString.charAt(i * 2 + 1));
            out[i] = (byte) ((high << 4) | low);
        }
        return out;
    }

    private static int hexCharToNibble(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        throw new IllegalArgumentException("Invalid hex character: " + c);
    }
}
