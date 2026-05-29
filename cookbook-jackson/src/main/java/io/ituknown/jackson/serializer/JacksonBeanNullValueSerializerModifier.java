package io.ituknown.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Jackson 序列化时对 NULL 值处理
 *
 * @author magicianlib@gmail.com
 */
public class JacksonBeanNullValueSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {

        for (BeanPropertyWriter writer : beanProperties) {

            JavaType javaType = writer.getType();
            Class<?> rawClass = javaType.getRawClass();

            if (javaType.isMapLikeType()) {
                // Map Fill {}
                writer.assignNullSerializer(NullMapSerializer.INSTANCE);

            } else if (javaType.isArrayType() || javaType.isCollectionLikeType()) {
                // Collection Fill []
                writer.assignNullSerializer(NullCollectionSerializer.INSTANCE);

            } else if (Boolean.class.isAssignableFrom(rawClass)) {
                // Boolean Fill false
                writer.assignNullSerializer(NullBooleanSerializer.INSTANCE);

            } else if (BigDecimal.class.isAssignableFrom(rawClass)) {
                // BigDecimal Fill ZERO
                writer.assignNullSerializer(NullBigDecimalSerializer.INSTANCE);

            } else if (Number.class.isAssignableFrom(rawClass)) {
                // Number Fill 0 (Integer, Long, Double, etc.)
                writer.assignNullSerializer(NullNumberSerializer.INSTANCE);

            } else if (String.class.isAssignableFrom(rawClass)) {
                // String Fill ""
                writer.assignNullSerializer(NullStringSerializer.INSTANCE);

            }
        }
        return beanProperties;
    }


    public static class NullMapSerializer extends JsonSerializer<Object> {
        public static final NullMapSerializer INSTANCE = new NullMapSerializer();

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            // {}
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }

    public static class NullCollectionSerializer extends JsonSerializer<Object> {
        public static final NullCollectionSerializer INSTANCE = new NullCollectionSerializer();

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            // []
            gen.writeStartArray();
            gen.writeEndArray();
        }
    }

    public static class NullBooleanSerializer extends JsonSerializer<Object> {
        public static final NullBooleanSerializer INSTANCE = new NullBooleanSerializer();

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeBoolean(false);
        }
    }

    public static class NullBigDecimalSerializer extends JsonSerializer<Object> {
        public static final NullBigDecimalSerializer INSTANCE = new NullBigDecimalSerializer();

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString("0");
        }
    }

    public static class NullNumberSerializer extends JsonSerializer<Object> {
        public static final NullNumberSerializer INSTANCE = new NullNumberSerializer();

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeNumber(0);
        }
    }

    public static class NullStringSerializer extends JsonSerializer<Object> {
        public static final NullStringSerializer INSTANCE = new NullStringSerializer();

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString("");
        }
    }
}
