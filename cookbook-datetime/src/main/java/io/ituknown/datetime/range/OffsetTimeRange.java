package io.ituknown.datetime.range;

import lombok.EqualsAndHashCode;

import java.time.OffsetTime;

/**
 * 带时区偏移的时刻区间，时刻先后按换算到统一基准的绝对时间判断
 */
@EqualsAndHashCode(callSuper = true)
public class OffsetTimeRange extends Range<OffsetTime> {

    public OffsetTimeRange() {
    }

    public OffsetTimeRange(OffsetTime start, OffsetTime end) {
        super(start, end);
    }

    public static OffsetTimeRange of(OffsetTime start, OffsetTime end) {
        return new OffsetTimeRange(start, end);
    }
}
