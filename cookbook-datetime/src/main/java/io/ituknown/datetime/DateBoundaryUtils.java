package io.ituknown.datetime;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;

/**
 * 日期边界工具类
 * <p>
 * 提供按年、季度、月、周、日等时间周期计算起止边界的能力，
 * 支持 {@link LocalDateTime}、{@link LocalDate}、{@link ZonedDateTime}、{@link OffsetDateTime} 等时间类型。
 *
 * @author magicianlib@gmail.com
 */
public final class DateBoundaryUtils {

    private DateBoundaryUtils() {
    }

    // ===== 年边界 =====

    /**
     * 获取给定时间所在年的第一天开始时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfYear(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.firstDayOfYear());
        return startOfDay(t);
    }

    /**
     * 获取给定时间下一年的第一天开始时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfNextYear(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.firstDayOfNextYear());
        return startOfDay(t);
    }

    /**
     * 获取给定时间所在年的最后一天结束时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T lastDayOfYear(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.lastDayOfYear());
        return endOfDay(t);
    }

    // ===== 季度边界 =====

    /**
     * 获取给定时间所在季度的第一天开始时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfQuarter(T temporal) {
        int month = temporal.get(ChronoField.MONTH_OF_YEAR);
        T t = (T) temporal.with(ChronoField.MONTH_OF_YEAR, month - (month - 1) % 3);
        t = (T) t.with(TemporalAdjusters.firstDayOfMonth());
        return startOfDay(t);
    }

    /**
     * 获取给定时间下一季度的第一天开始时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfNextQuarter(T temporal) {
        return (T) firstDayOfQuarter(temporal).plus(3, ChronoUnit.MONTHS);
    }

    /**
     * 获取给定时间所在季度的最后一天结束时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T lastDayOfQuarter(T temporal) {
        T t = (T) firstDayOfQuarter(temporal).plus(2, ChronoUnit.MONTHS);
        t = (T) t.with(TemporalAdjusters.lastDayOfMonth());
        return endOfDay(t);
    }

    // ===== 月边界 =====

    /**
     * 获取给定时间所在月的第一天开始时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfMonth(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.firstDayOfMonth());
        return startOfDay(t);
    }

    /**
     * 获取给定时间下一月的第一天开始时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfNextMonth(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.firstDayOfNextMonth());
        return startOfDay(t);
    }

    /**
     * 获取给定时间所在月的最后一天结束时刻
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T lastDayOfMonth(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.lastDayOfMonth());
        return endOfDay(t);
    }

    // ===== 周边界 =====

    /**
     * 获取给定时间所在周（周一为起始）的第一天开始时刻
     */
    public static <T extends Temporal> T firstDayOfWeek(T temporal) {
        T t = dayInCurrentWeek(temporal, DayOfWeek.MONDAY);
        return startOfDay(t);
    }

    /**
     * 获取给定时间下一周（周一为起始）的第一天开始时刻
     */
    public static <T extends Temporal> T firstDayOfNextWeek(T temporal) {
        T t = nextDayOfWeek(temporal, DayOfWeek.MONDAY);
        return startOfDay(t);
    }

    /**
     * 获取给定时间所在周（周一至周日）的最后一天结束时刻
     */
    public static <T extends Temporal> T lastDayOfWeek(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return endOfDay(t);
    }

    /**
     * 获取本周（或之前最近）指定星期几的日期
     * <p>
     * 等价于 {@link TemporalAdjusters#previousOrSame(DayOfWeek)}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T dayInCurrentWeek(T temporal, DayOfWeek dayOfWeek) {
        return (T) temporal.with(TemporalAdjusters.previousOrSame(dayOfWeek));
    }

    /**
     * 获取上一个指定星期几的日期（不包含当天）
     * <p>
     * 等价于 {@link TemporalAdjusters#previous(DayOfWeek)}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T previousDayOfWeek(T temporal, DayOfWeek dayOfWeek) {
        return (T) temporal.with(TemporalAdjusters.previous(dayOfWeek));
    }

    /**
     * 获取下一个指定星期几的日期（不包含当天）
     * <p>
     * 等价于 {@link TemporalAdjusters#next(DayOfWeek)}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T nextDayOfWeek(T temporal, DayOfWeek dayOfWeek) {
        return (T) temporal.with(TemporalAdjusters.next(dayOfWeek));
    }

    // ===== 日边界 =====

    /**
     * 获取给定时间当天的开始时刻（00:00:00）
     * <p>
     * 对 {@link LocalDateTime} 和 {@link OffsetDateTime} 设置时间为 {@link LocalTime#MIN}；
     * 对 {@link ZonedDateTime} 使用 {@link ChronoUnit#DAYS} 截断以保留时区信息；
     * 其他类型原样返回。
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T startOfDay(T temporal) {
        if (temporal instanceof LocalDateTime || temporal instanceof OffsetDateTime) {
            return (T) temporal.with(LocalTime.MIN);
        }
        if (temporal instanceof ZonedDateTime) {
            return (T) ((ZonedDateTime) temporal).truncatedTo(ChronoUnit.DAYS);
        }
        return temporal;
    }

    /**
     * 获取给定时间当天的结束时刻（23:59:59.999999999）
     * <p>
     * 对 {@link LocalDateTime} 和 {@link OffsetDateTime} 设置时间为 {@link LocalTime#MAX}；
     * 对 {@link ZonedDateTime} 使用「下一天开始减一纳秒」策略，以安全处理夏令时（DST）跳变；
     * 其他类型原样返回。
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T endOfDay(T temporal) {
        if (temporal instanceof LocalDateTime || temporal instanceof OffsetDateTime) {
            return (T) temporal.with(LocalTime.MAX);
        }
        if (temporal instanceof ZonedDateTime) {
            ZonedDateTime t = (ZonedDateTime) temporal;
            return (T) t.plusDays(1).toLocalDate().atStartOfDay(t.getZone()).minusNanos(1);
        }
        return temporal;
    }
}
