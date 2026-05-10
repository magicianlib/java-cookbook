package io.ituknown.datetime;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

public class DateBoundaryUtilsTest {

    // ===== Year boundaries =====

    @Test
    public void testFirstDayOfYear_LocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.firstDayOfYear(dateTime);
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfNextYear_LocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.firstDayOfNextYear(dateTime);
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0, 0), result);
    }

    @Test
    public void testLastDayOfYear_LocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.lastDayOfYear(dateTime);
        assertEquals(LocalDateTime.of(2025, 12, 31, 23, 59, 59, 999999999), result);
    }

    @Test
    public void testFirstDayOfYear_LocalDate() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        LocalDate result = DateBoundaryUtils.firstDayOfYear(date);
        assertEquals(LocalDate.of(2025, 1, 1), result);
    }

    @Test
    public void testFirstDayOfYear_ZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime result = DateBoundaryUtils.firstDayOfYear(zdt);
        assertEquals(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("Asia/Shanghai")), result);
    }

    // ===== Quarter boundaries =====

    @Test
    public void testFirstDayOfQuarter_Q1() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfQuarter(dateTime);
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfQuarter_Q2() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 5, 15, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfQuarter(dateTime);
        assertEquals(LocalDateTime.of(2025, 4, 1, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfQuarter_Q3() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 8, 15, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfQuarter(dateTime);
        assertEquals(LocalDateTime.of(2025, 7, 1, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfQuarter_Q4() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 11, 15, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfQuarter(dateTime);
        assertEquals(LocalDateTime.of(2025, 10, 1, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfNextQuarter() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 5, 15, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfNextQuarter(dateTime);
        assertEquals(LocalDateTime.of(2025, 7, 1, 0, 0, 0), result);
    }

    @Test
    public void testLastDayOfQuarter_Q1() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 2, 15, 10, 30);
        LocalDateTime result = DateBoundaryUtils.lastDayOfQuarter(dateTime);
        assertEquals(LocalDateTime.of(2025, 3, 31, 23, 59, 59, 999999999), result);
    }

    @Test
    public void testLastDayOfQuarter_Q4() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 12, 15, 10, 30);
        LocalDateTime result = DateBoundaryUtils.lastDayOfQuarter(dateTime);
        assertEquals(LocalDateTime.of(2025, 12, 31, 23, 59, 59, 999999999), result);
    }

    @Test
    public void testFirstDayOfQuarter_ZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 5, 15, 10, 30, 0, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime result = DateBoundaryUtils.firstDayOfQuarter(zdt);
        assertEquals(ZonedDateTime.of(2025, 4, 1, 0, 0, 0, 0, ZoneId.of("Asia/Shanghai")), result);
    }

    @Test
    public void testLastDayOfQuarter_ZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 5, 15, 10, 30, 0, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime result = DateBoundaryUtils.lastDayOfQuarter(zdt);
        assertEquals(ZonedDateTime.of(2025, 6, 30, 23, 59, 59, 999999999, ZoneId.of("Asia/Shanghai")), result);
    }

    // ===== Month boundaries =====

    @Test
    public void testFirstDayOfMonth_LocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.firstDayOfMonth(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 1, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfNextMonth_LocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.firstDayOfNextMonth(dateTime);
        assertEquals(LocalDateTime.of(2025, 7, 1, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfNextMonth_CrossYear() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 12, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.firstDayOfNextMonth(dateTime);
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0, 0), result);
    }

    @Test
    public void testLastDayOfMonth_LocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.lastDayOfMonth(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 30, 23, 59, 59, 999999999), result);
    }

    @Test
    public void testLastDayOfMonth_February_LeapYear() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 2, 10, 10, 30);
        LocalDateTime result = DateBoundaryUtils.lastDayOfMonth(dateTime);
        assertEquals(LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999999999), result);
    }

    @Test
    public void testLastDayOfMonth_February_NonLeapYear() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 2, 10, 10, 30);
        LocalDateTime result = DateBoundaryUtils.lastDayOfMonth(dateTime);
        assertEquals(LocalDateTime.of(2025, 2, 28, 23, 59, 59, 999999999), result);
    }

    // ===== Week boundaries =====

    @Test
    public void testFirstDayOfWeek_Monday() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfWeek(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 9, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfWeek_Wednesday() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 11, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfWeek(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 9, 0, 0, 0), result);
    }

    @Test
    public void testFirstDayOfWeek_OnMonday() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 9, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfWeek(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 9, 0, 0, 0), result);
    }

    @Test
    public void testLastDayOfWeek() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 11, 10, 30);
        LocalDateTime result = DateBoundaryUtils.lastDayOfWeek(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 15, 23, 59, 59, 999999999), result);
    }

    @Test
    public void testFirstDayOfNextWeek() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 11, 10, 30);
        LocalDateTime result = DateBoundaryUtils.firstDayOfNextWeek(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 16, 0, 0, 0), result);
    }

    @Test
    public void testDayInCurrentWeek() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 11, 10, 30); // 周三
        LocalDateTime result = DateBoundaryUtils.dayInCurrentWeek(dateTime, DayOfWeek.FRIDAY);
        // previousOrSame(FRIDAY) → 上一周五 = 2025-06-06
        assertEquals(LocalDateTime.of(2025, 6, 6, 10, 30), result);
    }

    @Test
    public void testPreviousDayOfWeek() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 11, 10, 30); // 周三
        LocalDateTime result = DateBoundaryUtils.previousDayOfWeek(dateTime, DayOfWeek.MONDAY);
        // previous(MONDAY) → 前一个周一 = 2025-06-09
        assertEquals(LocalDateTime.of(2025, 6, 9, 10, 30), result);
    }

    @Test
    public void testNextDayOfWeek() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 11, 10, 30); // 周三
        LocalDateTime result = DateBoundaryUtils.nextDayOfWeek(dateTime, DayOfWeek.MONDAY);
        // next(MONDAY) → 下周一 = 2025-06-16
        assertEquals(LocalDateTime.of(2025, 6, 16, 10, 30), result);
    }

    // ===== startOfDay / endOfDay =====

    @Test
    public void testStartOfDay_LocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.startOfDay(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 15, 0, 0, 0), result);
    }

    @Test
    public void testEndOfDay_LocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        LocalDateTime result = DateBoundaryUtils.endOfDay(dateTime);
        assertEquals(LocalDateTime.of(2025, 6, 15, 23, 59, 59, 999999999), result);
    }

    @Test
    public void testStartOfDay_OffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.ofHours(8));
        OffsetDateTime result = DateBoundaryUtils.startOfDay(odt);
        assertEquals(OffsetDateTime.of(2025, 6, 15, 0, 0, 0, 0, ZoneOffset.ofHours(8)), result);
    }

    @Test
    public void testEndOfDay_OffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneOffset.ofHours(8));
        OffsetDateTime result = DateBoundaryUtils.endOfDay(odt);
        assertEquals(OffsetDateTime.of(2025, 6, 15, 23, 59, 59, 999999999, ZoneOffset.ofHours(8)), result);
    }

    @Test
    public void testStartOfDay_ZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime result = DateBoundaryUtils.startOfDay(zdt);
        assertEquals(ZonedDateTime.of(2025, 6, 15, 0, 0, 0, 0, ZoneId.of("Asia/Shanghai")), result);
    }

    @Test
    public void testEndOfDay_ZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime result = DateBoundaryUtils.endOfDay(zdt);
        assertEquals(ZonedDateTime.of(2025, 6, 15, 23, 59, 59, 999999999, ZoneId.of("Asia/Shanghai")), result);
    }

    @Test
    public void testStartOfDay_LocalDate() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        LocalDate result = DateBoundaryUtils.startOfDay(date);
        assertEquals(date, result);
    }

    @Test
    public void testEndOfDay_LocalDate() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        LocalDate result = DateBoundaryUtils.endOfDay(date);
        assertEquals(date, result);
    }

    // ===== ZonedDateTime DST-safe behavior =====

    @Test
    public void testEndOfDay_ZonedDateTime_DST() {
        ZoneId zone = ZoneId.of("America/New_York");
        ZonedDateTime zdt = ZonedDateTime.of(2025, 3, 9, 10, 0, 0, 0, zone);
        ZonedDateTime result = DateBoundaryUtils.endOfDay(zdt);
        assertEquals(zdt.toLocalDate(), result.toLocalDate());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
        assertEquals(999999999, result.getNano());
    }

    @Test
    public void testFirstDayOfMonth_ZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime result = DateBoundaryUtils.firstDayOfMonth(zdt);
        assertEquals(ZonedDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneId.of("Asia/Shanghai")), result);
    }

    @Test
    public void testLastDayOfMonth_ZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime result = DateBoundaryUtils.lastDayOfMonth(zdt);
        assertEquals(ZonedDateTime.of(2025, 6, 30, 23, 59, 59, 999999999, ZoneId.of("Asia/Shanghai")), result);
    }

    @Test
    public void testLastDayOfYear_ZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 6, 15, 10, 30, 45, 0, ZoneId.of("Asia/Shanghai"));
        ZonedDateTime result = DateBoundaryUtils.lastDayOfYear(zdt);
        assertEquals(ZonedDateTime.of(2025, 12, 31, 23, 59, 59, 999999999, ZoneId.of("Asia/Shanghai")), result);
    }
}
