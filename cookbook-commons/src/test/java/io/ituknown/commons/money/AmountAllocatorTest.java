package io.ituknown.commons.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmountAllocatorTest {

    private static Weight<String> weight(String key, String weight) {
        return new Weight<>(key, new BigDecimal(weight));
    }

    private static BigDecimal resultOf(List<Weight<String>> weights, String key) {
        return weights.stream().filter(w -> key.equals(w.getKey())).findFirst().orElseThrow().getResult();
    }

    @Test
    public void testEqualWeightsEvenSplit() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"), weight("C", "1"), weight("D", "1"));
        AmountAllocator.allocate(new BigDecimal("100"), weights);
        assertEquals(new BigDecimal("25.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("25.00"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("25.00"), resultOf(weights, "C"));
        assertEquals(new BigDecimal("25.00"), resultOf(weights, "D"));
    }

    @Test
    public void testProportionalSplit() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "2"), weight("C", "7"));
        AmountAllocator.allocate(new BigDecimal("100"), weights);
        assertEquals(new BigDecimal("10.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("20.00"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("70.00"), resultOf(weights, "C"));
    }

    @Test
    public void testIndivisibleGoesToLargestFraction() {
        // 1.00 按 2:2:5 分摊, 三份理论值 0.2222/0.2222/0.5555, 零头补偿给残差最大的一份
        List<Weight<String>> weights = Arrays.asList(weight("A", "2"), weight("B", "2"), weight("C", "5"));
        AmountAllocator.allocate(new BigDecimal("1.00"), weights);
        assertEquals(new BigDecimal("0.22"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("0.22"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("0.56"), resultOf(weights, "C"));
    }

    @Test
    public void testLargerFractionWinsRegardlessOfOrder() {
        // 0.01 按 1:1.5 分摊, 两份理论值 0.004/0.006, 零头归残差大的后者而非列表靠前者
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1.5"));
        AmountAllocator.allocate(new BigDecimal("0.01"), weights);
        assertEquals(new BigDecimal("0.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("0.01"), resultOf(weights, "B"));
    }

    @Test
    public void testEqualFractionGoesToFirstItem() {
        List<Weight<String>> two = Arrays.asList(weight("A", "1"), weight("B", "1"));
        AmountAllocator.allocate(new BigDecimal("0.01"), two);
        assertEquals(new BigDecimal("0.01"), resultOf(two, "A"));
        assertEquals(new BigDecimal("0.00"), resultOf(two, "B"));

        List<Weight<String>> three = Arrays.asList(weight("A", "1"), weight("B", "1"), weight("C", "1"));
        AmountAllocator.allocate(new BigDecimal("100"), three);
        assertEquals(new BigDecimal("33.34"), resultOf(three, "A"));
        assertEquals(new BigDecimal("33.33"), resultOf(three, "B"));
        assertEquals(new BigDecimal("33.33"), resultOf(three, "C"));
    }

    @Test
    public void testSingleWeightTakesAll() {
        List<Weight<String>> weights = Collections.singletonList(weight("A", "7"));
        AmountAllocator.allocate(new BigDecimal("99.99"), weights);
        assertEquals(new BigDecimal("99.99"), resultOf(weights, "A"));
    }

    @Test
    public void testZeroWeightItemGetsZero() {
        // 0.01 按 1:1:0 分摊, 零权重项不参与分摊
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"), weight("C", "0"));
        AmountAllocator.allocate(new BigDecimal("0.01"), weights);
        assertEquals(new BigDecimal("0.01"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("0.00"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("0.00"), resultOf(weights, "C"));
    }

    @Test
    public void testZeroTotal() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "2"));
        AmountAllocator.allocate(BigDecimal.ZERO, weights, 3);
        assertEquals(new BigDecimal("0.000"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("0.000"), resultOf(weights, "B"));
    }

    @Test
    public void testDefaultScaleIsCent() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"));
        AmountAllocator.allocate(new BigDecimal("10"), weights);
        assertEquals(new BigDecimal("5.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("5.00"), resultOf(weights, "B"));
    }

    @Test
    public void testScaleZeroIntegerAmounts() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"), weight("C", "1"));
        AmountAllocator.allocate(new BigDecimal("10"), weights, 0);
        assertEquals(new BigDecimal("4"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("3"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("3"), resultOf(weights, "C"));
    }

    @Test
    public void testHigherPrecisionScale() {
        // 0.005 按 1:1 分摊到千分位, 两份理论值 0.0025, 零头归靠前一份
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"));
        AmountAllocator.allocate(new BigDecimal("0.005"), weights, 3);
        assertEquals(new BigDecimal("0.003"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("0.002"), resultOf(weights, "B"));
    }

    @Test
    public void testTotalAmountRoundedToScale() {
        List<Weight<String>> up = Collections.singletonList(weight("A", "1"));
        AmountAllocator.allocate(new BigDecimal("100.005"), up);
        assertEquals(new BigDecimal("100.01"), resultOf(up, "A"));

        List<Weight<String>> down = Collections.singletonList(weight("A", "1"));
        AmountAllocator.allocate(new BigDecimal("100.004"), down);
        assertEquals(new BigDecimal("100.00"), resultOf(down, "A"));
    }

    @Test
    public void testTinyTotalRoundsToZero() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"));
        AmountAllocator.allocate(new BigDecimal("0.004"), weights);
        assertEquals(new BigDecimal("0.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("0.00"), resultOf(weights, "B"));
    }

    @Test
    public void testSumInvariantOnManyWeights() {
        List<Weight<String>> weights = Arrays.asList(
                weight("A", "1"), weight("B", "2"), weight("C", "3"), weight("D", "5"),
                weight("E", "8"), weight("F", "13"), weight("G", "21"), weight("H", "34"));
        AmountAllocator.allocate(new BigDecimal("12345.67"), weights);

        BigDecimal sum = BigDecimal.ZERO;
        for (Weight<String> w : weights) {
            assertEquals(2, w.getResult().scale());
            assertTrue(w.getResult().signum() >= 0);
            sum = sum.add(w.getResult());
        }
        assertEquals(new BigDecimal("12345.67"), sum);
    }

    @Test
    public void testOriginalListOrderPreserved() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "5"), weight("B", "1"), weight("C", "1"));
        AmountAllocator.allocate(new BigDecimal("100"), weights);
        assertEquals("A", weights.get(0).getKey());
        assertEquals("B", weights.get(1).getKey());
        assertEquals("C", weights.get(2).getKey());
    }

    @Test
    public void testNullTotalRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocate(null, Collections.singletonList(weight("A", "1"))));
    }

    @Test
    public void testNullWeightsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocate(BigDecimal.ONE, null));
    }

    @Test
    public void testEmptyWeightsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocate(BigDecimal.ONE, Collections.emptyList()));
    }

    @Test
    public void testNegativeTotalRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocate(new BigDecimal("-0.01"), Collections.singletonList(weight("A", "1"))));
    }

    @Test
    public void testNegativeScaleRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocate(BigDecimal.ONE, Collections.singletonList(weight("A", "1")), -1));
    }

    @Test
    public void testNegativeWeightRejected() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "-1"), weight("B", "2"));
        assertThrows(IllegalArgumentException.class, () -> AmountAllocator.allocate(BigDecimal.ONE, weights));
    }

    @Test
    public void testNullWeightValueRejected() {
        List<Weight<String>> weights = Collections.singletonList(new Weight<>("A", null));
        assertThrows(IllegalArgumentException.class, () -> AmountAllocator.allocate(BigDecimal.ONE, weights));
    }

    @Test
    public void testNullWeightElementRejected() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), null);
        assertThrows(IllegalArgumentException.class, () -> AmountAllocator.allocate(BigDecimal.ONE, weights));
    }

    @Test
    public void testAllZeroWeightsRejected() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "0"), weight("B", "0"));
        assertThrows(IllegalArgumentException.class, () -> AmountAllocator.allocate(BigDecimal.ONE, weights));
    }

    @Test
    public void testZeroTotalStillValidatesWeights() {
        List<Weight<String>> negative = Arrays.asList(weight("A", "-1"), weight("B", "2"));
        assertThrows(IllegalArgumentException.class, () -> AmountAllocator.allocate(BigDecimal.ZERO, negative));

        List<Weight<String>> allZero = Arrays.asList(weight("A", "0"), weight("B", "0"));
        assertThrows(IllegalArgumentException.class, () -> AmountAllocator.allocate(BigDecimal.ZERO, allZero));
    }

    @Test
    public void testFloorEqualsPlainAllocateWhenMinZero() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "2"), weight("C", "7"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), weights, BigDecimal.ZERO);
        assertEquals(new BigDecimal("10.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("20.00"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("70.00"), resultOf(weights, "C"));
    }

    @Test
    public void testFloorLiftsSmallWeightItem() {
        // 100 按 1:19 分摊保底 10, 小权重项理论值 5 不足保底, 补足后差额由大权重项承担
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "19"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), weights, BigDecimal.TEN);
        assertEquals(new BigDecimal("10.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("90.00"), resultOf(weights, "B"));
    }

    @Test
    public void testFloorLiftsMultipleItemsInSameRound() {
        // 100 按 1:1:98 分摊保底 5, 前两项同一轮补足
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"), weight("C", "98"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), weights, BigDecimal.valueOf(5));
        assertEquals(new BigDecimal("5.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("5.00"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("90.00"), resultOf(weights, "C"));
    }

    @Test
    public void testFloorCascadesAcrossRounds() {
        // 100 按 1:2:7 分摊保底 20, 第二轮重分后次小项也跌破保底, 需迭代补足
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "2"), weight("C", "7"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), weights, BigDecimal.valueOf(20));
        assertEquals(new BigDecimal("20.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("20.00"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("60.00"), resultOf(weights, "C"));
    }

    @Test
    public void testFloorNotAppliedWhenShareExactlyAtFloor() {
        // 100 按 1:9 分摊保底 10, 小权重项理论值恰为保底值, 不触发补足
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "9"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), weights, BigDecimal.TEN);
        assertEquals(new BigDecimal("10.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("90.00"), resultOf(weights, "B"));
    }

    @Test
    public void testFloorEqualsAverageForcesEqualSplit() {
        // 保底等于平均分摊金额时, 不足者逐轮补足, 最终各项均为平均数
        List<Weight<String>> uneven = Arrays.asList(
                weight("A", "0.5"), weight("B", "0.5"), weight("C", "1"), weight("D", "2"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), uneven, BigDecimal.valueOf(25));
        assertEquals(new BigDecimal("25.00"), resultOf(uneven, "A"));
        assertEquals(new BigDecimal("25.00"), resultOf(uneven, "B"));
        assertEquals(new BigDecimal("25.00"), resultOf(uneven, "C"));
        assertEquals(new BigDecimal("25.00"), resultOf(uneven, "D"));

        List<Weight<String>> even = Arrays.asList(
                weight("A", "1"), weight("B", "1"), weight("C", "1"), weight("D", "1"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), even, BigDecimal.valueOf(25));
        assertEquals(new BigDecimal("25.00"), resultOf(even, "A"));
        assertEquals(new BigDecimal("25.00"), resultOf(even, "B"));
        assertEquals(new BigDecimal("25.00"), resultOf(even, "C"));
        assertEquals(new BigDecimal("25.00"), resultOf(even, "D"));
    }

    @Test
    public void testFloorGivesZeroWeightItemMinimum() {
        // 100 按 0:5 分摊保底 10, 零权重项依赖保底获得金额
        List<Weight<String>> weights = Arrays.asList(weight("A", "0"), weight("B", "5"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), weights, BigDecimal.TEN);
        assertEquals(new BigDecimal("10.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("90.00"), resultOf(weights, "B"));
    }

    @Test
    public void testFloorRoundsDownToScale() {
        // 保底 0.005 精确到分时向下取整为 0.00, 等价于普通分摊
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("0.01"), weights, 2, new BigDecimal("0.005"));
        assertEquals(new BigDecimal("0.01"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("0.00"), resultOf(weights, "B"));
    }

    @Test
    public void testFloorAtSubCentAverageBoundary() {
        // 平均分摊金额 50.005, 保底恰等于平均值时允许, 分摊结果各份不低于取整后的保底
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("100.01"), weights, 2, new BigDecimal("50.005"));
        assertEquals(new BigDecimal("50.01"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("50.00"), resultOf(weights, "B"));
    }

    @Test
    public void testFloorWithIntegerScale() {
        // 10 元按 1:1:8 整数分摊保底 2, 前两项补足到 2, 剩余归大权重项
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "1"), weight("C", "8"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("10"), weights, 0, BigDecimal.valueOf(2));
        assertEquals(new BigDecimal("2"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("2"), resultOf(weights, "B"));
        assertEquals(new BigDecimal("6"), resultOf(weights, "C"));
    }

    @Test
    public void testFloorWithZeroTotal() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "2"));
        AmountAllocator.allocateWithMiniAmount(BigDecimal.ZERO, weights, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), resultOf(weights, "A"));
        assertEquals(new BigDecimal("0.00"), resultOf(weights, "B"));
    }

    @Test
    public void testFloorSumInvariant() {
        List<Weight<String>> weights = Arrays.asList(
                weight("A", "1"), weight("B", "2"), weight("C", "3"), weight("D", "5"),
                weight("E", "8"), weight("F", "13"), weight("G", "21"), weight("H", "34"));
        AmountAllocator.allocateWithMiniAmount(new BigDecimal("12345.67"), weights, BigDecimal.valueOf(150));

        BigDecimal sum = BigDecimal.ZERO;
        for (Weight<String> w : weights) {
            assertEquals(2, w.getResult().scale());
            assertTrue(w.getResult().compareTo(new BigDecimal("150.00")) >= 0);
            sum = sum.add(w.getResult());
        }
        assertEquals(new BigDecimal("12345.67"), sum);
    }

    @Test
    public void testFloorExceedingAverageRejected() {
        List<Weight<String>> half = Arrays.asList(weight("A", "1"), weight("B", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocateWithMiniAmount(new BigDecimal("100"), half, new BigDecimal("50.01")));

        List<Weight<String>> cents = Arrays.asList(weight("A", "1"), weight("B", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocateWithMiniAmount(new BigDecimal("0.01"), cents, new BigDecimal("0.006")));
    }

    @Test
    public void testFloorExceedingTotalRejectedWhenTotalZero() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "2"));
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocateWithMiniAmount(BigDecimal.ZERO, weights, new BigDecimal("0.01")));
    }

    @Test
    public void testNegativeOrNullMinRejected() {
        List<Weight<String>> weights = Arrays.asList(weight("A", "1"), weight("B", "2"));
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocateWithMiniAmount(BigDecimal.TEN, weights, new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class,
                () -> AmountAllocator.allocateWithMiniAmount(BigDecimal.TEN, weights, null));
    }
}
