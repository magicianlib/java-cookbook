package io.ituknown.jackson.serializer;

import java.util.function.Function;

/**
 * 脱敏类型
 *
 * @author magicianlib@gmail.com
 */
public enum SensitiveType {
    /**
     * 姓名：只保留第一个字，后面加 *
     */
    NAME(s -> s.replaceAll("(\\S)\\S+", "$1**")),

    /**
     * 手机号：保留前三位和后四位
     */
    MOBILE(s -> s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")),

    /**
     * 邮箱：保留首位和域名
     */
    EMAIL(s -> s.replaceAll("(^.{1})[^@]*(@.*$)", "$1****$2")),

    /**
     * 身份证：保留前六位和后四位
     */
    ID_CARD(s -> s.replaceAll("(\\d{6})\\d{8,10}(\\w{4})", "$1**********$2")),

    /**
     * 地址：只保留省市区，隐藏后续详细地址
     */
    ADDRESS(s -> {
        // 如果地址使用英文逗号分割，需要特殊处理
        if (s.contains(",")) {
            String[] split = s.split(",");

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

        // 宽松版：支持到 区（如 工业园区）
        //return s.replaceAll("^(.+?[区县])(.+)$", "$1****");

        // 严格版：市/区后面的全部隐藏
        return s.replaceAll("^(.+?(?:区|县|旗|自治县|市))(.+)$", "$1****");
    });

    private final Function<String, String> strategy;

    SensitiveType(Function<String, String> strategy) {
        this.strategy = strategy;
    }

    public String mask(String target) {
        return (target == null || target.isEmpty()) ? target : strategy.apply(target);
    }
}
