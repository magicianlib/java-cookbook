package io.ituknown.datetime;

/**
 * 时间戳输出风格，用于 {@link DateFormatUtils#timestamp} 系列方法。
 *
 * @see DateFormatUtils#timestamp(java.time.temporal.TemporalAccessor, TimestampStyle)
 * @see DateFormatUtils#timestampUtc(TimestampStyle)
 */
public enum TimestampStyle {

    /** 四位年份: {@code yyyyMMddHHmmss} → {@code 20250615103045} */
    FULL,

    /** 四位年份 + 毫秒: {@code yyyyMMddHHmmssSSS} → {@code 20250615103045123} */
    MILLIS,

    /** 两位年份: {@code yyMMddHHmmss} → {@code 250615103045} */
    COMPACT
}
