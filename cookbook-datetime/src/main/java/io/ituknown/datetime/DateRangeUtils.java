package io.ituknown.datetime;


import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;

/**
 * 日期范围处理工具
 *
 * @author magicianlib@gmail.com
 */
public final class DateRangeUtils {
    /**
     * 今年开始时间
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfYear(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.firstDayOfYear());
        return startOfDay(t);
    }

    /**
     * 明年开始时间
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfNextYear(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.firstDayOfNextYear());
        return startOfDay(t);
    }

    /**
     * 今年结束时间
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T lastDayOfYear(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.lastDayOfYear());
        return endOfDay(t);
    }

    /**
     * 本季度开始时间
     */
    public static LocalDateTime firstDayOfQuarter(LocalDateTime dateTime) {
        LocalDateTime t = LocalDateTime.now();
        // 当前月份减去 (月份-1)%3 得到季度首月
        t = t.withMonth(dateTime.getMonthValue() - (dateTime.getMonthValue() - 1) % 3);
        // 当月第一天
        t = t.with(TemporalAdjusters.firstDayOfMonth());
        // 清除时分秒
        return startOfDay(t);
    }

    /**
     * 下季度开始时间
     */
    public static LocalDateTime firstDayOfLastQuarter(LocalDateTime dateTime) {
        // 本季度开始时间加 3 个月
        LocalDateTime with = firstDayOfQuarter(dateTime).plusMonths(3);
        return startOfDay(with); // 清除时分秒
    }

    /**
     * 本季度结束时间
     */
    public static LocalDateTime lastDayOfQuarter(LocalDateTime dateTime) {
        // 季度开始时间加 2 个月，然后取该月结束时间
        LocalDateTime t = firstDayOfQuarter(dateTime).plusMonths(2);
        // 当月最后一天
        t = t.with(TemporalAdjusters.lastDayOfMonth());
        // 调整时分秒
        return endOfDay(t);
    }

    /**
     * 本月开始时间
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfMonth(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.firstDayOfMonth());
        return startOfDay(t);
    }

    /**
     * 下月开始时间
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T firstDayOfNextMonth(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.firstDayOfNextMonth());
        return startOfDay(t);
    }

    /**
     * 本月结束时间
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T lastDayOfMonth(T temporal) {
        T t = (T) temporal.with(TemporalAdjusters.lastDayOfMonth());
        return endOfDay(t);
    }

    /**
     * 本周开始时间
     */
    public static <T extends Temporal> T firstDayOfWeek(T temporal) {
        T t = week(temporal, DayOfWeek.MONDAY);
        return startOfDay(t);
    }

    /**
     * 下周开始时间
     */
    public static <T extends Temporal> T firstDayOfNextWeek(T temporal) {
        T t = lastWeek(temporal, DayOfWeek.MONDAY);
        return startOfDay(t);
    }

    /**
     * 本周结束时间
     */
    public static <T extends Temporal> T lastDayOfWeek(T temporal) {
        T t = week(temporal, DayOfWeek.SUNDAY);
        return endOfDay(t);
    }

    /**
     * 获取本周指定日期
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T week(T temporal, DayOfWeek week) {
        return (T) temporal.with(TemporalAdjusters.previousOrSame(week));
    }

    /**
     * 获取上周指定日期
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T lastWeek(T temporal, DayOfWeek week) {
        return (T) temporal.with(TemporalAdjusters.previous(week));
    }

    /**
     * 获取下周指定日期
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T nextWeek(T temporal, DayOfWeek week) {
        return (T) temporal.with(TemporalAdjusters.next(week));
    }

    /**
     * 指定日期开始时间
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T startOfDay(T temporal) {
        // OffsetTime.MIN 会使用 +18:00 时区, 得到的结果是 2026-01-01 00:00:00 GMT+18:00
        if (temporal instanceof LocalDateTime || temporal instanceof OffsetDateTime) {
            return (T) temporal.with(LocalTime.MIN);
        }

        if (temporal instanceof ZonedDateTime) {
            return (T) ((ZonedDateTime) temporal).truncatedTo(ChronoUnit.DAYS);
        }
        return temporal;
    }

    /**
     * 指定日期结束时间
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal> T endOfDay(T temporal) {
        // OffsetTime.MAX 会使用 -18:00 时区, 得到的结果是 2026-12-31 23:59:59 GMT-18:00
        if (temporal instanceof LocalDateTime || temporal instanceof OffsetDateTime) {
            return (T) temporal.with(LocalTime.MAX);
        }

        if (temporal instanceof ZonedDateTime) {
            // ZonedDateTime 包含 夏令时（DST）逻辑。
            // 在某些地区, 由于夏令时切换一天的结束时刻(23:59:59)可能在当地时间中是不存在的, 或者偏移量发生了变化.
            // 最稳妥的方法是: 先获取下一天的开始, 然后减去 1 纳秒, 这样可以自动规避所有时区跳变问题.
            ZonedDateTime t = (ZonedDateTime) temporal;
            return (T) t.plusDays(1).toLocalDate().atStartOfDay(t.getZone()).minusNanos(1);

        }
        return temporal;
    }
}