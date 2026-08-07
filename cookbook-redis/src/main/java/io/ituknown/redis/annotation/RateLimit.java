package io.ituknown.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解用于配置方法级别请求频率限制
 * <p>
 * 使用该注解的前提是启用声明式限流功能{@link EnableRateLimit}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流键的 SpEL 表达式，用来把不同调用分到不同的配额桶。
     * <p>留空时，同一个方法的所有调用共用一个桶；填了表达式就按求值结果分桶，结果为 null 时退回方法级限流。
     *
     * <p><b>用法示例</b>：
     * <pre>
     * // 方法级：该接口所有调用共享配额
     * key = ""
     *
     * // 方法指定值：userId 时方法参数名，按 userId 参数值分别计数（入参为简单类型）
     * key = "#userId"
     *
     * // 方法指定对象属性值：同 #userId，区别是从参数对象里获取（需有对应 getter）
     * key = "#req.userId"
     *
     * // 组合维度：多标识拼接为一个限流维度
     * key = "#req.userId + ':' + #req.tenantId"
     * </pre>
     *
     * <p><b>注意</b>：
     * <ul>
     *   <li>按入参名引用需编译期保留参数名（{@code -parameters}），否则 {@code #参数名} 无法解析；</li>
     *   <li>对象属性导航按 JavaBean 规则取值，需存在对应 getter 或公有字段；</li>
     *   <li>表达式求值异常（如引用了不存在的入参）不在故障开放范围内，会直接抛给调用方，
     *       建议给维度表达式补个求值单测。</li>
     * </ul>
     */
    String key() default "";

    /**
     * 允许的突发上限，留空（-1）时取配额数，桶总容量为本值加一
     */
    int maxBurst() default -1;

    /**
     * 配额统计周期，单位秒
     */
    int period() default 1;

    /**
     * 单个周期内允许的请求配额数
     */
    int count();

    /**
     * 单次请求消耗的配额数
     */
    int quantity() default 1;
}