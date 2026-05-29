package io.ituknown.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ituknown.datetime.DateFormatUtils;
import io.ituknown.jackson.serializer.JacksonBeanNullValueSerializerModifier;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

/**
 * Jackson Config
 *
 * @author magicianlib@gmail.com
 */
public final class JacksonConfig {

    /**
     * 配置 Java8 日期处理格式
     *
     * @param objectMapper 实例
     */
    public static void configureObjectMapperForJsr310(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用 JSR310 将日期时间写为时间戳的特性 默认行为，必须禁用才能使用后面的字符串格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // LocalTime → "10:30:45"
        configureFormat(objectMapper, LocalTime.class, DateFormatUtils.TIME_PATTERN);
        // LocalDate → "2025-06-15"
        configureFormat(objectMapper, LocalDate.class, DateFormatUtils.DATE_PATTERN);
        // LocalDateTime → "2025-06-15 10:30:45"
        configureFormat(objectMapper, LocalDateTime.class, DateFormatUtils.DATE_TIME_PATTERN);
        // OffsetDateTime → "2025-06-15T10:30:45+08:00"
        configureFormat(objectMapper, OffsetDateTime.class, DateFormatUtils.ISO_OFFSET_DATE_TIME_PATTERN);
        // OffsetTime → "10:30:45+08:00"
        configureFormat(objectMapper, OffsetTime.class, DateFormatUtils.ISO_OFFSET_TIME_PATTERN);
        // ZonedDateTime → "2025-06-15 10:30:45.000 +08:00 [Asia/Shanghai]"
        configureFormat(objectMapper, ZonedDateTime.class, DateFormatUtils.ZONED_DATE_TIME_MILLIS_PATTERN);
        // Instant → "2025-06-15T02:30:45Z"
        configureFormat(objectMapper, Instant.class, DateFormatUtils.ISO_UTC_DATE_TIME_PATTERN);
    }

    /**
     * 配置序列化时对 Null 值的默认处理：Map → {}、Collection → []、Boolean → false、BigDecimal → "0"、Number → 0、String → ""
     *
     * @param objectMapper 实例
     */
    public static void configureNullValueSerialization(ObjectMapper objectMapper) {

        SerializerFactory serializerFactory = objectMapper.getSerializerFactory()
                .withSerializerModifier(new JacksonBeanNullValueSerializerModifier());

        objectMapper.setSerializerFactory(serializerFactory);
    }

    /**
     * 注册自定义序列化器
     *
     * @param objectMapper 实例
     * @param type         目标类型
     * @param serializer   序列化器
     */
    public static <T> void registerModule(ObjectMapper objectMapper, Class<? extends T> type, JsonSerializer<T> serializer) {

        SimpleModule module = new SimpleModule();
        module.addSerializer(type, serializer);

        objectMapper.registerModule(module);
    }

    /**
     * 注册自定义反序列化器
     *
     * @param objectMapper  实例
     * @param type          目标类型
     * @param deserializer  反序列化器
     */
    public static <T> void registerModule(ObjectMapper objectMapper, Class<T> type, JsonDeserializer<? extends T> deserializer) {

        SimpleModule module = new SimpleModule();
        module.addDeserializer(type, deserializer);

        objectMapper.registerModule(module);
    }

    private static void configureFormat(ObjectMapper objectMapper, Class<?> type, String pattern) {
        JsonFormat.Value format = JsonFormat.Value.forShape(JsonFormat.Shape.STRING).withPattern(pattern);
        objectMapper.configOverride(type).setFormat(format);
    }
}
