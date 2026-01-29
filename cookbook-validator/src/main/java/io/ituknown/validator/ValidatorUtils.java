package io.ituknown.validator;

import jakarta.validation.*;
import jakarta.validation.groups.Default;
import org.hibernate.validator.HibernateValidator;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ValidatorUtils {

    private static final ValidatorFactory VALIDATOR_FACTORY;
    private static final Map<Locale, Validator> VALIDATOR_CACHE = new ConcurrentHashMap<>();

    static {
        try {
            VALIDATOR_FACTORY = Validation.byProvider(HibernateValidator.class)
                    .configure()
                    .failFast(true)
                    .buildValidatorFactory();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize Validator: " + e.getMessage());
        }
    }

    public static Validator getValidator() {
        return getValidator(Locale.getDefault());
    }

    public static Validator getValidator(Locale locale) {
        return VALIDATOR_CACHE.computeIfAbsent(locale, key -> {
            MessageInterpolator interpolator = VALIDATOR_FACTORY.getMessageInterpolator();
            return VALIDATOR_FACTORY.usingContext()
                    .messageInterpolator(new LocaleSpecificMessageInterpolator(interpolator, key))
                    .getValidator();
        });
    }

    /**
     * 验证对象
     */
    public static <T> void validate(T obj) {
        validate(Locale.getDefault(), obj, Default.class);
    }

    public static <T> void validate(T obj, Class<?>... groups) {
        validate(Locale.getDefault(), obj, groups);
    }

    public static <T> void validate(Locale locale, T obj) {
        validate(locale, obj, Default.class);
    }

    public static <T> void validate(Locale locale, T obj, Class<?>... groups) {
        Set<ConstraintViolation<T>> validate = getValidator(locale).validate(obj, groups);
        for (ConstraintViolation<T> violation : validate) {
            throw new IllegalArgumentException(violation.getMessage());
            //throw new IllegalArgumentException(violation.getPropertyPath() + " " + violation.getMessage());
        }
    }

    /**
     * 验证指定属性
     */
    public static <T> void validateProperty(T obj, String propertyName) {
        validateProperty(Locale.getDefault(), obj, propertyName, Default.class);
    }

    public static <T> void validateProperty(T obj, String propertyName, Class<?>... groups) {
        validateProperty(Locale.getDefault(), obj, propertyName, groups);
    }

    public static <T> void validateProperty(Locale locale, T obj, String propertyName) {
        validateProperty(locale, obj, propertyName, Default.class);
    }

    public static <T> void validateProperty(Locale locale, T obj, String propertyName, Class<?>... groups) {
        Set<ConstraintViolation<T>> validate = getValidator(locale).validateProperty(obj, propertyName, groups);
        for (ConstraintViolation<T> violation : validate) {
            throw new IllegalArgumentException(violation.getMessage());
            //throw new IllegalArgumentException(propertyName + "：" + violation.getMessage());
        }
    }
}