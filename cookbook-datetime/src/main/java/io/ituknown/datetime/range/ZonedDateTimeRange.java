package io.ituknown.datetime.range;

import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;

/**
 * 带时区标识的日期时间区间
 */
@EqualsAndHashCode(callSuper = true)
public class ZonedDateTimeRange extends Range<ZonedDateTime> {

    public ZonedDateTimeRange() {
    }

    public ZonedDateTimeRange(ZonedDateTime start, ZonedDateTime end) {
        super(start, end);
    }

    public static ZonedDateTimeRange of(ZonedDateTime start, ZonedDateTime end) {
        return new ZonedDateTimeRange(start, end);
    }
}
