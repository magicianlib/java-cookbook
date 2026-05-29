package io.ituknown.jackson.serializer;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 脱敏类型
 *
 * @author magicianlib@gmail.com
 */
public enum SensitiveType {
    /**
     * 姓名：只保留第一个字，后面加 *
     */
    NAME(Pattern.compile("(\\S)\\S+"), "$1**"),

    /**
     * 手机号：保留前三位和后四位
     */
    MOBILE(Pattern.compile("(\\d{3})\\d{4}(\\d{4})"), "$1****$2"),

    /**
     * 邮箱：保留首位和域名
     */
    EMAIL(Pattern.compile("(^.{1})[^@]*(@.*$)"), "$1****$2"),

    /**
     * 身份证：保留前六位和后四位
     */
    ID_CARD(Pattern.compile("(\\d{6})\\d+(\\d{4})"), "$1****$2"),

    /**
     * 地址：只保留省市区，隐藏后续详细地址
     */
    ADDRESS(null, null) {
        @Override
        public String mask(String target) {
            if (target == null || target.isEmpty()) {
                return target;
            }

            // 如果地址使用英文逗号分割，需要特殊处理
            if (target.contains(",")) {
                String[] split = target.split(",");

                // 保留 省市区
                if (split.length > 3) {
                    return split[0] + split[1] + split[2] + "****";
                }

                // 保留省市
                if (split.length > 2) {
                    return split[0] + split[1] + "****";
                }

                // 保留省
                return split[0] + "****";
            }

            // 严格版：市/区后面的全部隐藏
            return ADDRESS_PATTERN.matcher(target).replaceAll("$1****");
        }
    };

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^(.+?(?:区|县|旗|自治县|市))(.+)$");

    private final Pattern pattern;
    private final String replacement;

    SensitiveType(Pattern pattern, String replacement) {
        this.pattern = pattern;
        this.replacement = replacement;
    }

    public String mask(String target) {
        if (target == null || target.isEmpty()) {
            return target;
        }
        return pattern.matcher(target).replaceAll(replacement);
    }
}
