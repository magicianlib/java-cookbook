package io.ituknown.commons;

/**
 * 邮箱脱敏
 *
 * @author magicianlib@gmail.com
 */
public class EmailMaskUtils {

    /**
     * 邮箱掩码处理
     * <p>
     * 长度 <= 2：全部替换为 *
     * 长度 > 2：保留邮箱第一个字符和 @ 之后的所有内容，中间用 **** 代替
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }

        // 验证是否包含 @ 符号
        int atIndex = email.lastIndexOf("@");
        if (atIndex <= 0) {
            // 如果不是标准邮箱格式，简单处理
            return email.replaceAll("(?<=.).", "*");
        }

        String prefix = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        return prefix.charAt(0) + "****" + domain;
    }
}