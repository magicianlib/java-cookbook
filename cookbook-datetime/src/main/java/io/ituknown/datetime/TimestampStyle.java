package io.ituknown.datetime;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 时间戳输出风格，用于 {@link DateFormatUtils#timestamp} 系列方法。
 *
 * @see DateFormatUtils#timestamp(java.time.temporal.TemporalAccessor, TimestampStyle)
 * @see DateFormatUtils#timestampUtc(TimestampStyle)
 */
public enum TimestampStyle {

    /** 四位年份: {@code yyyyMMddHHmmss} → {@code 20250615103045} */
    FULL("yyyyMMddHHmmss"),

    /** 四位年份 + 毫秒: {@code yyyyMMddHHmmssSSS} → {@code 20250615103045123} */
    MILLIS("yyyyMMddHHmmssSSS"),

    /** 两位年份: {@code yyMMddHHmmss} → {@code 250615103045} */
    COMPACT("yyMMddHHmmss"),

    /** 两位年份 + 毫秒: {@code yyMMddHHmmssSSS} → {@code 250615103045123} */
    COMPACT_MILLIS("yyMMddHHmmssSSS");

    /** 格式模式 */
    public final String pattern;

    /** 格式器 */
    public final DateTimeFormatter formatter;

    TimestampStyle(String pattern) {
        this.pattern = pattern;
        this.formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault());
    }
}
