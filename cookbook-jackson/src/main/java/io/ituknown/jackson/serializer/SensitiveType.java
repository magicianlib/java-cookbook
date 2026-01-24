package io.ituknown.jackson.serializer;

import java.util.function.Function;

/**
 * 脱敏类型
 *
 * @author magicianlib@gmail.com
 */
public enum SensitiveType {
    /**
     * 用户名称：只保留第一个字，后面加 *
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
    ID_CARD(s -> s.replaceAll("(\\d{6})\\d{8,10}(\\w{4})", "$1**********$2"));

    private final Function<String, String> strategy;

    SensitiveType(Function<String, String> strategy) {
        this.strategy = strategy;
    }

    public String mask(String target) {
        return (target == null || target.isEmpty()) ? target : strategy.apply(target);
    }
}