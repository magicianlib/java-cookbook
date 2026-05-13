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
    public void testTIMESTAMP_Format() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        String result = DateFormatUtils.TIMESTAMP.format(dateTime);
        assertEquals("20250615103045", result);
    }

    @Test
    public void testSIMPLIFY_TIMESTAMP_Format() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        String result = DateFormatUtils.SIMPLIFY_TIMESTAMP.format(dateTime);
        assertEquals("250615103045", result);
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
    public void testTIMESTAMP_MILLIS_Format() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45, 123_000_000);
        String result = DateFormatUtils.TIMESTAMP_MILLIS.format(dateTime);
        assertEquals("20250615103045123", result);
    }

    @Test
    public void testTimestamp_WithTemporalAndStyle() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45, 123_000_000);
        assertEquals("20250615103045", DateFormatUtils.timestamp(dateTime, TimestampStyle.FULL));
        assertEquals("20250615103045123", DateFormatUtils.timestamp(dateTime, TimestampStyle.MILLIS));
        assertEquals("250615103045", DateFormatUtils.timestamp(dateTime, TimestampStyle.COMPACT));
    }

    @Test
    public void testTimestamp_NoArg() {
        String ts = DateFormatUtils.timestamp();
        assertNotNull(ts);
        assertEquals(14, ts.length());
        assertDoesNotThrow(() -> LocalDateTime.parse(ts, DateFormatUtils.TIMESTAMP));
    }

    @Test
    public void testTimestamp_Style() {
        assertEquals(14, DateFormatUtils.timestamp(TimestampStyle.FULL).length());
        assertEquals(17, DateFormatUtils.timestamp(TimestampStyle.MILLIS).length());
        assertEquals(12, DateFormatUtils.timestamp(TimestampStyle.COMPACT).length());
    }

    @Test
    public void testTimestampUtc_NoArg() {
        String ts = DateFormatUtils.timestampUtc();
        assertNotNull(ts);
        assertEquals(14, ts.length());
    }

    @Test
    public void testTimestampUtc_Style() {
        assertEquals(14, DateFormatUtils.timestampUtc(TimestampStyle.FULL).length());
        assertEquals(17, DateFormatUtils.timestampUtc(TimestampStyle.MILLIS).length());
        assertEquals(12, DateFormatUtils.timestampUtc(TimestampStyle.COMPACT).length());
    }

    @Test
    public void testTimestampUtc_MatchesUTC() {
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        assertEquals(DateFormatUtils.TIMESTAMP.format(utcNow), DateFormatUtils.timestampUtc());
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
        assertEquals("yyyyMMddHHmmss", DateFormatUtils.TIMESTAMP_PATTERN);
        assertEquals("yyyyMMddHHmmssSSS", DateFormatUtils.TIMESTAMP_MILLIS_PATTERN);
        assertEquals("yyMMddHHmmss", DateFormatUtils.SIMPLIFY_TIMESTAMP_PATTERN);
        assertEquals("yyMMdd", DateFormatUtils.SIMPLIFY_DATE_PATTERN);
        assertEquals("HH:mm:ss OOOO", DateFormatUtils.TIME_ZONE_PATTERN);
        assertEquals("yyyy-MM-dd HH:mm:ss OOOO", DateFormatUtils.DATE_TIME_ZONE_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", DateFormatUtils.ISO_OFFSET_DATE_TIME_MILLIS_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", DateFormatUtils.ISO_INSTANT_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ssXXX", DateFormatUtils.ISO_OFFSET_DATE_TIME_PATTERN);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss'Z'", DateFormatUtils.ISO_UTC_DATE_TIME_PATTERN);
    }
}
