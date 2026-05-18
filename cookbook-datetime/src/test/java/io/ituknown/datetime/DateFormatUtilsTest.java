package io.ituknown.datetime;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static io.ituknown.datetime.TimestampStyle.*;
import static org.junit.jupiter.api.Assertions.*;

public class DateFormatUtilsTest {

    @Test
    public void testBASIC_DATE_Format() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        String result = DateFormatUtils.BASIC_DATE.format(dateTime);
        assertEquals("20250615", result);
    }

    @Test
    public void testISO_LOCAL_DATE_Format() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        String result = DateFormatUtils.ISO_LOCAL_DATE.format(dateTime);
        assertEquals("2025-06-15", result);
    }

    @Test
    public void testISO_LOCAL_TIME_Format() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        String result = DateFormatUtils.ISO_LOCAL_TIME.format(dateTime);
        assertEquals("10:30:45", result);
    }

    @Test
    public void testISO_LOCAL_DATE_TIME_Format() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        String result = DateFormatUtils.ISO_LOCAL_DATE_TIME.format(dateTime);
        assertEquals("2025-06-15T10:30:45", result);
    }

    @Test
    public void testDATE_TIME_Format() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        String result = DateFormatUtils.DATE_TIME.format(dateTime);
        assertEquals("2025-06-15 10:30:45", result);
    }

    @Test
    public void testTIMESTAMP_Full() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        assertEquals("20250615103045", FULL.formatter.format(dateTime));
    }

    @Test
    public void testTIMESTAMP_Millis() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45, 123_000_000);
        assertEquals("20250615103045123", MILLIS.formatter.format(dateTime));
    }

    @Test
    public void testTIMESTAMP_Compact() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        assertEquals("250615103045", COMPACT.formatter.format(dateTime));
    }

    @Test
    public void testTIMESTAMP_CompactMillis() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45, 123_000_000);
        assertEquals("250615103045123", COMPACT_MILLIS.formatter.format(dateTime));
    }

    @Test
    public void testSIMPLIFY_DATE_Format() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        String result = DateFormatUtils.SIMPLIFY_DATE.format(date);
        assertEquals("250615", result);
    }

    @Test
    public void testTIME_ZONE_Format() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        String result = DateFormatUtils.TIME_ZONE.format(zonedDateTime);
        assertEquals("10:30:45 GMT+08:00", result);
    }

    @Test
    public void testDATE_TIME_ZONE_Format() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        String result = DateFormatUtils.DATE_TIME_ZONE.format(zonedDateTime);
        assertEquals("2025-06-15 10:30:45 GMT+08:00", result);
    }

    @Test
    public void testISO_OFFSET_DATE_TIME_MILLIS_Format() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        String result = DateFormatUtils.ISO_OFFSET_DATE_TIME_MILLIS.format(zonedDateTime);
        assertEquals("2025-06-15T10:30:45.000+08:00", result);
    }

    @Test
    public void testISO_OFFSET_DATE_TIME_MILLIS_FormatUTC() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        String result = DateFormatUtils.ISO_OFFSET_DATE_TIME_MILLIS.format(zonedDateTime);
        assertEquals("2025-06-15T10:30:45.000Z", result);
    }

    @Test
    public void testISO_INSTANT_Format() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        String result = DateFormatUtils.ISO_INSTANT.format(zonedDateTime);
        assertEquals("2025-06-15T02:30:45.000Z", result);
    }

    @Test
    public void testISO_INSTANT_FormatUTC() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        String result = DateFormatUtils.ISO_INSTANT.format(zonedDateTime);
        assertEquals("2025-06-15T10:30:45.000Z", result);
    }

    @Test
    public void testISO_OFFSET_DATE_TIME_Format() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        String result = DateFormatUtils.ISO_OFFSET_DATE_TIME.format(zonedDateTime);
        assertEquals("2025-06-15T10:30:45+08:00", result);
    }

    @Test
    public void testISO_OFFSET_DATE_TIME_FormatUTC() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        String result = DateFormatUtils.ISO_OFFSET_DATE_TIME.format(zonedDateTime);
        assertEquals("2025-06-15T10:30:45Z", result);
    }

    @Test
    public void testISO_UTC_DATE_TIME_Format() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        String result = DateFormatUtils.ISO_UTC_DATE_TIME.format(zonedDateTime);
        assertEquals("2025-06-15T02:30:45Z", result);
    }

    @Test
    public void testISO_UTC_DATE_TIME_FormatUTC() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        String result = DateFormatUtils.ISO_UTC_DATE_TIME.format(zonedDateTime);
        assertEquals("2025-06-15T10:30:45Z", result);
    }

    @Test
    public void testTimestamp_WithTemporalAndStyle() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45, 123_000_000);
        assertEquals("20250615103045", DateFormatUtils.timestamp(dateTime, FULL));
        assertEquals("20250615103045123", DateFormatUtils.timestamp(dateTime, MILLIS));
        assertEquals("250615103045", DateFormatUtils.timestamp(dateTime, COMPACT));
        assertEquals("250615103045123", DateFormatUtils.timestamp(dateTime, COMPACT_MILLIS));
    }

    @Test
    public void testTimestamp_NoArg() {
        String ts = DateFormatUtils.timestamp();
        assertNotNull(ts);
        assertEquals(14, ts.length());
        assertDoesNotThrow(() -> LocalDateTime.parse(ts, FULL.formatter));
    }

    @Test
    public void testTimestamp_Style() {
        assertEquals(14, DateFormatUtils.timestamp(FULL).length());
        assertEquals(17, DateFormatUtils.timestamp(MILLIS).length());
        assertEquals(12, DateFormatUtils.timestamp(COMPACT).length());
        assertEquals(15, DateFormatUtils.timestamp(COMPACT_MILLIS).length());
    }

    @Test
    public void testTimestamp_Temporal() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        assertEquals("20250615103045", DateFormatUtils.timestamp(dateTime));
    }

    @Test
    public void testTimestampUtc_NoArg() {
        String ts = DateFormatUtils.timestampUtc();
        assertNotNull(ts);
        assertEquals(14, ts.length());
    }

    @Test
    public void testTimestampUtc_Style() {
        assertEquals(14, DateFormatUtils.timestampUtc(FULL).length());
        assertEquals(17, DateFormatUtils.timestampUtc(MILLIS).length());
        assertEquals(12, DateFormatUtils.timestampUtc(COMPACT).length());
        assertEquals(15, DateFormatUtils.timestampUtc(COMPACT_MILLIS).length());
    }

    @Test
    public void testTimestampUtc_Temporal() {
        LocalDateTime utcDateTime = LocalDateTime.of(2025, 6, 15, 2, 30, 45);
        assertEquals("20250615023045", DateFormatUtils.timestampUtc(utcDateTime));
    }

    @Test
    public void testTimestampUtc_MatchesUTC() {
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        assertEquals(FULL.formatter.format(utcNow), DateFormatUtils.timestampUtc());
    }

    @Test
    public void testDate_WithLocalDate() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        assertEquals("20250615", DateFormatUtils.date(date, false));
        assertEquals("250615", DateFormatUtils.date(date, true));
    }

    @Test
    public void testDate_NoArg() {
        String date = DateFormatUtils.date();
        assertNotNull(date);
        assertEquals(8, date.length());
        assertDoesNotThrow(() -> LocalDate.parse(date, DateFormatUtils.BASIC_DATE));
    }

    @Test
    public void testDate_Simplify() {
        String date = DateFormatUtils.date(true);
        assertNotNull(date);
        assertEquals(6, date.length());
    }

    @Test
    public void testPatternConstants() {
        assertEquals("yyyyMMdd", DateFormatUtils.BASIC_DATE_PATTERN);
        assertEquals("yyyy-MM-dd", DateFormatUtils.ISO_LOCAL_DATE_PATTERN);
        assertEquals("HH:mm:ss", DateFormatUtils.ISO_LOCAL_TIME_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss", DateFormatUtils.ISO_LOCAL_DATE_TIME_PATTERN);
        assertEquals("yyyy-MM-dd HH:mm:ss", DateFormatUtils.DATE_TIME_PATTERN);
        assertEquals("yyyyMMddHHmmss", FULL.pattern);
        assertEquals("yyyyMMddHHmmssSSS", MILLIS.pattern);
        assertEquals("yyMMddHHmmss", COMPACT.pattern);
        assertEquals("yyMMddHHmmssSSS", COMPACT_MILLIS.pattern);
        assertEquals("yyMMdd", DateFormatUtils.SIMPLIFY_DATE_PATTERN);
        assertEquals("HH:mm:ss OOOO", DateFormatUtils.TIME_ZONE_PATTERN);
        assertEquals("yyyy-MM-dd HH:mm:ss OOOO", DateFormatUtils.DATE_TIME_ZONE_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", DateFormatUtils.ISO_OFFSET_DATE_TIME_MILLIS_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", DateFormatUtils.ISO_INSTANT_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ssXXX", DateFormatUtils.ISO_OFFSET_DATE_TIME_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss'Z'", DateFormatUtils.ISO_UTC_DATE_TIME_PATTERN);
    }
}
