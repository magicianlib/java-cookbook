package io.ituknown.jackson;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 反序列化执行支持，统一将底层读取异常包装为反序列化异常
 *
 * @author magicianlib@gmail.com
 */
final class JacksonDeserializeSupport {

    private JacksonDeserializeSupport() {
    }

    @FunctionalInterface
    interface Deserializer<T> {
        T read() throws IOException;
    }

    static <T> T deserialize(Type targetType, Deserializer<T> deserializer) {
        try {
            return deserializer.read();
        } catch (IOException e) {
            throw new DeserializationException(targetType, e);
        }
    }
}
