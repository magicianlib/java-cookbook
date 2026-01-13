package io.ituknown.jackson;

import java.lang.reflect.Type;

public class DeserializationException extends RuntimeException {
    private static final long serialVersionUID = -6514424812979501022L;

    private static final String DEFAULT_ERROR = "deserialize failed. ";
    private static final String SPECIFIED_CLASS_ERROR = "deserialize for class [%s] failed.";

    private Class<?> targetClass;

    public DeserializationException() {
        super();
    }

    public DeserializationException(Class<?> targetClass) {
        super(String.format(SPECIFIED_CLASS_ERROR, targetClass.getName()));
        this.targetClass = targetClass;
    }

    public DeserializationException(Type targetType) {
        super(String.format(SPECIFIED_CLASS_ERROR, targetType.toString()));
    }

    public DeserializationException(Throwable throwable) {
        super(DEFAULT_ERROR, throwable);
    }

    public DeserializationException(Class<?> targetClass, Throwable throwable) {
        super(String.format(SPECIFIED_CLASS_ERROR, targetClass.getName()), throwable);
        this.targetClass = targetClass;
    }

    public DeserializationException(Type targetType, Throwable throwable) {
        super(String.format(SPECIFIED_CLASS_ERROR, targetType.toString()), throwable);
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }
}