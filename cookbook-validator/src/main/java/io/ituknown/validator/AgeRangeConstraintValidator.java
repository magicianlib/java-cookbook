package io.ituknown.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AgeRangeConstraintValidator implements ConstraintValidator<AgeRange, Integer> {
    private AgeRange annotation;

    @Override
    public void initialize(AgeRange constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(annotation.message()).addConstraintViolation();
        return value >= annotation.min() && value <= annotation.max();
    }
}