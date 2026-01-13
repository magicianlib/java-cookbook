package io.ituknown.jackson;

public class SerializationException extends RuntimeException {
    private static final long serialVersionUID = 6475435719302659535L;

    private static final String DEFAULT_ERROR = "serialize failed. ";
    private static final String SPECIFIED_CLASS_ERROR = "serialize for class [%s] failed.";

    private Class<?> serializedClass;

    public SerializationException() {
        super();
    }

    public SerializationException(Class<?> serializedClass) {
        super(String.format(SPECIFIED_CLASS_ERROR, serializedClass.getName()));
        this.serializedClass = serializedClass;
    }

    public SerializationException(Throwable throwable) {
        super(DEFAULT_ERROR, throwable);
    }

    public SerializationException(Class<?> serializedClass, Throwable throwable) {
        super(String.format(SPECIFIED_CLASS_ERROR, serializedClass.getName()), throwable);
        this.serializedClass = serializedClass;
    }

    public Class<?> getSerializedClass() {
        return serializedClass;
    }
}