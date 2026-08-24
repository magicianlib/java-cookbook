package io.ituknown.commons.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 金额按权重分摊工具类（基于 Weight 对象）
 * 采用最大余额法：各份先按权重占比向下取整，剩余零头按残差从大到小逐份补偿一个最小单位，
 * 分摊结果之和恒等于按精度取整后的总金额。
 * 另提供保底分摊：先按权重分摊，不足保底的项补足到保底，补足差额由其余项按权重承担。
 */
public class AmountAllocator {

    /**
     * 按权重分摊金额，默认保留两位小数（精确到分）
     */
    public static <K> void allocate(BigDecimal totalAmount, List<Weight<K>> weights) {
        allocate(totalAmount, weights, 2);
    }

    /**
     * 按权重分摊金额，结果回写到每个权重对象
     * <p>
     * 总金额先按保留位数四舍五入，各分摊结果之和等于取整后的总金额；
     * 权重为零的项不分得金额；残差相同时列表靠前的项优先获得补偿。
     *
     * @param totalAmount 总金额（不能为 null，必须 ≥ 0）
     * @param weights     权重列表（不能为 null 或空；单项不能为 null，权重值不能为 null 或负数，权重总和必须大于 0）
     * @param scale       保留小数位数（如 2 表示分）
     * @param <K>         业务标识类型
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public static <K> void allocate(BigDecimal totalAmount, List<Weight<K>> weights, int scale) {
        BigDecimal normalizedTotal = validate(totalAmount, weights, scale);
        allocateByWeight(normalizedTotal, weights, scale);
    }

    /**
     * 最大余额法按权重分摊并回写结果（入参需已通过校验）
     */
    private static <K> void allocateByWeight(BigDecimal normalizedTotal, List<Weight<K>> weights, int scale) {
        // 金额为零, 无需分摊
        if (normalizedTotal.signum() == 0) {
            weights.forEach(w -> w.setResult(BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP)));
            return;
        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Weight<K> w : weights) {
            totalWeight = totalWeight.add(w.getWeight());
        }

        // 局部辅助类
        class AllocateItem {
            final Weight<K> weightObj;
            final BigDecimal floorValue; // 向下取整的基础金额
            final BigDecimal fraction; // 理论值截断后的小数残差

            AllocateItem(Weight<K> weightObj, BigDecimal floorValue, BigDecimal fraction) {
                this.weightObj = weightObj;
                this.floorValue = floorValue;
                this.fraction = fraction;
            }
        }

        int tmpScale = scale + 10;
        BigDecimal sumFloor = BigDecimal.ZERO;
        List<AllocateItem> items = new ArrayList<>(weights.size());

        // 计算基础金额与残差
        for (Weight<K> w : weights) {
            // 计算理论值 (总金额 * 权重占比)
            BigDecimal theoretical = normalizedTotal.multiply(w.getWeight())
                    .divide(totalWeight, tmpScale, RoundingMode.HALF_EVEN);

            // 截断至目标精度 (基础金额)
            BigDecimal floorValue = theoretical.setScale(scale, RoundingMode.FLOOR);
            // 记录被截去的小数部分 (残差)
            BigDecimal fraction = theoretical.subtract(floorValue);

            items.add(new AllocateItem(w, floorValue, fraction));
            sumFloor = sumFloor.add(floorValue);
        }

        // 最大余额法分配残差

        // 算出还需要补偿多少个最小单位
        BigDecimal remainder = normalizedTotal.subtract(sumFloor);

        // 将剩余金额换算为最小单位的个数(例如剩余 0.03 元、精确到分时为 3)
        int remainCount = remainder.movePointRight(scale).intValueExact();

        // 按残差从大到小排序; 排序稳定, 残差相同时保持原有顺序
        items.sort((a, b) -> b.fraction.compareTo(a.fraction));

        // 定义最小分配单位（例如 scale=2 时，unit = 0.01）
        BigDecimal unit = BigDecimal.valueOf(1, scale);
        BigDecimal sumResult = BigDecimal.ZERO;

        // 结果回写
        for (int i = 0; i < items.size(); i++) {
            AllocateItem item = items.get(i);
            BigDecimal finalResult = item.floorValue;

            // 残差最大的前若干项(个数即剩余单位数)各补偿一个最小单位
            if (i < remainCount) {
                finalResult = finalResult.add(unit);
            }

            item.weightObj.setResult(finalResult);
            sumResult = sumResult.add(finalResult);
        }

        // 安全校验
        if (sumResult.compareTo(normalizedTotal) != 0) {
            throw new IllegalStateException("分摊总和与总金额不匹配，预期: " + normalizedTotal + ", 实际: " + sumResult);
        }
    }

    /**
     * 按权重分摊金额并保证每份至少分得保底金额，默认保留两位小数（精确到分）
     */
    public static <K> void allocateWithMiniAmount(BigDecimal totalAmount, List<Weight<K>> weights, BigDecimal minAmount) {
        allocateWithMiniAmount(totalAmount, weights, 2, minAmount);
    }

    /**
     * 按权重分摊金额并保证每份至少分得保底金额，结果回写到每个权重对象
     * <p>
     * 先按权重分摊，结果不足保底金额的项补足到保底金额（按精度向下取整），
     * 补足差额由其余项按权重重新分摊承担，迭代至所有项不低于保底；
     * 各分摊结果之和恒等于取整后的总金额，每份（含零权重项）至少分得保底金额。
     *
     * @param totalAmount 总金额（不能为 null，必须 ≥ 0）
     * @param weights     权重列表（不能为 null 或空；单项不能为 null，权重值不能为 null 或负数，权重总和必须大于 0）
     * @param scale       保留小数位数（如 2 表示分）
     * @param minAmount   保底分摊金额（不能为 null 或负数，且不能大于平均分摊金额）
     * @param <K>         业务标识类型
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public static <K> void allocateWithMiniAmount(BigDecimal totalAmount, List<Weight<K>> weights, int scale, BigDecimal minAmount) {
        BigDecimal normalizedTotal = validate(totalAmount, weights, scale);
        if (minAmount == null || minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("保底分摊金额不能为 null 或负数");
        }

        // 保底放大到全部份数后不能超过总金额, 即保底不能大于平均分摊金额
        if (minAmount.multiply(BigDecimal.valueOf(weights.size())).compareTo(normalizedTotal) > 0) {
            throw new IllegalArgumentException("保底分摊金额不能大于平均分摊金额");
        }

        // 保底向下对齐目标精度, 避免取整放大后保底总额超出总金额
        BigDecimal floor = minAmount.setScale(scale, RoundingMode.FLOOR);

        // 保底取整后为零, 等价于普通按权重分摊
        if (floor.signum() == 0) {
            allocateByWeight(normalizedTotal, weights, scale);
            return;
        }

        // 迭代补足: 每轮按权重分摊剩余金额, 不足保底者固定为保底并从后续轮次剔除
        List<Weight<K>> pool = new ArrayList<>(weights);
        BigDecimal remaining = normalizedTotal;
        while (true) {
            allocateByWeight(remaining, pool, scale);

            List<Weight<K>> belowFloor = new ArrayList<>();
            for (Weight<K> w : pool) {
                if (w.getResult().compareTo(floor) < 0) {
                    belowFloor.add(w);
                }
            }
            if (belowFloor.isEmpty()) {
                break;
            }

            for (Weight<K> w : belowFloor) {
                w.setResult(floor);
            }
            remaining = remaining.subtract(floor.multiply(BigDecimal.valueOf(belowFloor.size())));
            pool.removeAll(belowFloor);
            if (pool.isEmpty()) {
                break;
            }
        }

        // 总额守恒安全校验
        BigDecimal sumResult = BigDecimal.ZERO;
        for (Weight<K> w : weights) {
            sumResult = sumResult.add(w.getResult());
        }
        if (sumResult.compareTo(normalizedTotal) != 0) {
            throw new IllegalStateException("分摊总和与总金额不匹配，预期: " + normalizedTotal + ", 实际: " + sumResult);
        }
    }

    /**
     * 校验分摊入参，返回按精度取整后的总金额
     */
    private static <K> BigDecimal validate(BigDecimal totalAmount, List<Weight<K>> weights, int scale) {
        if (totalAmount == null || weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("总金额和权重列表不能为空");
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("总金额不能为负数");
        }
        if (scale < 0) {
            throw new IllegalArgumentException("小数位数不能为负数");
        }

        // 无论金额多少都先校验并汇总权重, 保证校验口径一致
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Weight<K> w : weights) {
            if (w == null || w.getWeight() == null || w.getWeight().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("权重项不能为 null，权重值不能为 null 或负数");
            }
            totalWeight = totalWeight.add(w.getWeight());
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("权重之和必须大于 0");
        }

        // 总金额先对齐目标精度, 避免超出精度的零头丢失或破坏总额守恒
        return totalAmount.setScale(scale, RoundingMode.HALF_UP);
    }
}
