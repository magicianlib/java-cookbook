package io.ituknown.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

import static io.ituknown.jackson.JacksonConfig.applyCommonBuilderConfig;
import static io.ituknown.jackson.JacksonConfig.configureObjectMapperForJsr310;

/**
 * Jackson MessagePack 序列化/反序列化工具类
 *
 * <p>基于 {@code jackson-dataformat-msgpack} 扩展，提供 MessagePack 二进制格式的序列化与反序列化能力。
 * 内部维护两套 {@link ObjectMapper} 实例（格式化 / 非格式化），通过 {@code format} 参数切换。
 * 所有反序列化方法均支持 {@code byte[]} 和 Base64 字符串两种输入形式。</p>
 *
 * <p><b>序列化输出：</b></p>
 * <ul>
 *     <li>{@link #toBytes} — 输出 {@code byte[]} 二进制数据</li>
 *     <li>{@link #toBase64} — 输出 Base64 编码字符串</li>
 * </ul>
 *
 * <p><b>反序列化输入：</b></p>
 * <ul>
 *     <li>{@code byte[]} — 原始 MessagePack 二进制数据</li>
 *     <li>{@link String} (base64) — Base64 编码的字符串，内部自动解码</li>
 *     <li>{@link InputStream} — 输入流</li>
 * </ul>
 *
 * @author magicianlib@gmail.com
 * @see JacksonUtils
 * @see <a href="https://github.com/msgpack/msgpack-java">msgpack-java</a>
 */
public enum MessagePackUtils {
    ;

    private static final ObjectMapper MAPPER_WITH_FORMAT = createObjectMapper(true);
    private static final ObjectMapper MAPPER_WITHOUT_FORMAT = createObjectMapper(false);

    public static ObjectMapper getObjectMapper() {
        return getObjectMapper(false);
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @param format 是否开启格式化输出
     */
    public static ObjectMapper getObjectMapper(boolean format) {
        return format ? MAPPER_WITH_FORMAT : MAPPER_WITHOUT_FORMAT;
    }

    /**
     * 创建基于 MessagePack 格式的 {@link ObjectMapper} 实例
     *
     * <p>默认配置：</p>
     * <ul>
     *     <li>忽略未知属性</li>
     *     <li>序列化时跳过 {@code null} 值</li>
     *     <li>忽略 {@code transient} 字段</li>
     *     <li>支持 {@code java.time.*} / {@code java.util.Date} 日期格式</li>
     *     <li>{@link BigDecimal} 序列化为字符串</li>
     * </ul>
     *
     * @param format 是否开启格式化输出
     * @return 配置完成的 ObjectMapper 实例
     */
    public static ObjectMapper createObjectMapper(boolean format) {
        JsonMapper.Builder builder = JsonMapper.builder(new MessagePackFactory());

        // 通用 builder 配置
        applyCommonBuilderConfig(builder, format);

        ObjectMapper mapper = builder.build();
        configureObjectMapperForJsr310(mapper);

        return mapper;
    }


    // ====================== 序列化 ======================

    /**
     * 将对象序列化为 MessagePack 二进制数据
     *
     * @param obj 待序列化对象
     * @return MessagePack 二进制数据
     * @throws SerializationException 序列化失败时抛出
     */
    public static byte[] toBytes(Object obj) {
        return toBytes(obj, false);
    }

    /**
     * 将对象序列化为 MessagePack 二进制数据
     *
     * @param obj    待序列化对象
     * @param format 是否开启格式化输出
     * @return MessagePack 二进制数据
     * @throws SerializationException 序列化失败时抛出
     */
    public static byte[] toBytes(Object obj, boolean format) {
        return toBytes(obj, getObjectMapper(format));
    }

    /**
     * 将对象序列化为 MessagePack 二进制数据
     *
     * @param obj          待序列化对象
     * @param objectMapper 自定义 ObjectMapper 实例
     * @return MessagePack 二进制数据
     * @throws SerializationException 序列化失败时抛出
     */
    public static byte[] toBytes(Object obj, final ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new SerializationException(obj.getClass(), e);
        }
    }

    /**
     * 将对象序列化为 Base64 编码字符串
     *
     * @param obj 待序列化对象
     * @return Base64 编码字符串
     * @throws SerializationException 序列化失败时抛出
     */
    public static String toBase64(Object obj) {
        return toBase64(obj, false);
    }

    /**
     * 将对象序列化为 Base64 编码字符串
     *
     * @param obj    待序列化对象
     * @param format 是否开启格式化输出
     * @return Base64 编码字符串
     * @throws SerializationException 序列化失败时抛出
     */
    public static String toBase64(Object obj, boolean format) {
        return toBase64(obj, getObjectMapper(format));
    }

    /**
     * 将对象序列化为 Base64 编码字符串
     *
     * @param obj          待序列化对象
     * @param objectMapper 自定义 ObjectMapper 实例
     * @return Base64 编码字符串
     * @throws SerializationException 序列化失败时抛出
     */
    public static String toBase64(Object obj, final ObjectMapper objectMapper) {
        return Base64.getEncoder().encodeToString(toBytes(obj, objectMapper));
    }

    // ====================== 反序列化 (Class) - byte[] ======================

    /**
     * 将 MessagePack 二进制数据反序列化为指定类型的对象
     *
     * @param rawBytes MessagePack 二进制数据
     * @param clazz    目标类型
     * @param <T>      目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, Class<T> clazz) {
        return toObj(rawBytes, clazz, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定类型的对象
     *
     * @param rawBytes MessagePack 二进制数据
     * @param clazz    目标类型
     * @param format   是否使用格式化 ObjectMapper
     * @param <T>      目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, Class<T> clazz, boolean format) {
        return toObj(rawBytes, clazz, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定类型的对象
     *
     * @param rawBytes     MessagePack 二进制数据
     * @param clazz        目标类型
     * @param objectMapper 自定义 ObjectMapper 实例
     * @param <T>          目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, Class<T> clazz, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(rawBytes, clazz);
        } catch (IOException e) {
            throw new DeserializationException(clazz, e);
        }
    }

    // ====================== 反序列化 (Class) - base64 ======================

    /**
     * 将 Base64 编码字符串反序列化为指定类型的对象
     *
     * @param base64 Base64 编码字符串
     * @param clazz  目标类型
     * @param <T>    目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, Class<T> clazz) {
        return toObj(base64, clazz, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为指定类型的对象
     *
     * @param base64 Base64 编码字符串
     * @param clazz  目标类型
     * @param format 是否使用格式化 ObjectMapper
     * @param <T>    目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, Class<T> clazz, boolean format) {
        return toObj(base64, clazz, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为指定类型的对象
     *
     * @param base64       Base64 编码字符串
     * @param clazz        目标类型
     * @param objectMapper 自定义 ObjectMapper 实例
     * @param <T>          目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, Class<T> clazz, final ObjectMapper objectMapper) {
        return toObj(Base64.getDecoder().decode(base64), clazz, objectMapper);
    }

    // ====================== 反序列化 (Type) - byte[] ======================

    /**
     * 将 MessagePack 二进制数据反序列化为指定类型的对象
     *
     * @param rawBytes MessagePack 二进制数据
     * @param type     目标类型
     * @param <T>      目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, Type type) {
        return toObj(rawBytes, type, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定类型的对象
     *
     * @param rawBytes MessagePack 二进制数据
     * @param type     目标类型
     * @param format   是否使用格式化 ObjectMapper
     * @param <T>      目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, Type type, boolean format) {
        return toObj(rawBytes, type, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定类型的对象
     *
     * @param rawBytes     MessagePack 二进制数据
     * @param type         目标类型
     * @param objectMapper 自定义 ObjectMapper 实例
     * @param <T>          目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, Type type, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(rawBytes, objectMapper.constructType(type));
        } catch (IOException e) {
            throw new DeserializationException(type, e);
        }
    }

    // ====================== 反序列化 (Type) - base64 ======================

    /**
     * 将 Base64 编码字符串反序列化为指定类型的对象
     *
     * @param base64 Base64 编码字符串
     * @param type   目标类型
     * @param <T>    目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, Type type) {
        return toObj(base64, type, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为指定类型的对象
     *
     * @param base64 Base64 编码字符串
     * @param type   目标类型
     * @param format 是否使用格式化 ObjectMapper
     * @param <T>    目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, Type type, boolean format) {
        return toObj(base64, type, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为指定类型的对象
     *
     * @param base64       Base64 编码字符串
     * @param type         目标类型
     * @param objectMapper 自定义 ObjectMapper 实例
     * @param <T>          目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, Type type, final ObjectMapper objectMapper) {
        return toObj(Base64.getDecoder().decode(base64), type, objectMapper);
    }

    // ====================== 反序列化 (InputStream + Class) ======================

    /**
     * 从输入流读取 MessagePack 数据并反序列化为指定类型的对象
     *
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param <T>         目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(InputStream inputStream, Class<T> clazz) {
        return toObj(inputStream, clazz, false);
    }

    /**
     * 从输入流读取 MessagePack 数据并反序列化为指定类型的对象
     *
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param format      是否使用格式化 ObjectMapper
     * @param <T>         目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(InputStream inputStream, Class<T> clazz, boolean format) {
        return toObj(inputStream, clazz, getObjectMapper(format));
    }

    /**
     * 从输入流读取 MessagePack 数据并反序列化为指定类型的对象
     *
     * @param inputStream  输入流
     * @param clazz        目标类型
     * @param objectMapper 自定义 ObjectMapper 实例
     * @param <T>          目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(InputStream inputStream, Class<T> clazz, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new DeserializationException(clazz, e);
        }
    }

    // ====================== 反序列化 (TypeReference) - byte[] ======================

    /**
     * 将 MessagePack 二进制数据反序列化为指定泛型类型的对象
     *
     * @param rawBytes      MessagePack 二进制数据
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, TypeReference<T> typeReference) {
        return toObj(rawBytes, typeReference, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定泛型类型的对象
     *
     * @param rawBytes      MessagePack 二进制数据
     * @param typeReference 目标泛型类型引用
     * @param format        是否使用格式化 ObjectMapper
     * @param <T>           目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, TypeReference<T> typeReference, boolean format) {
        return toObj(rawBytes, typeReference, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定泛型类型的对象
     *
     * @param rawBytes      MessagePack 二进制数据
     * @param typeReference 目标泛型类型引用
     * @param objectMapper  自定义 ObjectMapper 实例
     * @param <T>           目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, TypeReference<T> typeReference, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(rawBytes, typeReference);
        } catch (IOException e) {
            throw new DeserializationException(typeReference.getType(), e);
        }
    }

    // ====================== 反序列化 (TypeReference) - base64 ======================

    /**
     * 将 Base64 编码字符串反序列化为指定泛型类型的对象
     *
     * @param base64        Base64 编码字符串
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, TypeReference<T> typeReference) {
        return toObj(base64, typeReference, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为指定泛型类型的对象
     *
     * @param base64        Base64 编码字符串
     * @param typeReference 目标泛型类型引用
     * @param format        是否使用格式化 ObjectMapper
     * @param <T>           目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, TypeReference<T> typeReference, boolean format) {
        return toObj(base64, typeReference, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为指定泛型类型的对象
     *
     * @param base64        Base64 编码字符串
     * @param typeReference 目标泛型类型引用
     * @param objectMapper  自定义 ObjectMapper 实例
     * @param <T>           目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, TypeReference<T> typeReference, final ObjectMapper objectMapper) {
        return toObj(Base64.getDecoder().decode(base64), typeReference, objectMapper);
    }

    // ====================== 反序列化 (InputStream + Type) ======================

    /**
     * 从输入流读取 MessagePack 数据并反序列化为指定类型的对象
     *
     * @param inputStream 输入流
     * @param type        目标类型
     * @param <T>         目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(InputStream inputStream, Type type) {
        return toObj(inputStream, type, false);
    }

    /**
     * 从输入流读取 MessagePack 数据并反序列化为指定类型的对象
     *
     * @param inputStream 输入流
     * @param type        目标类型
     * @param format      是否使用格式化 ObjectMapper
     * @param <T>         目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(InputStream inputStream, Type type, boolean format) {
        return toObj(inputStream, type, getObjectMapper(format));
    }

    /**
     * 从输入流读取 MessagePack 数据并反序列化为指定类型的对象
     *
     * @param inputStream  输入流
     * @param type         目标类型
     * @param objectMapper 自定义 ObjectMapper 实例
     * @param <T>          目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(InputStream inputStream, Type type, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(inputStream, objectMapper.constructType(type));
        } catch (IOException e) {
            throw new DeserializationException(type, e);
        }
    }

    // ====================== toObjectNode - byte[] ======================

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link ObjectNode}
     *
     * @param rawBytes MessagePack 二进制数据
     * @return ObjectNode 实例
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static ObjectNode toObjectNode(byte[] rawBytes) {
        return toObjectNode(rawBytes, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link ObjectNode}
     *
     * @param rawBytes MessagePack 二进制数据
     * @param format   是否使用格式化 ObjectMapper
     * @return ObjectNode 实例
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static ObjectNode toObjectNode(byte[] rawBytes, boolean format) {
        return toObjectNode(rawBytes, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link ObjectNode}
     *
     * @param rawBytes     MessagePack 二进制数据
     * @param objectMapper 自定义 ObjectMapper 实例
     * @return ObjectNode 实例
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static ObjectNode toObjectNode(byte[] rawBytes, final ObjectMapper objectMapper) {
        return toObj(rawBytes, ObjectNode.class, objectMapper);
    }

    // ====================== toObjectNode - base64 ======================

    /**
     * 将 Base64 编码字符串反序列化为 {@link ObjectNode}
     *
     * @param base64 Base64 编码字符串
     * @return ObjectNode 实例
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static ObjectNode toObjectNode(String base64) {
        return toObjectNode(base64, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@link ObjectNode}
     *
     * @param base64 Base64 编码字符串
     * @param format 是否使用格式化 ObjectMapper
     * @return ObjectNode 实例
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static ObjectNode toObjectNode(String base64, boolean format) {
        return toObjectNode(base64, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@link ObjectNode}
     *
     * @param base64       Base64 编码字符串
     * @param objectMapper 自定义 ObjectMapper 实例
     * @return ObjectNode 实例
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static ObjectNode toObjectNode(String base64, final ObjectMapper objectMapper) {
        return toObjectNode(Base64.getDecoder().decode(base64), objectMapper);
    }

    // ====================== 反序列化 (参数化类型) - byte[] ======================

    /**
     * 将 MessagePack 二进制数据反序列化为参数化类型的对象
     *
     * @param rawBytes         MessagePack 二进制数据
     * @param parametrized     容器原始类型，如 {@code List.class}
     * @param parameterClasses 容器的泛型参数类型
     * @param <T>              目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(rawBytes, false, parametrized, parameterClasses);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为参数化类型的对象
     *
     * @param rawBytes         MessagePack 二进制数据
     * @param format           是否使用格式化 ObjectMapper
     * @param parametrized     容器原始类型，如 {@code List.class}
     * @param parameterClasses 容器的泛型参数类型
     * @param <T>              目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, boolean format, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(rawBytes, getObjectMapper(format), parametrized, parameterClasses);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为参数化类型的对象
     *
     * @param rawBytes         MessagePack 二进制数据
     * @param objectMapper     自定义 ObjectMapper 实例
     * @param parametrized     容器原始类型，如 {@code List.class}
     * @param parameterClasses 容器的泛型参数类型
     * @param <T>              目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(byte[] rawBytes, final ObjectMapper objectMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        try {
            return objectMapper.readValue(rawBytes, javaType);
        } catch (IOException e) {
            throw new DeserializationException(javaType, e);
        }
    }

    // ====================== 反序列化 (参数化类型) - base64 ======================

    /**
     * 将 Base64 编码字符串反序列化为参数化类型的对象
     *
     * @param base64           Base64 编码字符串
     * @param parametrized     容器原始类型，如 {@code List.class}
     * @param parameterClasses 容器的泛型参数类型
     * @param <T>              目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(base64, false, parametrized, parameterClasses);
    }

    /**
     * 将 Base64 编码字符串反序列化为参数化类型的对象
     *
     * @param base64           Base64 编码字符串
     * @param format           是否使用格式化 ObjectMapper
     * @param parametrized     容器原始类型，如 {@code List.class}
     * @param parameterClasses 容器的泛型参数类型
     * @param <T>              目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, boolean format, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(base64, getObjectMapper(format), parametrized, parameterClasses);
    }

    /**
     * 将 Base64 编码字符串反序列化为参数化类型的对象
     *
     * @param base64           Base64 编码字符串
     * @param objectMapper     自定义 ObjectMapper 实例
     * @param parametrized     容器原始类型，如 {@code List.class}
     * @param parameterClasses 容器的泛型参数类型
     * @param <T>              目标类型泛型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <T> T toObj(String base64, final ObjectMapper objectMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(Base64.getDecoder().decode(base64), objectMapper, parametrized, parameterClasses);
    }

    // ====================== toCollection - byte[] ======================

    /**
     * 将 MessagePack 二进制数据反序列化为指定集合类型
     *
     * @param rawBytes   MessagePack 二进制数据
     * @param collection 集合类型，如 {@code ArrayList.class}
     * @param element    集合元素类型
     * @param <C>        集合类型泛型
     * @param <E>        元素类型泛型
     * @return 反序列化后的集合
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <C extends Collection<E>, E> C toCollection(byte[] rawBytes, Class<C> collection, Class<E> element) {
        return toCollection(rawBytes, collection, element, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定集合类型
     *
     * @param rawBytes   MessagePack 二进制数据
     * @param collection 集合类型
     * @param element    集合元素类型
     * @param format     是否使用格式化 ObjectMapper
     * @param <C>        集合类型泛型
     * @param <E>        元素类型泛型
     * @return 反序列化后的集合
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <C extends Collection<E>, E> C toCollection(byte[] rawBytes, Class<C> collection, Class<E> element, boolean format) {
        return toCollection(rawBytes, collection, element, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定集合类型
     *
     * @param rawBytes     MessagePack 二进制数据
     * @param collection   集合类型
     * @param element      集合元素类型
     * @param objectMapper 自定义 ObjectMapper 实例
     * @param <C>          集合类型泛型
     * @param <E>          元素类型泛型
     * @return 反序列化后的集合
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <C extends Collection<E>, E> C toCollection(byte[] rawBytes, Class<C> collection, Class<E> element, final ObjectMapper objectMapper) {
        CollectionType type = objectMapper.getTypeFactory().constructCollectionType(collection, element);
        try {
            return objectMapper.readValue(rawBytes, type);
        } catch (Exception e) {
            throw new DeserializationException(type, e);
        }
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link ArrayList}
     *
     * @param rawBytes MessagePack 二进制数据
     * @param element  集合元素类型
     * @param <E>      元素类型泛型
     * @return 反序列化后的 ArrayList
     * @throws DeserializationException 反序列化失败时抛出
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(byte[] rawBytes, Class<E> element) {
        return toCollection(rawBytes, ArrayList.class, element, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link ArrayList}
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(byte[] rawBytes, Class<E> element, boolean format) {
        return toCollection(rawBytes, ArrayList.class, element, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link ArrayList}
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(byte[] rawBytes, Class<E> element, final ObjectMapper objectMapper) {
        return toCollection(rawBytes, ArrayList.class, element, objectMapper);
    }

    // ====================== toCollection - base64 ======================

    /**
     * 将 Base64 编码字符串反序列化为指定集合类型
     *
     * @param base64     Base64 编码字符串
     * @param collection 集合类型
     * @param element    集合元素类型
     * @param <C>        集合类型泛型
     * @param <E>        元素类型泛型
     * @return 反序列化后的集合
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <C extends Collection<E>, E> C toCollection(String base64, Class<C> collection, Class<E> element) {
        return toCollection(base64, collection, element, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为指定集合类型
     */
    public static <C extends Collection<E>, E> C toCollection(String base64, Class<C> collection, Class<E> element, boolean format) {
        return toCollection(base64, collection, element, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为指定集合类型
     */
    public static <C extends Collection<E>, E> C toCollection(String base64, Class<C> collection, Class<E> element, final ObjectMapper objectMapper) {
        return toCollection(Base64.getDecoder().decode(base64), collection, element, objectMapper);
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@link ArrayList}
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(String base64, Class<E> element) {
        return toCollection(base64, ArrayList.class, element, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@link ArrayList}
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(String base64, Class<E> element, boolean format) {
        return toCollection(base64, ArrayList.class, element, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@link ArrayList}
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(String base64, Class<E> element, final ObjectMapper objectMapper) {
        return toCollection(base64, ArrayList.class, element, objectMapper);
    }

    // ====================== toMap - byte[] ======================

    /**
     * 将 MessagePack 二进制数据反序列化为指定 Map 类型
     *
     * <pre>
     * HashMap&lt;String, String&gt; data = MessagePackUtils.toMap(rawBytes, HashMap.class, String.class, String.class);
     * </pre>
     *
     * @param rawBytes MessagePack 二进制数据
     * @param map      Map 类型，如 {@code HashMap.class}
     * @param key      键类型
     * @param value    值类型
     * @param <K>      键类型泛型
     * @param <V>      值类型泛型
     * @param <H>      Map 类型泛型
     * @return 反序列化后的 Map
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <K, V, H extends Map<K, V>> H toMap(byte[] rawBytes, Class<H> map, Class<K> key, Class<V> value) {
        return toMap(rawBytes, map, key, value, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定 Map 类型
     */
    public static <K, V, H extends Map<K, V>> H toMap(byte[] rawBytes, Class<H> map, Class<K> key, Class<V> value, boolean format) {
        return toMap(rawBytes, map, key, value, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为指定 Map 类型
     */
    public static <K, V, H extends Map<K, V>> H toMap(byte[] rawBytes, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        MapType type = objectMapper.getTypeFactory().constructMapType(map, key, value);
        try {
            return objectMapper.readValue(rawBytes, type);
        } catch (Exception e) {
            throw new DeserializationException(type, e);
        }
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link HashMap}
     *
     * @param rawBytes MessagePack 二进制数据
     * @param key      键类型
     * @param value    值类型
     * @param <K>      键类型泛型
     * @param <V>      值类型泛型
     * @return 反序列化后的 HashMap
     * @throws DeserializationException 反序列化失败时抛出
     */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(byte[] rawBytes, Class<K> key, Class<V> value) {
        return toMap(rawBytes, HashMap.class, key, value, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link HashMap}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(byte[] rawBytes, Class<K> key, Class<V> value, boolean format) {
        return toMap(rawBytes, HashMap.class, key, value, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@link HashMap}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(byte[] rawBytes, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toMap(rawBytes, HashMap.class, key, value, objectMapper);
    }

    // ====================== toMap - base64 ======================

    /**
     * 将 Base64 编码字符串反序列化为指定 Map 类型
     *
     * @param base64 Base64 编码字符串
     * @param map    Map 类型
     * @param key    键类型
     * @param value  值类型
     * @param <K>    键类型泛型
     * @param <V>    值类型泛型
     * @param <H>    Map 类型泛型
     * @return 反序列化后的 Map
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <K, V, H extends Map<K, V>> H toMap(String base64, Class<H> map, Class<K> key, Class<V> value) {
        return toMap(base64, map, key, value, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为指定 Map 类型
     */
    public static <K, V, H extends Map<K, V>> H toMap(String base64, Class<H> map, Class<K> key, Class<V> value, boolean format) {
        return toMap(base64, map, key, value, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为指定 Map 类型
     */
    public static <K, V, H extends Map<K, V>> H toMap(String base64, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toMap(Base64.getDecoder().decode(base64), map, key, value, objectMapper);
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@link HashMap}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(String base64, Class<K> key, Class<V> value) {
        return toMap(base64, HashMap.class, key, value, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@link HashMap}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(String base64, Class<K> key, Class<V> value, boolean format) {
        return toMap(base64, HashMap.class, key, value, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@link HashMap}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(String base64, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toMap(base64, HashMap.class, key, value, objectMapper);
    }

    // ====================== toCollectionMap - byte[] ======================

    /**
     * 将 MessagePack 二进制数据反序列化为 {@code Collection<Map>} 嵌套类型
     *
     * @param rawBytes   MessagePack 二进制数据
     * @param collection 集合类型
     * @param map        Map 类型
     * @param key        键类型
     * @param value      值类型
     * @param <K>        键类型泛型
     * @param <V>        值类型泛型
     * @param <H>        Map 类型泛型
     * @param <C>        集合类型泛型
     * @return 反序列化后的集合
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(byte[] rawBytes, Class<C> collection, Class<H> map, Class<K> key, Class<V> value) {
        return toCollectionMap(rawBytes, collection, map, key, value, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@code Collection<Map>} 嵌套类型
     */
    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(byte[] rawBytes, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, boolean format) {
        return toCollectionMap(rawBytes, collection, map, key, value, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@code Collection<Map>} 嵌套类型
     */
    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(byte[] rawBytes, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        MapType mapType = objectMapper.getTypeFactory().constructMapType(map, key, value);
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(collection, mapType);
        try {
            return objectMapper.readValue(rawBytes, collectionType);
        } catch (Exception e) {
            throw new DeserializationException(collectionType, e);
        }
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@code ArrayList<HashMap>}
     *
     * @param rawBytes MessagePack 二进制数据
     * @param key      键类型
     * @param value    值类型
     * @param <K>      键类型泛型
     * @param <V>      值类型泛型
     * @return 反序列化后的 ArrayList
     * @throws DeserializationException 反序列化失败时抛出
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(byte[] rawBytes, Class<K> key, Class<V> value) {
        return toCollectionMap(rawBytes, ArrayList.class, HashMap.class, key, value, false);
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@code ArrayList<HashMap>}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(byte[] rawBytes, Class<K> key, Class<V> value, boolean format) {
        return toCollectionMap(rawBytes, ArrayList.class, HashMap.class, key, value, getObjectMapper(format));
    }

    /**
     * 将 MessagePack 二进制数据反序列化为 {@code ArrayList<HashMap>}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(byte[] rawBytes, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toCollectionMap(rawBytes, ArrayList.class, HashMap.class, key, value, objectMapper);
    }

    // ====================== toCollectionMap - base64 ======================

    /**
     * 将 Base64 编码字符串反序列化为 {@code Collection<Map>} 嵌套类型
     *
     * @param base64     Base64 编码字符串
     * @param collection 集合类型
     * @param map        Map 类型
     * @param key        键类型
     * @param value      值类型
     * @param <K>        键类型泛型
     * @param <V>        值类型泛型
     * @param <H>        Map 类型泛型
     * @param <C>        集合类型泛型
     * @return 反序列化后的集合
     * @throws DeserializationException 反序列化失败时抛出
     */
    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(String base64, Class<C> collection, Class<H> map, Class<K> key, Class<V> value) {
        return toCollectionMap(base64, collection, map, key, value, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@code Collection<Map>} 嵌套类型
     */
    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(String base64, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, boolean format) {
        return toCollectionMap(base64, collection, map, key, value, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@code Collection<Map>} 嵌套类型
     */
    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(String base64, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toCollectionMap(Base64.getDecoder().decode(base64), collection, map, key, value, objectMapper);
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@code ArrayList<HashMap>}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(String base64, Class<K> key, Class<V> value) {
        return toCollectionMap(base64, ArrayList.class, HashMap.class, key, value, false);
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@code ArrayList<HashMap>}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(String base64, Class<K> key, Class<V> value, boolean format) {
        return toCollectionMap(base64, ArrayList.class, HashMap.class, key, value, getObjectMapper(format));
    }

    /**
     * 将 Base64 编码字符串反序列化为 {@code ArrayList<HashMap>}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(String base64, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toCollectionMap(base64, ArrayList.class, HashMap.class, key, value, objectMapper);
    }

    // ====================== 多态子类型注册 ======================

    /**
     * 注册多态子类型，用于基于类型名称的反序列化
     *
     * <p><b>线程安全警告：</b>此方法会修改共享的静态 {@link ObjectMapper} 实例，
     * 应仅在应用初始化阶段、并发访问开始之前调用。在多线程环境下调用可能导致不一致行为。</p>
     *
     * @param clazz 子类型 class
     * @param type  类型名称标识
     */
    public static void registerSubtype(Class<?> clazz, String type) {
        registerSubtype(clazz, type, false);
    }

    /**
     * 注册多态子类型
     *
     * @param clazz  子类型 class
     * @param type   类型名称标识
     * @param format 是否使用格式化 ObjectMapper
     */
    public static void registerSubtype(Class<?> clazz, String type, boolean format) {
        registerSubtype(clazz, type, getObjectMapper(format));
    }

    /**
     * 注册多态子类型
     *
     * @param clazz        子类型 class
     * @param type         类型名称标识
     * @param objectMapper 自定义 ObjectMapper 实例
     */
    public static void registerSubtype(Class<?> clazz, String type, final ObjectMapper objectMapper) {
        objectMapper.registerSubtypes(new NamedType(clazz, type));
    }

    // ====================== 节点工厂 ======================

    /**
     * 创建空的 {@link ObjectNode}
     *
     * @return 空的 ObjectNode 实例
     */
    public static ObjectNode createEmptyJsonNode() {
        return createEmptyJsonNode(false);
    }

    /**
     * 创建空的 {@link ObjectNode}
     *
     * @param format 是否使用格式化 ObjectMapper
     * @return 空的 ObjectNode 实例
     */
    public static ObjectNode createEmptyJsonNode(boolean format) {
        return createEmptyJsonNode(getObjectMapper(format));
    }

    /**
     * 创建空的 {@link ObjectNode}
     *
     * @param objectMapper 自定义 ObjectMapper 实例
     * @return 空的 ObjectNode 实例
     */
    public static ObjectNode createEmptyJsonNode(final ObjectMapper objectMapper) {
        return new ObjectNode(objectMapper.getNodeFactory());
    }

    /**
     * 创建空的 {@link ArrayNode}
     *
     * @return 空的 ArrayNode 实例
     */
    public static ArrayNode createEmptyArrayNode() {
        return createEmptyArrayNode(false);
    }

    /**
     * 创建空的 {@link ArrayNode}
     *
     * @param format 是否使用格式化 ObjectMapper
     * @return 空的 ArrayNode 实例
     */
    public static ArrayNode createEmptyArrayNode(boolean format) {
        return createEmptyArrayNode(getObjectMapper(format));
    }

    /**
     * 创建空的 {@link ArrayNode}
     *
     * @param objectMapper 自定义 ObjectMapper 实例
     * @return 空的 ArrayNode 实例
     */
    public static ArrayNode createEmptyArrayNode(final ObjectMapper objectMapper) {
        return new ArrayNode(objectMapper.getNodeFactory());
    }

    /**
     * 将对象转换为 {@link JsonNode} 树结构
     *
     * @param obj 待转换对象
     * @return JsonNode 树结构
     */
    public static JsonNode toJsonNode(Object obj) {
        return toJsonNode(obj, false);
    }

    /**
     * 将对象转换为 {@link JsonNode} 树结构
     *
     * @param obj    待转换对象
     * @param format 是否使用格式化 ObjectMapper
     * @return JsonNode 树结构
     */
    public static JsonNode toJsonNode(Object obj, boolean format) {
        return toJsonNode(obj, getObjectMapper(format));
    }

    /**
     * 将对象转换为 {@link JsonNode} 树结构
     *
     * @param obj          待转换对象
     * @param objectMapper 自定义 ObjectMapper 实例
     * @return JsonNode 树结构
     */
    public static JsonNode toJsonNode(Object obj, final ObjectMapper objectMapper) {
        return objectMapper.valueToTree(obj);
    }

    // ====================== 类型构造 ======================

    /**
     * 根据 {@link Type} 构造 {@link JavaType}
     *
     * @param type Java 类型
     * @return 对应的 JavaType 实例
     */
    public static JavaType constructJavaType(Type type) {
        return constructJavaType(type, false);
    }

    /**
     * 根据 {@link Type} 构造 {@link JavaType}
     *
     * @param type   Java 类型
     * @param format 是否使用格式化 ObjectMapper
     * @return 对应的 JavaType 实例
     */
    public static JavaType constructJavaType(Type type, boolean format) {
        return constructJavaType(type, getObjectMapper(format));
    }

    /**
     * 根据 {@link Type} 构造 {@link JavaType}
     *
     * @param type         Java 类型
     * @param objectMapper 自定义 ObjectMapper 实例
     * @return 对应的 JavaType 实例
     */
    public static JavaType constructJavaType(Type type, final ObjectMapper objectMapper) {
        return objectMapper.constructType(type);
    }
}