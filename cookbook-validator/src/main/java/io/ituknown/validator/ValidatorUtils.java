package io.ituknown.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ValidatorUtils {

    private static final ValidatorFactory VALIDATOR_FACTORY;
    private static final Map<Locale, Validator> VALIDATOR_CACHE = new ConcurrentHashMap<>();

    static {
        try {
            VALIDATOR_FACTORY = Validation
                    .byProvider(HibernateValidator.class)
                    .configure()
                    .failFast(true)
                    .buildValidatorFactory();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize Validator: " + e.getMessage());
        }
    }

    public static Validator getValidator(Locale locale) {


        return VALIDATOR_CACHE.computeIfAbsent(locale, l -> VALIDATOR_FACTORY.usingContext()
                .messageInterpolator(new LocaleSpe)
                .getValidator());
    }

    private static void requireNonNull() {
        Objects.requireNonNull(VALIDATOR, "validator is null");
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        User u = new User();
        u.setUsername("222");
        u.setAge(101);
        validate(u);
    }

    public static <T> void validate(T obj) {
        validate(obj, Default.class);
    }

    public static <T> void validate(T obj, Class<?>... groups) {
        requireNonNull();
        Set<ConstraintViolation<T>> validate = VALIDATOR.validate(obj, groups);
        for (ConstraintViolation<T> violation : validate) {
            throw new IllegalArgumentException(violation.getPropertyPath() + " " + violation.getMessage());
        }
    }

    public static <T> void validateProperty(T obj, String propertyName, Class<?>... groups) {
        requireNonNull();
        Set<ConstraintViolation<T>> validate = VALIDATOR.validateProperty(obj, propertyName, groups);
        for (ConstraintViolation<T> violation : validate) {
            throw new IllegalArgumentException(propertyName + "：" + violation.getMessage());
        }
    }

    @Setter
    @Getter
    public static class User {
        @NotBlank(groups = Modify.class)
        private String username;
        @IntegerRegex
        private Integer age;

        public @interface Modify {
        }
    }
}
