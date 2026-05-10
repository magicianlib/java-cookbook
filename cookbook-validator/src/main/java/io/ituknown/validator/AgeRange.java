package io.ituknown.validator;

import io.ituknown.validator.AgeRange.List;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 约束标注字段的年龄值必须在 [{@link #min()}, {@link #max()}] 范围内。
 *
 * <p>支持在同一个元素上重复标注（通过 {@link List}）。
 * 当值为 {@code null} 时视为有效，如需非空请搭配 {@code @NotNull}。</p>
 *
 * @author magicianlib@gmail.com
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Repeatable(List.class)
@Documented
@Constraint(validatedBy = AgeRangeValidator.class)
public @interface AgeRange {

    /** 校验失败时的消息模板，默认从资源文件加载。 */
    String message() default "{io.ituknown.validator.AgeRange.message}";

    /** 允许的最小年龄（含）。 */
    int min() default 1;

    /** 允许的最大年龄（含）。 */
    int max() default 100;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Defines several {@code @AgeRange} constraints on the same element.
     *
     * @see AgeRange
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Documented
    @interface List {
        AgeRange[] value();
    }
}