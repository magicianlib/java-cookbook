package io.ituknown.validator;

import jakarta.validation.*;
import jakarta.validation.groups.Default;
import org.hibernate.validator.HibernateValidator;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Bean Validation 工具类，封装 Hibernate Validator，支持按 Locale 缓存 Validator 实例。
 *
 * <p>配置了 failFast 模式（遇到第一个校验失败即返回），校验失败时抛出 {@link ConstraintViolationException}。</p>
 */
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

    /**
     * 获取默认 Locale 对应的 Validator。
     */
    public static Validator getValidator() {
        return getValidator(Locale.getDefault());
    }

    /**
     * 获取指定 Locale 对应的 Validator，使用缓存避免重复创建。
     */
    public static Validator getValidator(Locale locale) {
        return VALIDATOR_CACHE.computeIfAbsent(locale, key -> {
            MessageInterpolator interpolator = VALIDATOR_FACTORY.getMessageInterpolator();
            return VALIDATOR_FACTORY.usingContext()
                    .messageInterpolator(new LocaleSpecificMessageInterpolator(interpolator, key))
                    .getValidator();
        });
    }

    /**
     * 使用默认 Locale 和 Default 分组校验对象。
     *
     * @throws ConstraintViolationException 校验失败时
     */
    public static <T> void validate(T obj) {
        validate(Locale.getDefault(), obj, Default.class);
    }

    /**
     * 使用默认 Locale 和指定分组校验对象。
     *
     * @throws ConstraintViolationException 校验失败时
     */
    public static <T> void validate(T obj, Class<?>... groups) {
        validate(Locale.getDefault(), obj, groups);
    }

    /**
     * 使用指定 Locale 和 Default 分组校验对象。
     *
     * @throws ConstraintViolationException 校验失败时
     */
    public static <T> void validate(Locale locale, T obj) {
        validate(locale, obj, Default.class);
    }

    /**
     * 使用指定 Locale 和指定分组校验对象。
     *
     * @throws ConstraintViolationException 校验失败时
     */
    public static <T> void validate(Locale locale, T obj, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = getValidator(locale).validate(obj, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    /**
     * 使用默认 Locale 和 Default 分组校验对象的指定属性。
     *
     * @throws ConstraintViolationException 校验失败时
     */
    public static <T> void validateProperty(T obj, String propertyName) {
        validateProperty(Locale.getDefault(), obj, propertyName, Default.class);
    }

    /**
     * 使用默认 Locale 和指定分组校验对象的指定属性。
     *
     * @throws ConstraintViolationException 校验失败时
     */
    public static <T> void validateProperty(T obj, String propertyName, Class<?>... groups) {
        validateProperty(Locale.getDefault(), obj, propertyName, groups);
    }

    /**
     * 使用指定 Locale 和 Default 分组校验对象的指定属性。
     *
     * @throws ConstraintViolationException 校验失败时
     */
    public static <T> void validateProperty(Locale locale, T obj, String propertyName) {
        validateProperty(locale, obj, propertyName, Default.class);
    }

    /**
     * 使用指定 Locale 和指定分组校验对象的指定属性。
     *
     * @throws ConstraintViolationException 校验失败时
     */
    public static <T> void validateProperty(Locale locale, T obj, String propertyName, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = getValidator(locale).validateProperty(obj, propertyName, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
