package io.ituknown.datetime.range;

import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 本地日期区间
 */
@EqualsAndHashCode(callSuper = true)
public class LocalDateRange extends Range<LocalDate> {

    public LocalDateRange() {
    }

    public LocalDateRange(LocalDate start, LocalDate end) {
        super(start, end);
    }

    public static LocalDateRange of(LocalDate start, LocalDate end) {
        return new LocalDateRange(start, end);
    }
}
