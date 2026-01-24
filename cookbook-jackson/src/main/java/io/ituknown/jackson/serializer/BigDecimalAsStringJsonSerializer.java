package io.ituknown.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;


/**
 * Jackson 序列化 BigDecimal 时默认展示的是数字，示例：
 * <pre>{@code
 * BigDecimal money = new BigDecimal("1.01"); // 输出 1.01
 * }</pre>
 * <p>
 * 该扩展用于将其转化为字符串, 使其序列化后的值为 "1.01"
 *
 * @author magicianlib@gmail.com
 */
public class BigDecimalAsStringJsonSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toPlainString());
    }
}