package io.ituknown.jackson.mask;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

  private SensitiveType type;

  /**
   * 默认构造函数，Jackson 实例化时需要
   */
  public SensitiveSerializer() {
  }

  /**
   * 用于 {@link #createContextual(SerializerProvider, BeanProperty)} 传递状态
   */
  public SensitiveSerializer(SensitiveType type) {
    this.type = type;
  }

  @Override
  public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(type.mask(value));
  }

  @Override
  public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
      throws JsonMappingException {

    if (property == null) {
      return prov.findNullValueSerializer(null);
    }

    Sensitive annotation = property.getAnnotation(Sensitive.class);
    if (annotation != null) {
      // 不直接修改 this.type，因为序列化器通常是单例或被缓存。确保线程安全，直接返回一个新实例
      return new SensitiveSerializer(annotation.value());
    }

    // 如果没有注解，回退到系统默认的 String 序列化器
    return prov.findValueSerializer(property.getType(), property);
  }
}