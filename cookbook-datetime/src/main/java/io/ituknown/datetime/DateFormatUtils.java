package io.ituknown.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * 日期格式化工具类
 * <p>
 * 提供常用的 {@link DateTimeFormatter} 常量及快捷格式化方法，
 * 涵盖 ISO 标准格式、自定义格式和带时区格式。
 *
 * <h3>常用占位符速查</h3>
 * <table>
 *   <tr><th>占位符</th><th>含义</th><th>示例</th></tr>
 *   <tr><td>{@code yyyy}</td><td>四位年份</td><td>2025</td></tr>
 *   <tr><td>{@code yy}</td><td>两位年份</td><td>25</td></tr>
 *   <tr><td>{@code MM}</td><td>月份（补零）</td><td>06</td></tr>
 *   <tr><td>{@code dd}</td><td>日（补零）</td><td>15</td></tr>
 *   <tr><td>{@code HH}</td><td>24 小时制（补零）</td><td>14</td></tr>
 *   <tr><td>{@code hh}</td><td>12 小时制（补零）</td><td>02</td></tr>
 *   <tr><td>{@code mm}</td><td>分钟（补零）</td><td>30</td></tr>
 *   <tr><td>{@code ss}</td><td>秒（补零）</td><td>45</td></tr>
 *   <tr><td>{@code SSS}</td><td>毫秒</td><td>123</td></tr>
 *   <tr><td>{@code X}</td><td>时区偏移 {@code +HH}，UTC 显示为 {@code Z}</td><td>+08 / Z</td></tr>
 *   <tr><td>{@code XX}</td><td>时区偏移 {@code +HHmm}，UTC 显示为 {@code Z}</td><td>+0800 / Z</td></tr>
 *   <tr><td>{@code XXX}</td><td>时区偏移 {@code +HH:mm}，UTC 显示为 {@code Z}</td><td>+08:00 / Z</td></tr>
 *   <tr><td>{@code OOOO}</td><td>本地化时区偏移</td><td>GMT+08:00</td></tr>
 *   <tr><td>{@code 'T' / 'Z'}</td><td>字面量字符</td><td>T / Z</td></tr>
 * </table>
 *
 * @author magicianlib@gmail.com
 * @see DateTimeFormatter
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

    // ----- 带偏移/UTC 的 ISO 8601 日期时间格式 -----
    //
    // 使用约束:
    //   1. 含时区偏移（XXX）的格式要求输入对象携带时区信息，适用类型:
    //      - java.time.ZonedDateTime
    //      - java.time.OffsetDateTime
    //      - java.time.Instant（配合 withZone 使用）
    //      LocalDateTime / LocalDate / LocalTime 不携带时区，传入将抛出 DateTimeException。
    //
    //   2. 带 'Z' 后缀的格式（ISO_INSTANT / ISO_UTC_DATE_TIME）内部固定 UTC 时区:
    //      - 传入 ZonedDateTime / OffsetDateTime 时会自动转换为 UTC 后再格式化。
    //      - 传入 Instant 直接格式化，无需转换。
    //      - 传入 LocalDateTime 同样会按 UTC 解释，但通常不应这样做——请使用带偏移的格式。

    /**
     * ISO 8601 带毫秒的偏移日期时间格式: {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX} → {@code 2011-12-03T10:15:30.000+08:00}
     *
     * @see #ISO_INSTANT 始终以 UTC + Z 输出的变体
     */
    public static final String ISO_OFFSET_DATE_TIME_MILLIS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(ISO_OFFSET_DATE_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * ISO 8601 即时格式（UTC 带 Z）: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} → {@code 2011-12-03T10:15:30.000Z}
     * <p>
     * 内部固定 UTC 时区，格式化时自动转换为 UTC 并追加 {@code Z} 后缀。
     * 推荐搭配 {@code Instant} 或 {@code ZonedDateTime} 使用。
     *
     * @see #ISO_OFFSET_DATE_TIME_MILLIS 保留原始时区偏移的变体
     */
    public static final String ISO_INSTANT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ofPattern(ISO_INSTANT_PATTERN, Locale.getDefault()).withZone(ZoneOffset.UTC);

    /**
     * ISO 8601 偏移日期时间格式（无毫秒）: {@code yyyy-MM-dd'T'HH:mm:ssXXX} → {@code 2011-12-03T10:15:30+08:00}
     *
     * @see #ISO_UTC_DATE_TIME 始终以 UTC + Z 输出的变体
     */
    public static final String ISO_OFFSET_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ofPattern(ISO_OFFSET_DATE_TIME_PATTERN, Locale.getDefault());

    /**
     * ISO 8601 UTC 日期时间格式（无毫秒，带 Z）: {@code yyyy-MM-dd'T'HH:mm:ss'Z'} → {@code 2011-12-03T10:15:30Z}
     * <p>
     * 内部固定 UTC 时区，格式化时自动转换为 UTC 并追加 {@code Z} 后缀。
     * 推荐搭配 {@code Instant} 或 {@code ZonedDateTime} 使用。
     *
     * @see #ISO_OFFSET_DATE_TIME 保留原始时区偏移的变体
     */
    public static final String ISO_UTC_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final DateTimeFormatter ISO_UTC_DATE_TIME = DateTimeFormatter.ofPattern(ISO_UTC_DATE_TIME_PATTERN, Locale.getDefault()).withZone(ZoneOffset.UTC);

    // ===== 自定义格式 =====

    /**
     * 日期时间格式: {@code yyyy-MM-dd HH:mm:ss} → {@code 2011-12-03 10:15:30}
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN, Locale.getDefault());

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
     * <p>
     * 示例输出:
     * <ul>
     *   <li>{@link TimestampStyle#FULL}: {@code 20250615103045}</li>
     *   <li>{@link TimestampStyle#MILLIS}: {@code 20250615103045123}</li>
     *   <li>{@link TimestampStyle#COMPACT}: {@code 250615103045}</li>
     * </ul>
     *
     * @param temporal 时间对象
     * @param style    时间戳风格
     */
    public static String timestamp(TemporalAccessor temporal, TimestampStyle style) {
        return style.formatter.format(temporal);
    }

    /**
     * 以当前本地时间生成时间戳字符串（{@link TimestampStyle#FULL}）
     * <p>
     * 示例输出: {@code 20250615103045}
     */
    public static String timestamp(TimestampStyle style) {
        return timestamp(LocalDateTime.now(), style);
    }

    /**
     * 以当前本地时间生成时间戳字符串（{@link TimestampStyle#FULL}）
     * <p>
     * 示例输出: {@code 20250615103045}
     */
    public static String timestamp() {
        return timestamp(TimestampStyle.FULL);
    }

    /**
     * 以当前 UTC 时间生成时间戳字符串
     * <p>
     * 示例输出:
     * <ul>
     *   <li>{@link TimestampStyle#FULL}: {@code 20250615023045}</li>
     *   <li>{@link TimestampStyle#MILLIS}: {@code 20250615023045123}</li>
     *   <li>{@link TimestampStyle#COMPACT}: {@code 250615023045}</li>
     * </ul>
     *
     * @param style 时间戳风格
     */
    public static String timestampUtc(TimestampStyle style) {
        return timestamp(LocalDateTime.now(ZoneOffset.UTC), style);
    }

    /**
     * 以当前 UTC 时间生成时间戳字符串（{@link TimestampStyle#FULL}）
     * <p>
     * 示例输出: {@code 20250615023045}
     */
    public static String timestampUtc() {
        return timestampUtc(TimestampStyle.FULL);
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
