package io.ituknown.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AgeRangeConstraintValidator implements ConstraintValidator<AgeRange, Integer> {
    private AgeRange annotation;

    @Override
    public void initialize(AgeRange constraintAnnotation) {
        annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(integerRegex.message()).addConstraintViolation();
        if (value > annotation.max() || value < annotationmin()) {
            return false;
        }
        return true;
    }
}
