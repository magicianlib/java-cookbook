package io.magician.result.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Constraint(validatedBy = RegexConstraintValidator.class)
public @interface IntegerRegex {
    String message() default "{io.magician.result.validator.IntegerRegex.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}