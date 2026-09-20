package io.ituknown.datetime.range;

import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 本地时间区间
 */
@EqualsAndHashCode(callSuper = true)
public class LocalTimeRange extends Range<LocalTime> {

    public LocalTimeRange() {
    }

    public LocalTimeRange(LocalTime start, LocalTime end) {
        super(start, end);
    }

    public static LocalTimeRange of(LocalTime start, LocalTime end) {
        return new LocalTimeRange(start, end);
    }
}
