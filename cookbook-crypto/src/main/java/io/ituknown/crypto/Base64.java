package io.ituknown.crypto;

/**
 * Base64 编解码工具类。
 * <p>
 * {@link #toByte(String)} 在解码前会去除所有空白与换行符，因此单行或多行（例如直接复制带换行的 Base64）均可正确解码。
 *
 * @author magicianlib@gmail.com
 */
public final class Base64 {

    private Base64() {
    }

    /**
     * 将字节数组编码为 Base64 字符串。
     *
     * @param encoded 字节数组
     * @return Base64 字符串
     * @throws IllegalArgumentException encoded 为 null
     */
    public static String toString(byte[] encoded) {
        Require.requireNonNull(encoded, "encoded");
        return java.util.Base64.getEncoder().encodeToString(encoded);
    }

    /**
     * 将 Base64 字符串解码为字节数组。会先去除所有空白与换行符。
     *
     * @param decoded Base64 字符串（允许含换行/空白）
     * @return 字节数组
     * @throws IllegalArgumentException decoded 为 null
     */
    public static byte[] toByte(String decoded) {
        Require.requireNonNull(decoded, "decoded");
        return java.util.Base64.getDecoder().decode(decoded.replaceAll("\\s", ""));
    }
}
