package io.ituknown.datetime.range;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OffsetTimeRange")
public class OffsetTimeRangeTest {

    // 10:00+02:00 的绝对时刻为 08:00Z
    private static final OffsetTime TEN_AT_PLUS2 = OffsetTime.of(10, 0, 0, 0, ZoneOffset.ofHours(2));
    private static final OffsetTime NINE_THIRTY_UTC = OffsetTime.of(9, 30, 0, 0, ZoneOffset.UTC);
    private static final OffsetTime NOON_UTC = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetTime EIGHT_UTC = OffsetTime.of(8, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetTime TEN_UTC = OffsetTime.of(10, 0, 0, 0, ZoneOffset.UTC);
    // 09:00+03:00 的绝对时刻为 06:00Z
    private static final OffsetTime NINE_AT_PLUS3 = OffsetTime.of(9, 0, 0, 0, ZoneOffset.ofHours(3));

    // ===== 构造（按绝对时刻） =====

    @Nested
    @DisplayName("构造（按绝对时刻）")
    class ConstructTest {

        @Test
        @DisplayName("开始本地时间较晚但绝对时刻较早时为正常区间")
        void testCrossOffsetValidRange() {
            OffsetTimeRange range = assertDoesNotThrow(() -> new OffsetTimeRange(TEN_AT_PLUS2, NINE_THIRTY_UTC));
            assertTrue(range.contains(NINE_THIRTY_UTC));
        }
    }

    // ===== 包含判断（按绝对时刻） =====

    @Nested
    @DisplayName("包含判断（按绝对时刻）")
    class ContainsTest {

        @Test
        @DisplayName("绝对时刻落在区间内即包含，即使本地时间早于开始")
        void testContainsByInstantInside() {
            OffsetTimeRange range = new OffsetTimeRange(TEN_AT_PLUS2, NOON_UTC);
            assertTrue(range.contains(NINE_THIRTY_UTC));
        }

        @Test
        @DisplayName("绝对时刻早于开始时不包含，即使本地时间晚于开始")
        void testContainsByInstantBeforeStart() {
            OffsetTimeRange range = new OffsetTimeRange(EIGHT_UTC, TEN_UTC);
            assertFalse(range.contains(NINE_AT_PLUS3));
        }

        @Test
        @DisplayName("同偏移量下与普通时刻区间行为一致")
        void testSameOffsetBehavior() {
            OffsetTimeRange range = new OffsetTimeRange(EIGHT_UTC, TEN_UTC);
            assertTrue(range.contains(EIGHT_UTC));
            assertTrue(range.contains(NINE_THIRTY_UTC));
            assertTrue(range.contains(TEN_UTC));
            assertFalse(range.contains(NOON_UTC));
        }
    }
}
