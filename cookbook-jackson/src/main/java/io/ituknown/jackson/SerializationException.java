package io.ituknown.jackson;

import lombok.Getter;

import java.io.Serial;

/**
 * 序列化异常
 *
 * @author magicianlib@gmail.com
 */
@Getter
public class SerializationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6475435719302659535L;

    private static final String DEFAULT_ERROR = "Failed to serialize";
    private static final String SPECIFIED_CLASS_ERROR = "Failed to serialize class [%s]";

    private Class<?> sourceClass;

    public SerializationException() {
        super();
    }

    public SerializationException(Class<?> sourceClass) {
        super(String.format(SPECIFIED_CLASS_ERROR, sourceClass.getName()));
        this.sourceClass = sourceClass;
    }

    public SerializationException(Throwable throwable) {
        super(DEFAULT_ERROR, throwable);
    }

    public SerializationException(Class<?> sourceClass, Throwable throwable) {
        super(String.format(SPECIFIED_CLASS_ERROR, sourceClass.getName()), throwable);
        this.sourceClass = sourceClass;
    }

}