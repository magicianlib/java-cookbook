package io.ituknown.commons.money;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 权重载体，携带业务标识 Key、权重值和分摊结果
 *
 * @param <K> 业务标识的类型（如 String、Long 等）。
 *            如果使用自定义对象必须重写 equals 和 hashCode 方法。
 */
@Getter
@Setter
public class Weight<K> {
    private final K key;
    private final BigDecimal weight;
    private BigDecimal result;

    public Weight(K key, BigDecimal weight) {
        this.key = key;
        this.weight = weight;
        this.result = BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return String.format("Weight{key=%s, weight=%s, result=%s}", key, weight, result);
    }
}