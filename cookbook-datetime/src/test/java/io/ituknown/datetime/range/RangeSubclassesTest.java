package io.ituknown.datetime.range;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("命名区间工厂")
public class RangeSubclassesTest {

    @Test
    @DisplayName("本地日期区间工厂")
    void testLocalDateRangeOf() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);
        LocalDateRange range = LocalDateRange.of(start, end);
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());
    }

    @Test
    @DisplayName("本地时间区间工厂")
    void testLocalTimeRangeOf() {
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(18, 0);
        LocalTimeRange range = LocalTimeRange.of(start, end);
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());
    }

    @Test
    @DisplayName("本地日期时间区间工厂")
    void testLocalDateTimeRangeOf() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        LocalDateTimeRange range = LocalDateTimeRange.of(start, end);
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());
    }

    @Test
    @DisplayName("带偏移的日期时间区间工厂")
    void testOffsetDateTimeRangeOf() {
        OffsetDateTime start = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(8));
        OffsetDateTime end = OffsetDateTime.of(2025, 12, 31, 23, 59, 0, 0, ZoneOffset.ofHours(8));
        OffsetDateTimeRange range = OffsetDateTimeRange.of(start, end);
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());
    }

    @Test
    @DisplayName("带偏移的时刻区间工厂")
    void testOffsetTimeRangeOf() {
        OffsetTime start = OffsetTime.of(8, 0, 0, 0, ZoneOffset.ofHours(8));
        OffsetTime end = OffsetTime.of(18, 0, 0, 0, ZoneOffset.ofHours(8));
        OffsetTimeRange range = OffsetTimeRange.of(start, end);
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());
    }

    @Test
    @DisplayName("带时区标识的日期时间区间工厂")
    void testZonedDateTimeRangeOf() {
        ZonedDateTime start = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime end = ZonedDateTime.of(2025, 12, 31, 23, 59, 0, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTimeRange range = ZonedDateTimeRange.of(start, end);
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());
    }

    @Test
    @DisplayName("遗留日期区间工厂")
    void testDateRangeOf() {
        Date start = new Date(1_000);
        Date end = new Date(2_000);
        DateRange range = DateRange.of(start, end);
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());
    }
}
