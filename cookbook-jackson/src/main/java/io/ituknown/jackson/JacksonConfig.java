package io.ituknown.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ituknown.datetime.DateFormatUtils;
import io.ituknown.jackson.serializer.JacksonBeanNullValueSerializerModifier;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.TimeZone;

/**
 * Jackson Config
 *
 * @author magicianlib@gmail.com
 */
public final class JacksonConfig {

    /**
     * 配置 Java8（{@code java.time.*}） 日期处理格式
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

    private static void configureFormat(ObjectMapper objectMapper, Class<?> type, String pattern) {
        JsonFormat.Value format = JsonFormat.Value.forShape(JsonFormat.Shape.STRING).withPattern(pattern);
        objectMapper.configOverride(type).setFormat(format);
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
     * @param objectMapper 实例
     * @param type         目标类型
     * @param deserializer 反序列化器
     */
    public static <T> void registerModule(ObjectMapper objectMapper, Class<T> type, JsonDeserializer<? extends T> deserializer) {

        SimpleModule module = new SimpleModule();
        module.addDeserializer(type, deserializer);

        objectMapper.registerModule(module);
    }

    /**
     * 对 MapperBuilder 应用通用配置（适用于 JSON、XML、MessagePack 等所有格式）
     *
     * <p>配置项包括：</p>
     * <ul>
     *     <li>忽略未知属性</li>
     *     <li>序列化时跳过 {@code null} 值</li>
     *     <li>设置默认时区</li>
     *     <li>忽略 {@code transient} 字段</li>
     *     <li>设置 {@code java.util.Date} 日期格式</li>
     * </ul>
     *
     * @param builder MapperBuilder 实例（JsonMapper.Builder、XmlMapper.Builder 等）
     */
    public static void applyCommonBuilderConfig(MapperBuilder<?, ?> builder, boolean format) {
        // 格式化输出
        builder.configure(SerializationFeature.INDENT_OUTPUT, format);
        // 忽略未知字段
        builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 序列化时忽略空值
        builder.defaultPropertyInclusion(
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL)
        );

        // 忽略 transient 字段
        builder.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);

        // 设置时区
        builder.defaultTimeZone(TimeZone.getDefault());

        // java.util.Date 日期格式处理
        builder.defaultDateFormat(new SimpleDateFormat(DateFormatUtils.DATE_TIME_PATTERN));
    }
}