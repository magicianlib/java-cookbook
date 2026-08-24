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
}
