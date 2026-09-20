package io.ituknown.datetime.range;

import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 遗留日期类型的区间，
 * 边界持有独立副本，外部改动入参或读取到的对象均不影响区间内部状态
 */
@EqualsAndHashCode(callSuper = true)
public class DateRange extends Range<Date> {

    public DateRange() {
    }

    public DateRange(Date start, Date end) {
        super(copy(start), copy(end));
    }

    public static DateRange of(Date start, Date end) {
        return new DateRange(start, end);
    }

    @Override
    public void setStart(Date start) {
        super.setStart(copy(start));
    }

    @Override
    public void setEnd(Date end) {
        super.setEnd(copy(end));
    }

    @Override
    public Date getStart() {
        return copy(super.getStart());
    }

    @Override
    public Date getEnd() {
        return copy(super.getEnd());
    }

    private static Date copy(Date date) {
        return date == null ? null : new Date(date.getTime());
    }
}
