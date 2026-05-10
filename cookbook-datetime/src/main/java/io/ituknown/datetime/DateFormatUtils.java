package io.ituknown.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * 日期格式化工具类
 * <p>
 * 提供常用的 {@link DateTimeFormatter} 常量及快捷格式化方法，
 * 涵盖 ISO 标准格式、自定义格式和带时区格式。
 *
 * @author magicianlib@gmail.com
 */
public final class DateFormatUtils {

    private DateFormatUtils() {
    }

    // ===== ISO 8601 格式 =====

    /**
     * ISO 8601 基本日期格式（无分隔符）: {@code yyyyMMdd} → {@code 20111203}
     */
    public static final String BASIC_DATE_PATTERN = "yyyyMMdd";
    public static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.ofPattern(BASIC_DATE_PATTERN, Locale.getDefault());

    /**
     * ISO 8601 本地日期格式: {@code yyyy-MM-dd} → {@code 2011-12-03}
     */
    public static final String ISO_LOCAL_DATE_PATTERN = "yyyy-MM-dd";
    public static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ofPattern(ISO_LOCAL_DATE_PATTERN, Locale.getDefault());

    /**
     * ISO 8601 本地时间格式: {@code HH:mm:ss} → {@code 10:15:30}
     */
    public static final String ISO_LOCAL_TIME_PATTERN = "HH:mm:ss";
    public static final DateTimeFormatter ISO_LOCAL_TIME = DateTimeFormatter.ofPattern(ISO_LOCAL_TIME_PATTERN, Locale.getDefault());

    /**
     * ISO 8601 本地日期时间格式: {@code yyyy-MM-dd'T'HH:mm:ss} → {@code 2011-12-03T10:15:30}
     */
    public static final String ISO_LOCAL_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ofPattern(ISO_LOCAL_DATE_TIME_PATTERN, Locale.getDefault());

    // ===== 自定义格式 =====

    /**
     * 日期时间格式: {@code yyyy-MM-dd HH:mm:ss} → {@code 2011-12-03 10:15:30}
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN, Locale.getDefault());

    /**
     * 时间戳格式: {@code yyyyMMddHHmmss} → {@code 20111203101530}
     */
    public static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmss";
    public static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN, Locale.getDefault());

    /**
     * 简化时间戳格式（两位年份）: {@code yyMMddHHmmss} → {@code 111203101530}
     */
    public static final String SIMPLIFY_TIMESTAMP_PATTERN = "yyMMddHHmmss";
    public static final DateTimeFormatter SIMPLIFY_TIMESTAMP = DateTimeFormatter.ofPattern(SIMPLIFY_TIMESTAMP_PATTERN, Locale.getDefault());

    /**
     * 简化日期格式（两位年份）: {@code yyMMdd} → {@code 111203}
     */
    public static final String SIMPLIFY_DATE_PATTERN = "yyMMdd";
    public static final DateTimeFormatter SIMPLIFY_DATE = DateTimeFormatter.ofPattern(SIMPLIFY_DATE_PATTERN, Locale.getDefault());

    // ===== 带时区格式 =====

    /**
     * 时间 + 时区格式: {@code HH:mm:ss OOOO} → {@code 10:15:30 GMT+08:00}
     */
    public static final String TIME_ZONE_PATTERN = "HH:mm:ss OOOO";
    public static final DateTimeFormatter TIME_ZONE = DateTimeFormatter.ofPattern(TIME_ZONE_PATTERN, Locale.getDefault());

    /**
     * 日期时间 + 时区格式: {@code yyyy-MM-dd HH:mm:ss OOOO} → {@code 2011-12-03 10:30:45 GMT+08:00}
     */
    public static final String DATE_TIME_ZONE_PATTERN = "yyyy-MM-dd HH:mm:ss OOOO";
    public static final DateTimeFormatter DATE_TIME_ZONE = DateTimeFormatter.ofPattern(DATE_TIME_ZONE_PATTERN, Locale.getDefault());

    // ===== 快捷方法 =====

    /**
     * 格式化为时间戳字符串
     *
     * @param temporal 时间对象
     * @param simplify 是否使用两位年份
     */
    public static String timestamp(TemporalAccessor temporal, boolean simplify) {
        return (simplify ? SIMPLIFY_TIMESTAMP : TIMESTAMP).format(temporal);
    }

    /**
     * 以当前时间生成时间戳字符串
     *
     * @param simplify 是否使用两位年份
     */
    public static String timestamp(boolean simplify) {
        return timestamp(LocalDateTime.now(), simplify);
    }

    /**
     * 以当前时间生成完整时间戳字符串
     */
    public static String timestamp() {
        return timestamp(LocalDateTime.now(), false);
    }

    /**
     * 格式化为日期字符串
     *
     * @param date     日期对象
     * @param simplify 是否使用两位年份
     */
    public static String date(LocalDate date, boolean simplify) {
        return (simplify ? SIMPLIFY_DATE : BASIC_DATE).format(date);
    }

    /**
     * 以当前日期生成日期字符串
     *
     * @param simplify 是否使用两位年份
     */
    public static String date(boolean simplify) {
        return date(LocalDate.now(), simplify);
    }

    /**
     * 以当前日期生成完整日期字符串
     */
    public static String date() {
        return date(LocalDate.now(), false);
    }
}
