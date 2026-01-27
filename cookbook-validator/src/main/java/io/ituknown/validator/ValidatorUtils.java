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
import java.util.Objects;
import java.util.Set;

public class ValidatorUtils {

    private static final Validator VALIDATOR;

    static {
        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class)
                .configure()
                .failFast(true)
                .defaultLocale(Locale.getDefault())
                .failFast(true);

        try (ValidatorFactory factory = configuration.buildValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }

       /* ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();*/
    }

    private static void requireNonNull() {
        Objects.requireNonNull(VALIDATOR, "validator is null");
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        User u = new User();
        u.setUsername("222");
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
