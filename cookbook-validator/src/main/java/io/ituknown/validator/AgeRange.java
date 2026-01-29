package io.ituknown.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(List.class)
@Documented
@Constraint(validatedBy = RegexConstraintValidator.class)
public @interface AgeRange {
    String message() default "{io.magician.result.validator.IntegerRegex.message}";

    int min() default 1;
    int max() default 100;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
	  @Retention(RUNTIME)
	  @Documented
	  @interface List {
		    AgeRange[] value();
	  }
}
