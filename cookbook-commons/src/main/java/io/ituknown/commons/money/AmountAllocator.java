package io.ituknown.commons.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 金额按权重分摊工具类（基于 Weight 对象）
 * 使用最大余额法确保分摊总额等于原始金额，结果直接写入每个 Weight 对象的 result 字段。
 */
public class AmountAllocator {

    /**
     * 默认保留两位小数（精确到分）
     */
    public static <K> void allocate(BigDecimal totalAmount, List<Weight<K>> weights) {
        allocate(totalAmount, weights, 2);
    }

    /**
     * 按权重分摊金额，结果设置到每个 Weight 对象的 result 字段
     *
     * @param totalAmount 总金额（不能为 null，必须 ≥ 0）
     * @param weights     权重列表（不能为 null 或空，每个 Weight 对象的 weight 必须 ≥ 0，权重总和 > 0）
     * @param scale       保留小数位数（如 2 表示分）
     * @param <K>         业务标识类型
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public static <K> void allocate(BigDecimal totalAmount, List<Weight<K>> weights, int scale) {
        if (totalAmount == null || weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("总金额和权重列表不能为空");
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("总金额不能为负数");
        }
        if (scale < 0) {
            throw new IllegalArgumentException("小数位数不能为负数");
        }

        // 总金额为0, 无需后续计算
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            weights.forEach(w -> w.setResult(BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP)));
            return;
        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Weight<K> w : weights) {
            if (w.getWeight() == null || w.getWeight().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("权重不能为负数或 null");
            }
            totalWeight = totalWeight.add(w.getWeight());
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("权重之和必须大于 0");
        }

        // 对齐总金额精度, 防止 totalAmount 传入 "100.005" 超出 scale 导致丢失残值或校验报错
        BigDecimal normalizedTotal = totalAmount.setScale(scale, RoundingMode.HALF_UP);

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

        // 直接移动小数点转 int，例如剩余 0.03元，scale=2 时，直接转换为 3
        int remainCount = remainder.movePointRight(scale).intValueExact();

        // 将条目按残差从大到小降序排列
        items.sort((a, b) -> b.fraction.compareTo(a.fraction));

        // 定义最小分配单位（例如 scale=2 时，unit = 0.01）
        BigDecimal unit = BigDecimal.valueOf(1, scale);
        BigDecimal sumResult = BigDecimal.ZERO;

        // 结果回写
        for (int i = 0; i < items.size(); i++) {
            AllocateItem item = items.get(i);
            BigDecimal finalResult = item.floorValue;

            // 前 remainCount 个残差最大的项，额外补偿 1 个最小单位
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
}