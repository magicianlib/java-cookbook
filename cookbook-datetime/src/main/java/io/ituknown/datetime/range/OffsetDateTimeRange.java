package io.ituknown.datetime.range;

import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 带时区偏移的日期时间区间
 */
@EqualsAndHashCode(callSuper = true)
public class OffsetDateTimeRange extends Range<OffsetDateTime> {

    public OffsetDateTimeRange() {
    }

    public OffsetDateTimeRange(OffsetDateTime start, OffsetDateTime end) {
        super(start, end);
    }

    public static OffsetDateTimeRange of(OffsetDateTime start, OffsetDateTime end) {
        return new OffsetDateTimeRange(start, end);
    }
}
