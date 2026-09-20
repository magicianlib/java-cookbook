package io.ituknown.datetime.range;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Range")
public class RangeTest {

    private static final LocalDate START = LocalDate.of(2025, 1, 1);
    private static final LocalDate END = LocalDate.of(2025, 12, 31);

    // ===== 构造 =====

    @Nested
    @DisplayName("构造")
    class ConstructTest {

        @Test
        @DisplayName("开始与结束相等时为单点闭区间")
        void testEqualBoundariesAllowed() {
            Range<LocalDate> range = new Range<>(START, START);
            assertEquals(START, range.getStart());
            assertEquals(START, range.getEnd());
        }

        @Test
        @DisplayName("任一边界为空表示该侧不限制")
        void testNullBoundaryAllowed() {
            assertDoesNotThrow(() -> new Range<>(null, END));
            assertDoesNotThrow(() -> new Range<>(START, null));
        }

        @Test
        @DisplayName("两侧均为空表示全量区间")
        void testBothNullAllowed() {
            assertDoesNotThrow(() -> new Range<>(null, null));
        }

        @Test
        @DisplayName("无参构造创建全量区间（供反序列化框架使用）")
        void testNoArgsConstructor() {
            Range<LocalDate> range = new Range<>();
            assertNull(range.getStart());
            assertNull(range.getEnd());
            assertTrue(range.contains(LocalDate.of(2999, 1, 1)));
        }
    }

    // ===== 边界更新 =====

    @Nested
    @DisplayName("边界更新")
    class UpdateTest {

        @Test
        @DisplayName("更新开始成功后立即生效")
        void testSetStartTakesEffect() {
            Range<LocalDate> range = new Range<>(START, END);
            range.setStart(LocalDate.of(2025, 6, 1));
            assertEquals(LocalDate.of(2025, 6, 1), range.getStart());
            assertEquals(END, range.getEnd());
        }
    }

    // ===== 包含判断 =====

    @Nested
    @DisplayName("包含判断")
    class ContainsTest {

        @Test
        @DisplayName("闭区间：边界值本身包含在内")
        void testBoundariesIncluded() {
            Range<LocalDate> range = new Range<>(START, END);
            assertTrue(range.contains(START));
            assertTrue(range.contains(END));
        }

        @Test
        @DisplayName("区间内与区间外的值")
        void testInsideAndOutside() {
            Range<LocalDate> range = new Range<>(START, END);
            assertTrue(range.contains(LocalDate.of(2025, 6, 15)));
            assertFalse(range.contains(LocalDate.of(2024, 12, 31)));
            assertFalse(range.contains(LocalDate.of(2026, 1, 1)));
        }

        @Test
        @DisplayName("待判断的值为空时恒为不包含")
        void testNullValueNotContained() {
            Range<LocalDate> range = new Range<>(START, END);
            assertFalse(range.contains(null));
        }

        @Test
        @DisplayName("开始为空时只受结束约束")
        void testUnboundedStart() {
            Range<LocalDate> range = new Range<>(null, END);
            assertTrue(range.contains(LocalDate.of(1900, 1, 1)));
            assertFalse(range.contains(LocalDate.of(2026, 1, 1)));
        }

        @Test
        @DisplayName("结束为空时只受开始约束")
        void testUnboundedEnd() {
            Range<LocalDate> range = new Range<>(START, null);
            assertFalse(range.contains(LocalDate.of(2024, 12, 31)));
            assertTrue(range.contains(LocalDate.of(2999, 1, 1)));
        }
    }

    // ===== 值语义 =====

    @Nested
    @DisplayName("值语义")
    class ValueSemanticsTest {

        @Test
        @DisplayName("边界相同的两个区间相等")
        void testEqualsAndHashCode() {
            Range<LocalDate> a = new Range<>(START, END);
            Range<LocalDate> b = Range.of(START, END);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("边界不同的区间不相等")
        void testNotEqualsOnDifferentBoundaries() {
            assertNotEquals(new Range<>(START, END), new Range<>(START, START));
            assertNotEquals(new Range<>(START, END), new Range<>(null, END));
        }

        @Test
        @DisplayName("不同边界类型的区间互不相等")
        void testNotEqualsAcrossTypes() {
            assertNotEquals(new LocalDateRange(), new LocalTimeRange());
            assertNotEquals(new DateRange(), new LocalTimeRange());
            assertNotEquals(new DateRange(new Date(1_000), new Date(2_000)),
                    new LocalDateRange());
        }

        @Test
        @DisplayName("可读的字符串形式")
        void testToString() {
            String s = new Range<>(START, END).toString();
            assertTrue(s.contains("2025-01-01"));
            assertTrue(s.contains("2025-12-31"));
        }
    }
}
