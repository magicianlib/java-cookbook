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
     * 限流维度的键表达式，采用 Spring 表达式语言（SpEL）。是声明式限流按维度细分的入口：
     * 留空时该接口所有调用共享一个配额桶；填写后按表达式求值结果各自独立计数。
     *
     * <p><b>解析步骤</b>：
     * <ol>
     *   <li>取固定前缀「简单类名#方法名」，作为限流键的命名空间，避免不同接口维度撞键；</li>
     *   <li>表达式非空时，在方法调用上下文中求值，以 {@code #参数名} 引用对应入参；</li>
     *   <li>最终键由「前缀#求值结果」拼接而成；求值结果为 {@code null} 时退化为方法签名限流，
     *       该接口所有求值为空的调用共享同一桶。</li>
     * </ol>
     *
     * <p><b>用法示例</b>：
     * <pre>
     * // 方法级：该接口所有调用共享配额
     * key = ""
     *
     * // 调用方级：按用户标识分别计数（入参为简单类型）
     * key = "#userId"
     *
     * // 对象入参：导航到对象属性，按对象内某标识分别计数（需有对应 getter）
     * key = "#req.userId"
     *
     * // 组合维度：多标识拼接为一个限流维度
     * key = "#req.userId + ':' + #req.tenantId"
     * </pre>
     *
     * <p><b>前提与注意</b>：
     * <ul>
     *   <li>按入参名引用需编译期保留参数名（{@code -parameters}），否则 {@code #参数名} 无法解析；</li>
     *   <li>对象属性导航按 JavaBean 规则取值，需存在对应 getter 或公有字段；</li>
     *   <li>表达式求值异常（如引用了不存在的入参）不在「故障开放」范围内，会直接冒泡到调用方，
     *       建议为维度表达式补充求值单测。</li>
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
