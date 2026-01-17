package io.ituknown.crypto;

public class Base64 {

    public static String toString(byte[] encoded) {
        return java.util.Base64.getEncoder().encodeToString(encoded);
    }

    public static byte[] toByte(String decoded) {
        return java.util.Base64.getDecoder().decode(decoded);
    }
}