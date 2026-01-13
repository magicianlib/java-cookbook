package io.ituknown.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import io.ituknown.datetime.DateFormatUtils;
import io.ituknown.jackson.serializer.BigDecimalAsStringJsonSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Jackson JSON 序列化/反序列化工具类
 *
 * @author magicianlib@gmail.com
 * @see com.fasterxml.jackson.annotation
 */
public enum JacksonUtils {
    ;

    private static final ObjectMapper MAPPER_WITH_FORMAT = createObjectMapper(true);
    private static final ObjectMapper MAPPER_WITHOUT_FORMAT = createObjectMapper(false);

    public static ObjectMapper getObjectMapper() {
        return getObjectMapper(false);
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @param format 是否开启JSON格式化
     */
    public static ObjectMapper getObjectMapper(boolean format) {
        return format ? MAPPER_WITH_FORMAT : MAPPER_WITHOUT_FORMAT;
    }

    /**
     * 创建 ObjectMapper 对象
     *
     * @param format 是否开启JSON格式化
     */
    public static ObjectMapper createObjectMapper(boolean format) {
        ObjectMapper mapper = new ObjectMapper();

        // 格式化输出
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, format);

        // 忽略未知字段
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 序列化时忽略空值
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 设置时区
        mapper.setTimeZone(TimeZone.getDefault());

        // 忽略 transient 字段
        mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);

        // java.util.Date 日期格式 处理
        mapper.setDateFormat(new SimpleDateFormat(DateFormatUtils.DATE_TIME_PATTERN));

        // java.time.* 日期格式处理
        JacksonConfig.configureObjectMapper4Jsr310(mapper);

        // Null 值处理
        JacksonConfig.configureNullObject(mapper);

        // BigDecimal 自定义序列化
        JacksonConfig.registerModule(mapper, BigDecimal.class, new BigDecimalAsStringJsonSerializer());

        return mapper;
    }


    // ====================== ObjectMapper ======================

    public static String toJson(Object obj) {
        return toJson(obj, false);
    }

    public static String toJson(Object obj, boolean format) {
        return toJson(obj, getObjectMapper(format));
    }

    public static String toJson(Object obj, final ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialize " + obj.getClass().getSimpleName() + " to json failed", e);
        }
    }

    public static byte[] toJsonBytes(Object obj) {
        return toJsonBytes(obj, false);
    }

    public static byte[] toJsonBytes(Object obj, boolean format) {
        return toJsonBytes(obj, getObjectMapper(format));
    }

    public static byte[] toJsonBytes(Object obj, final ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialize " + obj.getClass().getSimpleName() + " to json-bytes failed", e);
        }
    }

    public static <T> T toObj(byte[] json, Class<T> clazz) {
        return toObj(json, clazz, false);
    }

    public static <T> T toObj(byte[] json, Class<T> clazz, boolean format) {
        return toObj(json, clazz, getObjectMapper(format));
    }

    public static <T> T toObj(byte[] json, Class<T> clazz, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Parse json-bytes to " + clazz.getSimpleName() + " failed", e);
        }
    }

    public static <T> T toObj(byte[] json, Type type) {
        return toObj(json, type, false);
    }

    public static <T> T toObj(byte[] json, Type type, boolean format) {
        return toObj(json, type, getObjectMapper(format));
    }

    public static <T> T toObj(byte[] json, Type type, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, objectMapper.constructType(type));
        } catch (IOException e) {
            throw new RuntimeException("Parse json-bytes to " + type.getTypeName() + " failed", e);
        }
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz) {
        return toObj(inputStream, clazz, false);
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz, boolean format) {
        return toObj(inputStream, clazz, getObjectMapper(format));
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Parse inputStream to " + clazz.getSimpleName() + " failed", e);
        }
    }

    public static <T> T toObj(byte[] json, TypeReference<T> typeReference) {
        return toObj(json, typeReference, false);
    }

    public static <T> T toObj(byte[] json, TypeReference<T> typeReference, boolean format) {
        return toObj(json, typeReference, getObjectMapper(format));
    }

    public static <T> T toObj(byte[] json, TypeReference<T> typeReference, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("Parse json-bytes to " + typeReference.getType().getTypeName() + " failed", e);
        }
    }

    public static <T> T toObj(String json, Class<T> clazz) {
        return toObj(json, clazz, false);
    }

    public static <T> T toObj(String json, Class<T> clazz, boolean format) {
        return toObj(json, clazz, getObjectMapper(format));
    }

    public static <T> T toObj(String json, Class<T> clazz, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Parse json to " + clazz.getSimpleName() + " failed: " + json, e);
        }
    }

    public static <T> T toObj(String json, Type type) {
        return toObj(json, type, false);
    }

    public static <T> T toObj(String json, Type type, boolean format) {
        return toObj(json, type, getObjectMapper(format));
    }

    public static <T> T toObj(String json, Type type, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, objectMapper.constructType(type));
        } catch (IOException e) {
            throw new RuntimeException("Parse json to " + type.getTypeName() + " failed: " + json, e);
        }
    }

    public static <T> T toObj(String json, TypeReference<T> typeReference) {
        return toObj(json, typeReference, false);
    }

    public static <T> T toObj(String json, TypeReference<T> typeReference, boolean format) {
        return toObj(json, typeReference, getObjectMapper(format));
    }

    public static <T> T toObj(String json, TypeReference<T> typeReference, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("Parse json to " + typeReference.getType().getTypeName() + " failed: " + json, e);
        }
    }

    public static <T> T toObj(InputStream inputStream, Type type) {
        return toObj(inputStream, type, false);
    }

    public static <T> T toObj(InputStream inputStream, Type type, boolean format) {
        return toObj(inputStream, type, getObjectMapper(format));
    }

    public static <T> T toObj(InputStream inputStream, Type type, final ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(inputStream, objectMapper.constructType(type));
        } catch (IOException e) {
            throw new RuntimeException("Parse inputStream to " + type.getTypeName() + " failed", e);
        }
    }

    public static ObjectNode toObjectNode(String json) {
        return toObjectNode(json, false);
    }

    public static ObjectNode toObjectNode(String json, boolean format) {
        return toObjectNode(json, getObjectMapper(format));
    }

    public static ObjectNode toObjectNode(String json, final ObjectMapper objectMapper) {
        return toObj(json, ObjectNode.class, objectMapper);
    }

    public static <T> T toObj(String json, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(json, false, parametrized, parameterClasses);
    }

    public static <T> T toObj(String json, boolean format, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(json, getObjectMapper(format), parametrized, parameterClasses);
    }

    public static <T> T toObj(String json, final ObjectMapper objectMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        try {
            return objectMapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new RuntimeException("Parse json to " + javaType.getTypeName() + " failed: " + json, e);
        }
    }

    /**
     * 将 JSON 字符串转换为指定集合类型
     */
    public static <C extends Collection<E>, E> C toCollection(String json, Class<C> collection, Class<E> element) {
        return toCollection(json, collection, element, false);
    }

    public static <C extends Collection<E>, E> C toCollection(String json, Class<C> collection, Class<E> element, boolean format) {
        return toCollection(json, collection, element, getObjectMapper(format));
    }

    public static <C extends Collection<E>, E> C toCollection(String json, Class<C> collection, Class<E> element, final ObjectMapper objectMapper) {
        try {
            CollectionType type = objectMapper.getTypeFactory().constructCollectionType(collection, element);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Parse json to Collection<E> failed:" + json, e);
        }
    }


    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(String json, Class<E> element) {
        return toCollection(json, ArrayList.class, element, false);
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(String json, Class<E> element, boolean format) {
        return toCollection(json, ArrayList.class, element, getObjectMapper(format));
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(String json, Class<E> element, final ObjectMapper objectMapper) {
        return toCollection(json, ArrayList.class, element, objectMapper);
    }

    /**
     * 将 JSON 字符串转为 Map
     * <pre>
     * HashMap<String, String> data = toMap(jsonMap, HashMap.class, String.class, String.class);
     * </pre>
     *
     * @throws RuntimeException 如果解析失败
     */
    public static <K, V, H extends Map<K, V>> H toMap(String json, Class<H> map, Class<K> key, Class<V> value) {
        return toMap(json, map, key, value, false);
    }

    public static <K, V, H extends Map<K, V>> H toMap(String json, Class<H> map, Class<K> key, Class<V> value, boolean format) {
        return toMap(json, map, key, value, getObjectMapper(format));
    }

    public static <K, V, H extends Map<K, V>> H toMap(String json, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        try {
            MapType type = objectMapper.getTypeFactory().constructMapType(map, key, value);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Parse json to Map<K, V> failed:" + json, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(String json, Class<K> key, Class<V> value) {
        return toMap(json, HashMap.class, key, value, false);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(String json, Class<K> key, Class<V> value, boolean format) {
        return toMap(json, HashMap.class, key, value, getObjectMapper(format));
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(String json, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toMap(json, HashMap.class, key, value, objectMapper);
    }

    /**
     * 将 JSON 字符串转换为指定集合Map类型
     */
    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(String json, Class<C> collection, Class<H> map, Class<K> key, Class<V> value) {
        return toCollectionMap(json, collection, map, key, value, false);
    }

    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(String json, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, boolean format) {
        return toCollectionMap(json, collection, map, key, value, getObjectMapper(format));
    }

    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(String json, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        try {
            MapType mapType = objectMapper.getTypeFactory().constructMapType(map, key, value);
            CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(collection, mapType);
            return objectMapper.readValue(json, collectionType);
        } catch (Exception e) {
            throw new RuntimeException("Parse json to Collection<Map<K, V>>> failed:" + json, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(String json, Class<K> key, Class<V> value) {
        return toCollectionMap(json, ArrayList.class, HashMap.class, key, value, false);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(String json, Class<K> key, Class<V> value, boolean format) {
        return toCollectionMap(json, ArrayList.class, HashMap.class, key, value, getObjectMapper(format));
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(String json, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toCollectionMap(json, ArrayList.class, HashMap.class, key, value, objectMapper);
    }

    public static void registerSubtype(Class<?> clazz, String type) {
        registerSubtype(clazz, type, false);
    }

    public static void registerSubtype(Class<?> clazz, String type, boolean format) {
        registerSubtype(clazz, type, getObjectMapper(format));
    }

    public static void registerSubtype(Class<?> clazz, String type, final ObjectMapper objectMapper) {
        objectMapper.registerSubtypes(new NamedType(clazz, type));
    }

    public static ObjectNode createEmptyJsonNode() {
        return createEmptyJsonNode(false);
    }

    public static ObjectNode createEmptyJsonNode(boolean format) {
        return createEmptyJsonNode(getObjectMapper(format));
    }

    public static ObjectNode createEmptyJsonNode(final ObjectMapper objectMapper) {
        return new ObjectNode(objectMapper.getNodeFactory());
    }

    public static ArrayNode createEmptyArrayNode() {
        return createEmptyArrayNode(false);
    }

    public static ArrayNode createEmptyArrayNode(boolean format) {
        return createEmptyArrayNode(getObjectMapper(format));
    }

    public static ArrayNode createEmptyArrayNode(final ObjectMapper objectMapper) {
        return new ArrayNode(objectMapper.getNodeFactory());
    }

    public static JsonNode transferToJsonNode(Object obj) {
        return transferToJsonNode(obj, false);
    }

    public static JsonNode transferToJsonNode(Object obj, boolean format) {
        return transferToJsonNode(obj, getObjectMapper(format));
    }

    public static JsonNode transferToJsonNode(Object obj, final ObjectMapper objectMapper) {
        return objectMapper.valueToTree(obj);
    }

    public static JavaType constructJavaType(Type type) {
        return constructJavaType(type, false);
    }

    public static JavaType constructJavaType(Type type, boolean format) {
        return constructJavaType(type, getObjectMapper(format));
    }

    public static JavaType constructJavaType(Type type, final ObjectMapper objectMapper) {
        return objectMapper.constructType(type);
    }
}