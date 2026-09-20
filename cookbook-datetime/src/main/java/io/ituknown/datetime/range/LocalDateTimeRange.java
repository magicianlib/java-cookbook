package io.ituknown.datetime.range;

import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本地日期时间区间
 */
@EqualsAndHashCode(callSuper = true)
public class LocalDateTimeRange extends Range<LocalDateTime> {

    public LocalDateTimeRange() {
    }

    public LocalDateTimeRange(LocalDateTime start, LocalDateTime end) {
        super(start, end);
    }

    public static LocalDateTimeRange of(LocalDateTime start, LocalDateTime end) {
        return new LocalDateTimeRange(start, end);
    }
}
