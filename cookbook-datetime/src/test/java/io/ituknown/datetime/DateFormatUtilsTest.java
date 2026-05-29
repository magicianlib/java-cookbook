package io.ituknown.datetime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static io.ituknown.datetime.TimestampStyle.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateFormatUtils")
public class DateFormatUtilsTest {

    // 公共测试数据

    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2025, 6, 15, 10, 30, 45, 123_000_000);
    private static final LocalDateTime DATE_TIME_NO_MILLIS = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
    private static final LocalDate DATE = LocalDate.of(2025, 6, 15);
    private static final LocalTime TIME_NO_MILLIS = LocalTime.of(10, 30, 45);
    private static final LocalTime TIME_WITH_MILLIS = LocalTime.of(10, 30, 45, 123_000_000);
    private static final ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
    private static final ZonedDateTime ZONED_DATE_TIME_MILLIS = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 123_000_000, ZoneId.of("Asia/Shanghai"));
    private static final ZonedDateTime ZONED_DATE_TIME_UTC = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.UTC);
    private static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.ofHours(8));
    private static final OffsetDateTime OFFSET_DATE_TIME_UTC = OffsetDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.UTC);
    private static final OffsetTime OFFSET_TIME = OffsetTime.of(10, 30, 45, 0, ZoneOffset.ofHours(8));

    // ===== 紧凑格式 =====

    @Nested
    @DisplayName("紧凑格式（COMPACT_*）")
    class CompactFormatTest {

        @Test
        @DisplayName("COMPACT_DATE_TIME_MILLIS")
        void testCompactDateTimeMillis() {
            assertEquals("20250615103045123", DateFormatUtils.COMPACT_DATE_TIME_MILLIS.format(DATE_TIME));
        }

        @Test
        @DisplayName("COMPACT_DATE_TIME_MILLIS_SHORT")
        void testCompactDateTimeMillisShort() {
            assertEquals("250615103045123", DateFormatUtils.COMPACT_DATE_TIME_MILLIS_SHORT.format(DATE_TIME));
        }

        @Test
        @DisplayName("COMPACT_DATE_TIME")
        void testCompactDateTime() {
            assertEquals("20250615103045", DateFormatUtils.COMPACT_DATE_TIME.format(DATE_TIME_NO_MILLIS));
        }

        @Test
        @DisplayName("COMPACT_DATE_TIME_SHORT")
        void testCompactDateTimeShort() {
            assertEquals("250615103045", DateFormatUtils.COMPACT_DATE_TIME_SHORT.format(DATE_TIME_NO_MILLIS));
        }
    }

    // ===== 标准格式 =====

    @Nested
    @DisplayName("标准格式（DATE_TIME_* / DATE）")
    class StandardFormatTest {

        @Test
        @DisplayName("DATE_TIME_MILLIS")
        void testDateTimeMillis() {
            assertEquals("2025-06-15 10:30:45.123", DateFormatUtils.DATE_TIME_MILLIS.format(DATE_TIME));
        }

        @Test
        @DisplayName("DATE_TIME")
        void testDateTime() {
            assertEquals("2025-06-15 10:30:45", DateFormatUtils.DATE_TIME.format(DATE_TIME_NO_MILLIS));
        }

        @Test
        @DisplayName("DATE")
        void testDate() {
            assertEquals("2025-06-15", DateFormatUtils.DATE.format(DATE));
        }
    }

    // ===== 斜杠格式 =====

    @Nested
    @DisplayName("斜杠格式（SLASH_*）")
    class SlashFormatTest {

        @Test
        @DisplayName("SLASH_DATE_TIME_MILLIS")
        void testSlashDateTimeMillis() {
            assertEquals("2025/06/15 10:30:45.123", DateFormatUtils.SLASH_DATE_TIME_MILLIS.format(DATE_TIME));
        }

        @Test
        @DisplayName("SLASH_DATE_TIME")
        void testSlashDateTime() {
            assertEquals("2025/06/15 10:30:45", DateFormatUtils.SLASH_DATE_TIME.format(DATE_TIME_NO_MILLIS));
        }

        @Test
        @DisplayName("SLASH_DATE")
        void testSlashDate() {
            assertEquals("2025/06/15", DateFormatUtils.SLASH_DATE.format(DATE));
        }
    }

    // ===== 时间格式 =====

    @Nested
    @DisplayName("时间格式（TIME_*）")
    class TimeFormatTest {

        @Test
        @DisplayName("TIME_MILLIS")
        void testTimeMillis() {
            assertEquals("10:30:45.123", DateFormatUtils.TIME_MILLIS.format(TIME_WITH_MILLIS));
        }

        @Test
        @DisplayName("TIME")
        void testTime() {
            assertEquals("10:30:45", DateFormatUtils.TIME.format(TIME_NO_MILLIS));
        }
    }

    // ===== ISO 偏移格式 =====

    @Nested
    @DisplayName("ISO 偏移格式（ISO_OFFSET_*）")
    class IsoOffsetFormatTest {

        @Test
        @DisplayName("ISO_OFFSET_DATE_TIME_MILLIS")
        void testIsoOffsetDateTimeMillis() {
            assertEquals("2025-06-15T10:30:45.000+08:00", DateFormatUtils.ISO_OFFSET_DATE_TIME_MILLIS.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_OFFSET_DATE_TIME_MILLIS - UTC")
        void testIsoOffsetDateTimeMillisUTC() {
            assertEquals("2025-06-15T10:30:45.000Z", DateFormatUtils.ISO_OFFSET_DATE_TIME_MILLIS.format(ZONED_DATE_TIME_UTC));
        }

        @Test
        @DisplayName("ISO_OFFSET_DATE_TIME")
        void testIsoOffsetDateTime() {
            assertEquals("2025-06-15T10:30:45+08:00", DateFormatUtils.ISO_OFFSET_DATE_TIME.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_OFFSET_DATE_TIME - UTC")
        void testIsoOffsetDateTimeUTC() {
            assertEquals("2025-06-15T10:30:45Z", DateFormatUtils.ISO_OFFSET_DATE_TIME.format(ZONED_DATE_TIME_UTC));
        }

        @Test
        @DisplayName("SLASH_ISO_OFFSET_DATE_TIME_MILLIS")
        void testSlashIsoOffsetDateTimeMillis() {
            assertEquals("2025/06/15T10:30:45.000+08:00", DateFormatUtils.SLASH_ISO_OFFSET_DATE_TIME_MILLIS.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("SLASH_ISO_OFFSET_DATE_TIME")
        void testSlashIsoOffsetDateTime() {
            assertEquals("2025/06/15T10:30:45+08:00", DateFormatUtils.SLASH_ISO_OFFSET_DATE_TIME.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_OFFSET_TIME_MILLIS")
        void testIsoOffsetTimeMillis() {
            assertEquals("10:30:45.000+08:00", DateFormatUtils.ISO_OFFSET_TIME_MILLIS.format(OFFSET_TIME));
        }

        @Test
        @DisplayName("ISO_OFFSET_TIME")
        void testIsoOffsetTime() {
            assertEquals("10:30:45+08:00", DateFormatUtils.ISO_OFFSET_TIME.format(OFFSET_TIME));
        }

        @Test
        @DisplayName("ISO_OFFSET_DATE_TIME 兼容 OffsetDateTime")
        void testIsoOffsetDateTimeWithOffsetDateTime() {
            assertEquals("2025-06-15T10:30:45+08:00", DateFormatUtils.ISO_OFFSET_DATE_TIME.format(OFFSET_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_OFFSET_DATE_TIME 兼容 OffsetDateTime - UTC")
        void testIsoOffsetDateTimeWithOffsetDateTimeUTC() {
            assertEquals("2025-06-15T10:30:45Z", DateFormatUtils.ISO_OFFSET_DATE_TIME.format(OFFSET_DATE_TIME_UTC));
        }
    }

    // ===== ISO UTC Zulu 格式 =====

    @Nested
    @DisplayName("ISO UTC Zulu 格式（ISO_UTC_*）")
    class IsoUtcFormatTest {

        @Test
        @DisplayName("ISO_UTC_DATE_TIME_MILLIS")
        void testIsoUtcDateTimeMillis() {
            assertEquals("2025-06-15T02:30:45.000Z", DateFormatUtils.ISO_UTC_DATE_TIME_MILLIS.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_UTC_DATE_TIME_MILLIS - UTC")
        void testIsoUtcDateTimeMillisUTC() {
            assertEquals("2025-06-15T10:30:45.000Z", DateFormatUtils.ISO_UTC_DATE_TIME_MILLIS.format(ZONED_DATE_TIME_UTC));
        }

        @Test
        @DisplayName("ISO_UTC_DATE_TIME")
        void testIsoUtcDateTime() {
            assertEquals("2025-06-15T02:30:45Z", DateFormatUtils.ISO_UTC_DATE_TIME.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_UTC_DATE_TIME - UTC")
        void testIsoUtcDateTimeUTC() {
            assertEquals("2025-06-15T10:30:45Z", DateFormatUtils.ISO_UTC_DATE_TIME.format(ZONED_DATE_TIME_UTC));
        }

        @Test
        @DisplayName("SLASH_ISO_UTC_DATE_TIME_MILLIS")
        void testSlashIsoUtcDateTimeMillis() {
            assertEquals("2025/06/15T02:30:45.000Z", DateFormatUtils.SLASH_ISO_UTC_DATE_TIME_MILLIS.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("SLASH_ISO_UTC_DATE_TIME")
        void testSlashIsoUtcDateTime() {
            assertEquals("2025/06/15T02:30:45Z", DateFormatUtils.SLASH_ISO_UTC_DATE_TIME.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_UTC_TIME_MILLIS")
        void testIsoUtcTimeMillis() {
            assertEquals("02:30:45.000Z", DateFormatUtils.ISO_UTC_TIME_MILLIS.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_UTC_TIME")
        void testIsoUtcTime() {
            assertEquals("02:30:45Z", DateFormatUtils.ISO_UTC_TIME.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ISO_UTC_DATE_TIME 兼容 OffsetDateTime")
        void testIsoUtcDateTimeWithOffsetDateTime() {
            assertEquals("2025-06-15T02:30:45Z", DateFormatUtils.ISO_UTC_DATE_TIME.format(OFFSET_DATE_TIME));
        }
    }

    // ===== 带时区 ID 格式 =====

    @Nested
    @DisplayName("带时区 ID 格式（ZONED_*）")
    class ZonedFormatTest {

        @Test
        @DisplayName("ZONED_DATE_TIME_MILLIS")
        void testZonedDateTimeMillis() {
            assertEquals("2025-06-15 10:30:45.123 +08:00 [Asia/Shanghai]",
                    DateFormatUtils.ZONED_DATE_TIME_MILLIS.format(ZONED_DATE_TIME_MILLIS));
        }

        @Test
        @DisplayName("ZONED_DATE_TIME")
        void testZonedDateTime() {
            assertEquals("2025-06-15 10:30:45 +08:00 [Asia/Shanghai]",
                    DateFormatUtils.ZONED_DATE_TIME.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("SLASH_ZONED_DATE_TIME_MILLIS")
        void testSlashZonedDateTimeMillis() {
            assertEquals("2025/06/15 10:30:45.123 +08:00 [Asia/Shanghai]",
                    DateFormatUtils.SLASH_ZONED_DATE_TIME_MILLIS.format(ZONED_DATE_TIME_MILLIS));
        }

        @Test
        @DisplayName("SLASH_ZONED_DATE_TIME")
        void testSlashZonedDateTime() {
            assertEquals("2025/06/15 10:30:45 +08:00 [Asia/Shanghai]",
                    DateFormatUtils.SLASH_ZONED_DATE_TIME.format(ZONED_DATE_TIME));
        }

        @Test
        @DisplayName("ZONED_DATE_TIME_MILLIS - UTC")
        void testZonedDateTimeMillisUTC() {
            ZonedDateTime utcZoned = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 123_000_000, ZoneOffset.UTC);
            assertEquals("2025-06-15 10:30:45.123 Z [Z]",
                    DateFormatUtils.ZONED_DATE_TIME_MILLIS.format(utcZoned));
        }

        @Test
        @DisplayName("ZONED_DATE_TIME - UTC")
        void testZonedDateTimeUTC() {
            ZonedDateTime utcZoned = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.UTC);
            assertEquals("2025-06-15 10:30:45 Z [Z]",
                    DateFormatUtils.ZONED_DATE_TIME.format(utcZoned));
        }
    }

    // ===== 快捷方法 =====

    @Nested
    @DisplayName("timestamp / timestampUtc 快捷方法")
    class TimestampMethodTest {

        @Test
        @DisplayName("timestamp(TemporalAccessor, TimestampStyle)")
        void testTimestampWithTemporalAndStyle() {
            assertEquals("20250615103045", DateFormatUtils.timestamp(DATE_TIME_NO_MILLIS, FULL));
            assertEquals("20250615103045123", DateFormatUtils.timestamp(DATE_TIME, MILLIS));
            assertEquals("250615103045", DateFormatUtils.timestamp(DATE_TIME_NO_MILLIS, COMPACT));
            assertEquals("250615103045123", DateFormatUtils.timestamp(DATE_TIME, COMPACT_MILLIS));
        }

        @Test
        @DisplayName("timestamp(TemporalAccessor)")
        void testTimestampWithTemporal() {
            assertEquals("20250615103045", DateFormatUtils.timestamp(DATE_TIME_NO_MILLIS));
        }

        @Test
        @DisplayName("timestamp(TimestampStyle)")
        void testTimestampWithStyle() {
            assertEquals(14, DateFormatUtils.timestamp(FULL).length());
            assertEquals(17, DateFormatUtils.timestamp(MILLIS).length());
            assertEquals(12, DateFormatUtils.timestamp(COMPACT).length());
            assertEquals(15, DateFormatUtils.timestamp(COMPACT_MILLIS).length());
        }

        @Test
        @DisplayName("timestamp()")
        void testTimestampNoArg() {
            String ts = DateFormatUtils.timestamp();
            assertNotNull(ts);
            assertEquals(14, ts.length());
            assertDoesNotThrow(() -> LocalDateTime.parse(ts, FULL.formatter));
        }

        @Test
        @DisplayName("timestampUtc(TimestampStyle)")
        void testTimestampUtcWithStyle() {
            assertEquals(14, DateFormatUtils.timestampUtc(FULL).length());
            assertEquals(17, DateFormatUtils.timestampUtc(MILLIS).length());
            assertEquals(12, DateFormatUtils.timestampUtc(COMPACT).length());
            assertEquals(15, DateFormatUtils.timestampUtc(COMPACT_MILLIS).length());
        }

        @Test
        @DisplayName("timestampUtc(TemporalAccessor)")
        void testTimestampUtcWithTemporal() {
            LocalDateTime utcDateTime = LocalDateTime.of(2025, 6, 15, 2, 30, 45);
            assertEquals("20250615023045", DateFormatUtils.timestampUtc(utcDateTime));
        }

        @Test
        @DisplayName("timestampUtc()")
        void testTimestampUtcNoArg() {
            String ts = DateFormatUtils.timestampUtc();
            assertNotNull(ts);
            assertEquals(14, ts.length());
        }

        @Test
        @DisplayName("timestampUtc() 输出与 UTC 时间一致")
        void testTimestampUtcMatchesUTC() {
            LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
            assertEquals(FULL.formatter.format(utcNow), DateFormatUtils.timestampUtc());
        }
    }

    // ===== Pattern 常量校验 =====

    @Nested
    @DisplayName("Pattern 常量校验")
    class PatternConstantTest {

        @Test
        @DisplayName("所有 _PATTERN 常量与格式器一致")
        void testAllPatternConstants() {
            // 紧凑格式
            assertEquals("yyyyMMddHHmmssSSS", DateFormatUtils.COMPACT_DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyMMddHHmmssSSS", DateFormatUtils.COMPACT_DATE_TIME_MILLIS_SHORT_PATTERN);
            assertEquals("yyyyMMddHHmmss", DateFormatUtils.COMPACT_DATE_TIME_PATTERN);
            assertEquals("yyMMddHHmmss", DateFormatUtils.COMPACT_DATE_TIME_SHORT_PATTERN);

            // 标准格式
            assertEquals("yyyy-MM-dd HH:mm:ss.SSS", DateFormatUtils.DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyyy-MM-dd HH:mm:ss", DateFormatUtils.DATE_TIME_PATTERN);
            assertEquals("yyyy-MM-dd", DateFormatUtils.DATE_PATTERN);

            // 斜杠格式
            assertEquals("yyyy/MM/dd HH:mm:ss.SSS", DateFormatUtils.SLASH_DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyyy/MM/dd HH:mm:ss", DateFormatUtils.SLASH_DATE_TIME_PATTERN);
            assertEquals("yyyy/MM/dd", DateFormatUtils.SLASH_DATE_PATTERN);

            // 时间格式
            assertEquals("HH:mm:ss.SSS", DateFormatUtils.TIME_MILLIS_PATTERN);
            assertEquals("HH:mm:ss", DateFormatUtils.TIME_PATTERN);

            // ISO 偏移格式
            assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", DateFormatUtils.ISO_OFFSET_DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyyy-MM-dd'T'HH:mm:ssXXX", DateFormatUtils.ISO_OFFSET_DATE_TIME_PATTERN);
            assertEquals("yyyy/MM/dd'T'HH:mm:ss.SSSXXX", DateFormatUtils.SLASH_ISO_OFFSET_DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyyy/MM/dd'T'HH:mm:ssXXX", DateFormatUtils.SLASH_ISO_OFFSET_DATE_TIME_PATTERN);
            assertEquals("HH:mm:ss.SSSXXX", DateFormatUtils.ISO_OFFSET_TIME_MILLIS_PATTERN);
            assertEquals("HH:mm:ssXXX", DateFormatUtils.ISO_OFFSET_TIME_PATTERN);

            // ISO UTC Zulu 格式
            assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", DateFormatUtils.ISO_UTC_DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyyy-MM-dd'T'HH:mm:ss'Z'", DateFormatUtils.ISO_UTC_DATE_TIME_PATTERN);
            assertEquals("yyyy/MM/dd'T'HH:mm:ss.SSS'Z'", DateFormatUtils.SLASH_ISO_UTC_DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyyy/MM/dd'T'HH:mm:ss'Z'", DateFormatUtils.SLASH_ISO_UTC_DATE_TIME_PATTERN);
            assertEquals("HH:mm:ss.SSS'Z'", DateFormatUtils.ISO_UTC_TIME_MILLIS_PATTERN);
            assertEquals("HH:mm:ss'Z'", DateFormatUtils.ISO_UTC_TIME_PATTERN);

            // 带时区 ID 格式
            assertEquals("yyyy-MM-dd HH:mm:ss.SSS XXX '['VV']'", DateFormatUtils.ZONED_DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyyy-MM-dd HH:mm:ss XXX '['VV']'", DateFormatUtils.ZONED_DATE_TIME_PATTERN);
            assertEquals("yyyy/MM/dd HH:mm:ss.SSS XXX '['VV']'", DateFormatUtils.SLASH_ZONED_DATE_TIME_MILLIS_PATTERN);
            assertEquals("yyyy/MM/dd HH:mm:ss XXX '['VV']'", DateFormatUtils.SLASH_ZONED_DATE_TIME_PATTERN);

            // TimestampStyle
            assertEquals("yyyyMMddHHmmss", FULL.pattern);
            assertEquals("yyyyMMddHHmmssSSS", MILLIS.pattern);
            assertEquals("yyMMddHHmmss", COMPACT.pattern);
            assertEquals("yyMMddHHmmssSSS", COMPACT_MILLIS.pattern);
        }
    }
}
