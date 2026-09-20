package io.ituknown.datetime.range;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateRange")
public class DateRangeTest {

    // ===== 防御性拷贝 =====

    @Nested
    @DisplayName("防御性拷贝")
    class DefensiveCopyTest {

        @Test
        @DisplayName("构造后改动入参源对象不影响区间边界")
        void testExternalMutationAfterConstructIgnored() {
            Date start = new Date(1_000);
            Date end = new Date(2_000);
            DateRange range = new DateRange(start, end);

            start.setTime(9_000);
            assertEquals(1_000, range.getStart().getTime());
            assertEquals(2_000, range.getEnd().getTime());
            assertFalse(range.contains(new Date(9_000)));
        }

        @Test
        @DisplayName("改动读取到的边界对象不影响区间内部状态")
        void testGetterReturnsDefensiveCopy() {
            DateRange range = new DateRange(new Date(1_000), new Date(2_000));

            range.getStart().setTime(9_000);
            assertEquals(1_000, range.getStart().getTime());
        }

        @Test
        @DisplayName("单字段更新持有独立副本（无参构造组装场景）")
        void testSetterStoresDefensiveCopy() {
            DateRange range = new DateRange();
            Date start = new Date(1_000);

            range.setStart(start);
            start.setTime(9_000);
            assertEquals(1_000, range.getStart().getTime());
        }

        @Test
        @DisplayName("单字段更新结束同样持有独立副本")
        void testSetEndStoresDefensiveCopy() {
            DateRange range = new DateRange();
            Date end = new Date(2_000);

            range.setEnd(end);
            end.setTime(300);
            assertEquals(2_000, range.getEnd().getTime());
        }
    }

    // ===== 值语义 =====

    @Nested
    @DisplayName("值语义")
    class ValueSemanticsTest {

        @Test
        @DisplayName("时刻相同的两个区间相等")
        void testEqualsByInstant() {
            assertEquals(new DateRange(new Date(1_000), new Date(2_000)),
                    new DateRange(new Date(1_000), new Date(2_000)));
        }
    }
}
