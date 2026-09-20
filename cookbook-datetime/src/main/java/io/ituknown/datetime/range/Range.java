package io.ituknown.datetime.range;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 可比较区间的通用值对象
 * <p>
 * 表示一段连续的取值范围，起止边界均包含在内；
 * 任一边界为空表示该侧不限制，两侧均为空表示全量区间。
 * 仅承载边界数据，不做边界顺序校验，调用方自行保证开始不晚于结束，
 * 顺序颠倒的区间包含判断恒为假。
 *
 * @param <T> 边界值类型
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class Range<T extends Comparable<? super T>> {

    private T start;
    private T end;

    public Range(T start, T end) {
        this.start = start;
        this.end = end;
    }

    public static <T extends Comparable<? super T>> Range<T> of(T start, T end) {
        return new Range<>(start, end);
    }

    /**
     * 判断给定值是否落在区间内，起止边界均包含在内
     *
     * @param value 待判断的值，为空时恒为不包含
     */
    public boolean contains(T value) {
        if (value == null) {
            return false;
        }

        boolean afterStart = (start == null) || start.compareTo(value) <= 0;
        boolean beforeEnd = (end == null) || end.compareTo(value) >= 0;
        return afterStart && beforeEnd;
    }

    /**
     * 边界检查
     */
    public boolean checkBoundary() {
        if (start == null || end == null) {
            return true;
        }
        return start.compareTo(end) <= 0;
    }
}
