package io.ituknown.datetime;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * 日期格式化工具类
 * <p>
 * 提供常用的 {@link DateTimeFormatter} 常量及快捷格式化方法，
 * 涵盖紧凑格式、标准日期时间格式、ISO 8601 带偏移/UTC 格式。
 *
 * <h3>格式分类</h3>
 * <ul>
 *   <li><b>紧凑格式</b>（COMPACT_*）—— 无分隔符，适用于日志文件名、排序键等场景</li>
 *   <li><b>标准格式</b>（DATE_TIME_* / DATE / TIME_*）—— 短横线分隔日期、冒号分隔时间</li>
 *   <li><b>斜杠格式</b>（SLASH_*）—— 斜杠分隔日期、冒号分隔时间</li>
 *   <li><b>ISO 偏移格式</b>（ISO_OFFSET_*）—— ISO 8601 格式 + 时区偏移 ({@code +HH:mm})</li>
 *   <li><b>ISO UTC 格式</b>（ISO_UTC_*）—— ISO 8601 格式 + 固定 UTC 时区，以 {@code Z} 结尾</li>
 * </ul>
 *
 * <h3>使用约束</h3>
 * <ul>
 *   <li>不含时区偏移（XXX）的格式适用于 {@code LocalDateTime}、{@code LocalDate}、{@code LocalTime}</li>
 *   <li>含时区偏移（XXX）的格式要求输入携带时区信息，适用于 {@code OffsetDateTime}、{@code ZonedDateTime}、{@code OffsetTime}</li>
 *   <li>带 {@code 'Z'} 后缀的格式内部固定 UTC 时区，适用于 {@code Instant}、{@code ZonedDateTime}、{@code OffsetDateTime}</li>
 * </ul>
 *
 * <h3>常用占位符速查</h3>
 * <table>
 *   <tr><th>占位符</th><th>含义</th><th>示例</th></tr>
 *   <tr><td>{@code yyyy}</td><td>四位年份</td><td>2025</td></tr>
 *   <tr><td>{@code yy}</td><td>两位年份</td><td>25</td></tr>
 *   <tr><td>{@code MM}</td><td>月份（补零）</td><td>06</td></tr>
 *   <tr><td>{@code dd}</td><td>日（补零）</td><td>15</td></tr>
 *   <tr><td>{@code HH}</td><td>24 小时制（补零）</td><td>14</td></tr>
 *   <tr><td>{@code mm}</td><td>分钟（补零）</td><td>30</td></tr>
 *   <tr><td>{@code ss}</td><td>秒（补零）</td><td>45</td></tr>
 *   <tr><td>{@code SSS}</td><td>毫秒</td><td>123</td></tr>
 *   <tr><td>{@code XXX}</td><td>时区偏移 {@code +HH:mm}，UTC 显示为 {@code Z}</td><td>+08:00 / Z</td></tr>
 *   <tr><td>{@code 'T' / 'Z'}</td><td>字面量字符</td><td>T / Z</td></tr>
 * </table>
 *
 * @author magicianlib@gmail.com
 * @see DateTimeFormatter
 * @see TimestampStyle
 */
public final class DateFormatUtils {

    private DateFormatUtils() {
    }

    // ===== 紧凑格式（无分隔符） =====
    // 适用类型: LocalDateTime
    // 常用于日志文件名、排序键等场景

    /**
     * 紧凑日期时间 + 毫秒: {@code yyyyMMddHHmmssSSS}
     * <p>
     * 输出示例: {@code 20250615101530123}
     * <p>
     * 适用类型: {@link java.time.LocalDateTime LocalDateTime}
     */
    public static final String COMPACT_DATE_TIME_MILLIS_PATTERN = "yyyyMMddHHmmssSSS";
    public static final DateTimeFormatter COMPACT_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(COMPACT_DATE_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * 紧凑日期时间 + 毫秒（两位年份）: {@code yyMMddHHmmssSSS}
     * <p>
     * 输出示例: {@code 250615101530123}
     * <p>
     * 适用类型: {@link java.time.LocalDateTime LocalDateTime}
     */
    public static final String COMPACT_DATE_TIME_MILLIS_SHORT_PATTERN = "yyMMddHHmmssSSS";
    public static final DateTimeFormatter COMPACT_DATE_TIME_MILLIS_SHORT = DateTimeFormatter.ofPattern(COMPACT_DATE_TIME_MILLIS_SHORT_PATTERN, Locale.getDefault());

    /**
     * 紧凑日期时间: {@code yyyyMMddHHmmss}
     * <p>
     * 输出示例: {@code 20250615101530}
     * <p>
     * 适用类型: {@link java.time.LocalDateTime LocalDateTime}
     */
    public static final String COMPACT_DATE_TIME_PATTERN = "yyyyMMddHHmmss";
    public static final DateTimeFormatter COMPACT_DATE_TIME = DateTimeFormatter.ofPattern(COMPACT_DATE_TIME_PATTERN, Locale.getDefault());

    /**
     * 紧凑日期时间（两位年份）: {@code yyMMddHHmmss}
     * <p>
     * 输出示例: {@code 250615101530}
     * <p>
     * 适用类型: {@link java.time.LocalDateTime LocalDateTime}
     */
    public static final String COMPACT_DATE_TIME_SHORT_PATTERN = "yyMMddHHmmss";
    public static final DateTimeFormatter COMPACT_DATE_TIME_SHORT = DateTimeFormatter.ofPattern(COMPACT_DATE_TIME_SHORT_PATTERN, Locale.getDefault());

    // ===== 标准格式（短横线分隔日期，冒号分隔时间） =====
    // 适用类型: LocalDateTime, LocalDate, LocalTime

    /**
     * 日期时间 + 毫秒: {@code yyyy-MM-dd HH:mm:ss.SSS}
     * <p>
     * 输出示例: {@code 2025-06-15 10:15:30.123}
     * <p>
     * 适用类型: {@link java.time.LocalDateTime LocalDateTime}
     */
    public static final String DATE_TIME_MILLIS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final DateTimeFormatter DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(DATE_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * 日期时间: {@code yyyy-MM-dd HH:mm:ss}
     * <p>
     * 输出示例: {@code 2025-06-15 10:15:30}
     * <p>
     * 适用类型: {@link java.time.LocalDateTime LocalDateTime}
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN, Locale.getDefault());

    /**
     * 日期: {@code yyyy-MM-dd}
     * <p>
     * 输出示例: {@code 2025-06-15}
     * <p>
     * 适用类型: {@link java.time.LocalDate LocalDate}
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.getDefault());

    // ===== 斜杠格式（斜杠分隔日期，冒号分隔时间） =====
    // 适用类型: LocalDateTime, LocalDate, LocalTime

    /**
     * 日期时间 + 毫秒: {@code yyyy/MM/dd HH:mm:ss.SSS}
     * <p>
     * 输出示例: {@code 2025/06/15 10:15:30.123}
     * <p>
     * 适用类型: {@link java.time.LocalDateTime LocalDateTime}
     */
    public static final String SLASH_DATE_TIME_MILLIS_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS";
    public static final DateTimeFormatter SLASH_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(SLASH_DATE_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * 日期时间: {@code yyyy/MM/dd HH:mm:ss}
     * <p>
     * 输出示例: {@code 2025/06/15 10:15:30}
     * <p>
     * 适用类型: {@link java.time.LocalDateTime LocalDateTime}
     */
    public static final String SLASH_DATE_TIME_PATTERN = "yyyy/MM/dd HH:mm:ss";
    public static final DateTimeFormatter SLASH_DATE_TIME = DateTimeFormatter.ofPattern(SLASH_DATE_TIME_PATTERN, Locale.getDefault());

    /**
     * 日期: {@code yyyy/MM/dd}
     * <p>
     * 输出示例: {@code 2025/06/15}
     * <p>
     * 适用类型: {@link java.time.LocalDate LocalDate}
     */
    public static final String SLASH_DATE_PATTERN = "yyyy/MM/dd";
    public static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern(SLASH_DATE_PATTERN, Locale.getDefault());

    // ===== 时间格式 =====
    // 适用类型: LocalTime

    /**
     * 时间 + 毫秒: {@code HH:mm:ss.SSS}
     * <p>
     * 输出示例: {@code 10:15:30.123}
     * <p>
     * 适用类型: {@link java.time.LocalTime LocalTime}
     */
    public static final String TIME_MILLIS_PATTERN = "HH:mm:ss.SSS";
    public static final DateTimeFormatter TIME_MILLIS = DateTimeFormatter.ofPattern(TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * 时间: {@code HH:mm:ss}
     * <p>
     * 输出示例: {@code 10:15:30}
     * <p>
     * 适用类型: {@link java.time.LocalTime LocalTime}
     */
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern(TIME_PATTERN, Locale.getDefault());

    // ===== ISO 8601 带时区偏移格式（XXX） =====
    //
    // 使用约束:
    //   含时区偏移（XXX）的格式要求输入对象携带时区信息:
    //     - java.time.OffsetDateTime
    //     - java.time.ZonedDateTime
    //     - java.time.OffsetTime
    //   LocalDateTime / LocalDate / LocalTime 不携带时区，传入将抛出 DateTimeException。

    /**
     * ISO 偏移日期时间 + 毫秒: {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX}
     * <p>
     * 输出示例: {@code 2025-06-15T10:15:30.123+08:00}
     * <p>
     * 适用类型: {@link java.time.OffsetDateTime OffsetDateTime}, {@link java.time.ZonedDateTime ZonedDateTime}
     *
     * @see #ISO_UTC_DATE_TIME_MILLIS 始终以 UTC + Z 输出的变体
     */
    public static final String ISO_OFFSET_DATE_TIME_MILLIS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(ISO_OFFSET_DATE_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * ISO 偏移日期时间: {@code yyyy-MM-dd'T'HH:mm:ssXXX}
     * <p>
     * 输出示例: {@code 2025-06-15T10:15:30+08:00}
     * <p>
     * 适用类型: {@link java.time.OffsetDateTime OffsetDateTime}, {@link java.time.ZonedDateTime ZonedDateTime}
     *
     * @see #ISO_UTC_DATE_TIME 始终以 UTC + Z 输出的变体
     */
    public static final String ISO_OFFSET_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ofPattern(ISO_OFFSET_DATE_TIME_PATTERN, Locale.getDefault());

    /**
     * ISO 偏移日期时间 + 毫秒（斜杠分隔）: {@code yyyy/MM/dd'T'HH:mm:ss.SSSXXX}
     * <p>
     * 输出示例: {@code 2025/06/15T10:15:30.123+08:00}
     * <p>
     * 适用类型: {@link java.time.OffsetDateTime OffsetDateTime}, {@link java.time.ZonedDateTime ZonedDateTime}
     *
     * @see #SLASH_ISO_UTC_DATE_TIME_MILLIS 始终以 UTC + Z 输出的变体
     */
    public static final String SLASH_ISO_OFFSET_DATE_TIME_MILLIS_PATTERN = "yyyy/MM/dd'T'HH:mm:ss.SSSXXX";
    public static final DateTimeFormatter SLASH_ISO_OFFSET_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(SLASH_ISO_OFFSET_DATE_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * ISO 偏移日期时间（斜杠分隔）: {@code yyyy/MM/dd'T'HH:mm:ssXXX}
     * <p>
     * 输出示例: {@code 2025/06/15T10:15:30+08:00}
     * <p>
     * 适用类型: {@link java.time.OffsetDateTime OffsetDateTime}, {@link java.time.ZonedDateTime ZonedDateTime}
     *
     * @see #SLASH_ISO_UTC_DATE_TIME 始终以 UTC + Z 输出的变体
     */
    public static final String SLASH_ISO_OFFSET_DATE_TIME_PATTERN = "yyyy/MM/dd'T'HH:mm:ssXXX";
    public static final DateTimeFormatter SLASH_ISO_OFFSET_DATE_TIME = DateTimeFormatter.ofPattern(SLASH_ISO_OFFSET_DATE_TIME_PATTERN, Locale.getDefault());

    /**
     * ISO 偏移时间 + 毫秒: {@code HH:mm:ss.SSSXXX}
     * <p>
     * 输出示例: {@code 10:15:30.123+08:00}
     * <p>
     * 适用类型: {@link java.time.OffsetTime OffsetTime}
     *
     * @see #ISO_UTC_TIME_MILLIS 始终以 UTC + Z 输出的变体
     */
    public static final String ISO_OFFSET_TIME_MILLIS_PATTERN = "HH:mm:ss.SSSXXX";
    public static final DateTimeFormatter ISO_OFFSET_TIME_MILLIS = DateTimeFormatter.ofPattern(ISO_OFFSET_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * ISO 偏移时间: {@code HH:mm:ssXXX}
     * <p>
     * 输出示例: {@code 10:15:30+08:00}
     * <p>
     * 适用类型: {@link java.time.OffsetTime OffsetTime}
     *
     * @see #ISO_UTC_TIME 始终以 UTC + Z 输出的变体
     */
    public static final String ISO_OFFSET_TIME_PATTERN = "HH:mm:ssXXX";
    public static final DateTimeFormatter ISO_OFFSET_TIME = DateTimeFormatter.ofPattern(ISO_OFFSET_TIME_PATTERN, Locale.getDefault());

    // ===== ISO 8601 UTC Zulu 格式（固定 UTC 时区，以 Z 结尾） =====
    //
    // 使用约束:
    //   内部固定 UTC 时区（ZoneOffset.UTC），格式化时自动转换为 UTC 并追加 Z 后缀。
    //   适用类型:
    //     - java.time.Instant
    //     - java.time.ZonedDateTime
    //     - java.time.OffsetDateTime

    /**
     * ISO UTC 日期时间 + 毫秒: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}
     * <p>
     * 输出示例: {@code 2025-06-15T02:15:30.123Z}
     * <p>
     * 适用类型: {@link java.time.Instant Instant}, {@link java.time.ZonedDateTime ZonedDateTime},
     * {@link java.time.OffsetDateTime OffsetDateTime}
     *
     * @see #ISO_OFFSET_DATE_TIME_MILLIS 保留原始时区偏移的变体
     */
    public static final String ISO_UTC_DATE_TIME_MILLIS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final DateTimeFormatter ISO_UTC_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(ISO_UTC_DATE_TIME_MILLIS_PATTERN, Locale.getDefault()).withZone(ZoneOffset.UTC);

    /**
     * ISO UTC 日期时间: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}
     * <p>
     * 输出示例: {@code 2025-06-15T02:15:30Z}
     * <p>
     * 适用类型: {@link java.time.Instant Instant}, {@link java.time.ZonedDateTime ZonedDateTime},
     * {@link java.time.OffsetDateTime OffsetDateTime}
     *
     * @see #ISO_OFFSET_DATE_TIME 保留原始时区偏移的变体
     */
    public static final String ISO_UTC_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final DateTimeFormatter ISO_UTC_DATE_TIME = DateTimeFormatter.ofPattern(ISO_UTC_DATE_TIME_PATTERN, Locale.getDefault()).withZone(ZoneOffset.UTC);

    /**
     * ISO UTC 日期时间 + 毫秒（斜杠分隔）: {@code yyyy/MM/dd'T'HH:mm:ss.SSS'Z'}
     * <p>
     * 输出示例: {@code 2025/06/15T02:15:30.123Z}
     * <p>
     * 适用类型: {@link java.time.Instant Instant}, {@link java.time.ZonedDateTime ZonedDateTime},
     * {@link java.time.OffsetDateTime OffsetDateTime}
     *
     * @see #SLASH_ISO_OFFSET_DATE_TIME_MILLIS 保留原始时区偏移的变体
     */
    public static final String SLASH_ISO_UTC_DATE_TIME_MILLIS_PATTERN = "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'";
    public static final DateTimeFormatter SLASH_ISO_UTC_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(SLASH_ISO_UTC_DATE_TIME_MILLIS_PATTERN, Locale.getDefault()).withZone(ZoneOffset.UTC);

    /**
     * ISO UTC 日期时间（斜杠分隔）: {@code yyyy/MM/dd'T'HH:mm:ss'Z'}
     * <p>
     * 输出示例: {@code 2025/06/15T02:15:30Z}
     * <p>
     * 适用类型: {@link java.time.Instant Instant}, {@link java.time.ZonedDateTime ZonedDateTime},
     * {@link java.time.OffsetDateTime OffsetDateTime}
     *
     * @see #SLASH_ISO_OFFSET_DATE_TIME 保留原始时区偏移的变体
     */
    public static final String SLASH_ISO_UTC_DATE_TIME_PATTERN = "yyyy/MM/dd'T'HH:mm:ss'Z'";
    public static final DateTimeFormatter SLASH_ISO_UTC_DATE_TIME = DateTimeFormatter.ofPattern(SLASH_ISO_UTC_DATE_TIME_PATTERN, Locale.getDefault()).withZone(ZoneOffset.UTC);

    /**
     * ISO UTC 时间 + 毫秒: {@code HH:mm:ss.SSS'Z'}
     * <p>
     * 输出示例: {@code 02:15:30.123Z}
     * <p>
     * 适用类型: {@link java.time.Instant Instant}, {@link java.time.OffsetTime OffsetTime}
     *
     * @see #ISO_OFFSET_TIME_MILLIS 保留原始时区偏移的变体
     */
    public static final String ISO_UTC_TIME_MILLIS_PATTERN = "HH:mm:ss.SSS'Z'";
    public static final DateTimeFormatter ISO_UTC_TIME_MILLIS = DateTimeFormatter.ofPattern(ISO_UTC_TIME_MILLIS_PATTERN, Locale.getDefault()).withZone(ZoneOffset.UTC);

    /**
     * ISO UTC 时间: {@code HH:mm:ss'Z'}
     * <p>
     * 输出示例: {@code 02:15:30Z}
     * <p>
     * 适用类型: {@link java.time.Instant Instant}, {@link java.time.OffsetTime OffsetTime}
     *
     * @see #ISO_OFFSET_TIME 保留原始时区偏移的变体
     */
    public static final String ISO_UTC_TIME_PATTERN = "HH:mm:ss'Z'";
    public static final DateTimeFormatter ISO_UTC_TIME = DateTimeFormatter.ofPattern(ISO_UTC_TIME_PATTERN, Locale.getDefault()).withZone(ZoneOffset.UTC);

    // ===== 带时区 ID 格式（偏移 + ZoneId） =====
    //
    // 使用约束:
    //   同时输出时区偏移（XXX）和时区 ID（VV），仅适用于携带完整时区信息的类型:
    //     - java.time.ZonedDateTime
    //   OffsetDateTime / OffsetTime / LocalDateTime 等类型不携带 ZoneId，传入将抛出 DateTimeException。

    /**
     * 日期时间 + 毫秒 + 时区偏移 + 时区 ID: {@code yyyy-MM-dd HH:mm:ss.SSS XXX '['VV']'}
     * <p>
     * 输出示例: {@code 2025-06-15 10:15:30.123 +08:00 [Asia/Shanghai]}
     * <p>
     * 适用类型: {@link java.time.ZonedDateTime ZonedDateTime}
     *
     * @see #SLASH_ZONED_DATE_TIME_MILLIS 斜杠分隔日期的变体
     */
    public static final String ZONED_DATE_TIME_MILLIS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS XXX '['VV']'";
    public static final DateTimeFormatter ZONED_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(ZONED_DATE_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * 日期时间 + 时区偏移 + 时区 ID: {@code yyyy-MM-dd HH:mm:ss XXX '['VV']'}
     * <p>
     * 输出示例: {@code 2025-06-15 10:15:30 +08:00 [Asia/Shanghai]}
     * <p>
     * 适用类型: {@link java.time.ZonedDateTime ZonedDateTime}
     *
     * @see #SLASH_ZONED_DATE_TIME 斜杠分隔日期的变体
     */
    public static final String ZONED_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss XXX '['VV']'";
    public static final DateTimeFormatter ZONED_DATE_TIME = DateTimeFormatter.ofPattern(ZONED_DATE_TIME_PATTERN, Locale.getDefault());

    /**
     * 日期时间 + 毫秒 + 时区偏移 + 时区 ID（斜杠分隔）: {@code yyyy/MM/dd HH:mm:ss.SSS XXX '['VV']'}
     * <p>
     * 输出示例: {@code 2025/06/15 10:15:30.123 +08:00 [Asia/Shanghai]}
     * <p>
     * 适用类型: {@link java.time.ZonedDateTime ZonedDateTime}
     *
     * @see #ZONED_DATE_TIME_MILLIS 短横线分隔日期的变体
     */
    public static final String SLASH_ZONED_DATE_TIME_MILLIS_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS XXX '['VV']'";
    public static final DateTimeFormatter SLASH_ZONED_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(SLASH_ZONED_DATE_TIME_MILLIS_PATTERN, Locale.getDefault());

    /**
     * 日期时间 + 时区偏移 + 时区 ID（斜杠分隔）: {@code yyyy/MM/dd HH:mm:ss XXX '['VV']'}
     * <p>
     * 输出示例: {@code 2025/06/15 10:15:30 +08:00 [Asia/Shanghai]}
     * <p>
     * 适用类型: {@link java.time.ZonedDateTime ZonedDateTime}
     *
     * @see #ZONED_DATE_TIME 短横线分隔日期的变体
     */
    public static final String SLASH_ZONED_DATE_TIME_PATTERN = "yyyy/MM/dd HH:mm:ss XXX '['VV']'";
    public static final DateTimeFormatter SLASH_ZONED_DATE_TIME = DateTimeFormatter.ofPattern(SLASH_ZONED_DATE_TIME_PATTERN, Locale.getDefault());

    // ===== 快捷方法 =====

    /**
     * 格式化为时间戳字符串
     * <p>
     * 支持的时间对象类型:
     * <ul>
     *   <li>{@link LocalDateTime}</li>
     *   <li>{@link java.time.ZonedDateTime ZonedDateTime}</li>
     *   <li>{@link java.time.OffsetDateTime OffsetDateTime}</li>
     * </ul>
     * {@link java.time.Instant Instant}、{@link java.time.LocalDate LocalDate}、{@link java.time.LocalTime LocalTime}
     * 不支持，传入将抛出 {@link java.time.DateTimeException}。
     * <p>
     * 示例输出:
     * <ul>
     *   <li>{@link TimestampStyle#FULL}: {@code 20250615103045}</li>
     *   <li>{@link TimestampStyle#MILLIS}: {@code 20250615103045123}</li>
     *   <li>{@link TimestampStyle#COMPACT}: {@code 250615103045}</li>
     *   <li>{@link TimestampStyle#COMPACT_MILLIS}: {@code 250615103045123}</li>
     * </ul>
     *
     * @param temporal 时间对象
     * @param style    时间戳风格
     */
    public static String timestamp(TemporalAccessor temporal, TimestampStyle style) {
        return style.formatter.format(temporal);
    }

    /**
     * 以指定时间生成时间戳字符串（{@link TimestampStyle#FULL}）
     * <p>
     * 支持的时间对象类型见 {@link #timestamp(TemporalAccessor, TimestampStyle)}。
     * <p>
     * 示例输出: {@code 20250615103045}
     *
     * @param temporal 时间对象
     */
    public static String timestamp(TemporalAccessor temporal) {
        return timestamp(temporal, TimestampStyle.FULL);
    }

    /**
     * 以当前本地时间生成时间戳字符串
     *
     * @param style 时间戳风格
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
     * 以指定时间生成 UTC 时间戳字符串（{@link TimestampStyle#FULL}）
     * <p>
     * 支持的时间对象类型见 {@link #timestamp(TemporalAccessor, TimestampStyle)}。
     * <p>
     * 示例输出: {@code 20250615023045}
     *
     * @param temporal 时间对象
     */
    public static String timestampUtc(TemporalAccessor temporal) {
        return timestamp(temporal, TimestampStyle.FULL);
    }

    /**
     * 以当前 UTC 时间生成时间戳字符串（{@link TimestampStyle#FULL}）
     * <p>
     * 示例输出: {@code 20250615023045}
     */
    public static String timestampUtc() {
        return timestampUtc(TimestampStyle.FULL);
    }
}
