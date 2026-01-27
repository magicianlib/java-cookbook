package io.magician.result.validator;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RegexConstraintValidator implements ConstraintValidator<IntegerRegex, Integer> {
    private IntegerRegex integerRegex;

    @Override
    public void initialize(IntegerRegex constraintAnnotation) {
        integerRegex = constraintAnnotation;
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(integerRegex.message()).addConstraintViolation();
        return false;
    }
}