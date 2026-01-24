package io.ituknown.commons;

import java.util.function.Function;

/**
 * 号码脱敏
 *
 * @author magicianlib@gmail.com
 */
public class PhoneMaskUtils {
    public final static String MOBILE_MASK_REGEX = "(\\d{3})\\d{4}(\\d{4})";

    /**
     * 普通手机号掩码 (隐藏中间4位)
     * <p>
     * 示例：{@code 13812345678 -> 138****5678}
     */
    public static String mobile(String mobile) {
        String trim;
        if (mobile == null || (trim = mobile.trim()).length() != 11) {
            return mobile;
        }
        return trim.replaceAll(MOBILE_MASK_REGEX, "$1****$2");
    }

    /**
     * 自定义掩码范围
     * <p>
     * 示例：{@code ("13812345678", 3, 2) -> 138******78}
     *
     * @param phone 电话号码
     * @param head  保留号码前几位
     * @param tail  保留号码后几位
     */
    public static String custom(String phone, int head, int tail) {
        if (phone == null || phone.isBlank()) {
            return "";
        }

        phone = phone.trim();
        int len = phone.length();
        if (head + tail > len) {
            return phone;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i < head || i >= (len - tail)) {
                sb.append(phone.charAt(i));
            } else {
                sb.append("*");
            }
        }
        return sb.toString();
    }

    /**
     * 固定电话脱敏
     * <p>
     * 示例：{@code 021-62231234 -> 021-****1234}
     */
    public static String fixedPhone(String phone) {
        return fixedPhone(phone, s -> {
            if (s.length() < 7) {
                return 2;
            } else {
                return s.length() == 7 ? 3 : 4;
            }
        });
    }

    /**
     * 固定电话脱敏
     * <p>
     * 示例：{@code ("021-62231234", 4) -> 021-****1234}
     *
     * @param tail 保留号码后几位
     */
    public static String fixedPhone(String phone, int tail) {
        return fixedPhone(phone, (s) -> tail);
    }

    /**
     * 固定电话脱敏
     * <p>
     * 示例：{@code 021-62231234 -> 021-****1234}
     *
     * @param cal 根据座机号计算保留指定后几位
     */
    public static String fixedPhone(String phone, Production<String> cal) {
        if (phone == null || phone.isBlank() || !phone.contains("-")) {
            return phone;
        }

        phone = phone.trim();

        String[] parts = phone.split("-");
        String areaCode = parts[0]; // 区号
        String number = parts[1];   // 座机号

        Integer tail = cal.apply(number);

        // 如果号码长度小于等于要保留的位数，则不进行掩码，避免下标越界
        if (number.length() <= tail) {
            return phone;
        }

        // 计算截取的起始位置：总长度 - 保留位数
        String maskedPart = "****";
        String visiblePart = number.substring(number.length() - tail);

        return areaCode + "-" + maskedPart + visiblePart;
    }

    /**
     * 根据号码自动识别脱敏类型
     */
    public static String auto(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        phone = phone.trim();

        if (phone.contains("-")) {
            return fixedPhone(phone);
        }

        if (phone.length() != 11) {
            return custom(phone, 3, 2);
        }

        return mobile(phone);
    }

    public interface Production<T> extends Function<T, Integer> {
    }
}