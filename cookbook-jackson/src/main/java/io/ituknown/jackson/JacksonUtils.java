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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static io.ituknown.jackson.JacksonConfig.applyCommonBuilderConfig;
import static io.ituknown.jackson.JacksonConfig.configureObjectMapperForJsr310;

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
        JsonMapper.Builder builder = JsonMapper.builder();

        // 通用 builder 配置
        applyCommonBuilderConfig(builder, format);

        ObjectMapper mapper = builder.build();
        configureObjectMapperForJsr310(mapper);

        return mapper;
    }

    /**
     * 执行反序列化读取，统一将底层读取异常包装为反序列化异常
     */
    @FunctionalInterface
    private interface Deserializer<T> {
        T read() throws IOException;
    }

    private static <T> T deserialize(Type targetType, Deserializer<T> deserializer) {
        try {
            return deserializer.read();
        } catch (IOException e) {
            throw new DeserializationException(targetType, e);
        }
    }

    // ====================== toJson ======================

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
            throw new SerializationException(obj.getClass(), e);
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
            throw new SerializationException(obj.getClass(), e);
        }
    }

    // ====================== toObj ======================

    public static <T> T toObj(byte[] json, Class<T> clazz) {
        return toObj(json, clazz, getObjectMapper());
    }

    public static <T> T toObj(byte[] json, Class<T> clazz, final ObjectMapper objectMapper) {
        return deserialize(clazz, () -> objectMapper.readValue(json, clazz));
    }

    public static <T> T toObj(byte[] json, Type type) {
        return toObj(json, type, getObjectMapper());
    }

    public static <T> T toObj(byte[] json, Type type, final ObjectMapper objectMapper) {
        return deserialize(type, () -> objectMapper.readValue(json, objectMapper.constructType(type)));
    }

    public static <T> T toObj(byte[] json, TypeReference<T> typeReference) {
        return toObj(json, typeReference, getObjectMapper());
    }

    public static <T> T toObj(byte[] json, TypeReference<T> typeReference, final ObjectMapper objectMapper) {
        return deserialize(typeReference.getType(), () -> objectMapper.readValue(json, typeReference));
    }

    public static <T> T toObj(String json, Class<T> clazz) {
        return toObj(json, clazz, getObjectMapper());
    }

    public static <T> T toObj(String json, Class<T> clazz, final ObjectMapper objectMapper) {
        return deserialize(clazz, () -> objectMapper.readValue(json, clazz));
    }

    public static <T> T toObj(String json, Type type) {
        return toObj(json, type, getObjectMapper());
    }

    public static <T> T toObj(String json, Type type, final ObjectMapper objectMapper) {
        return deserialize(type, () -> objectMapper.readValue(json, objectMapper.constructType(type)));
    }

    public static <T> T toObj(String json, TypeReference<T> typeReference) {
        return toObj(json, typeReference, getObjectMapper());
    }

    public static <T> T toObj(String json, TypeReference<T> typeReference, final ObjectMapper objectMapper) {
        return deserialize(typeReference.getType(), () -> objectMapper.readValue(json, typeReference));
    }

    public static <T> T toObj(String json, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(json, getObjectMapper(), parametrized, parameterClasses);
    }

    public static <T> T toObj(String json, final ObjectMapper objectMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        return deserialize(javaType, () -> objectMapper.readValue(json, javaType));
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz) {
        return toObj(inputStream, clazz, getObjectMapper());
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz, final ObjectMapper objectMapper) {
        return deserialize(clazz, () -> objectMapper.readValue(inputStream, clazz));
    }

    public static <T> T toObj(InputStream inputStream, Type type) {
        return toObj(inputStream, type, getObjectMapper());
    }

    public static <T> T toObj(InputStream inputStream, Type type, final ObjectMapper objectMapper) {
        return deserialize(type, () -> objectMapper.readValue(inputStream, objectMapper.constructType(type)));
    }

    public static <T> T toObj(InputStream inputStream, TypeReference<T> typeReference) {
        return toObj(inputStream, typeReference, getObjectMapper());
    }

    public static <T> T toObj(InputStream inputStream, TypeReference<T> typeReference, final ObjectMapper objectMapper) {
        return deserialize(typeReference.getType(), () -> objectMapper.readValue(inputStream, typeReference));
    }

    public static <T> T toObj(InputStream inputStream, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(inputStream, getObjectMapper(), parametrized, parameterClasses);
    }

    public static <T> T toObj(InputStream inputStream, final ObjectMapper objectMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        return deserialize(javaType, () -> objectMapper.readValue(inputStream, javaType));
    }

    public static <T> T toObj(Reader reader, Class<T> clazz) {
        return toObj(reader, clazz, getObjectMapper());
    }

    public static <T> T toObj(Reader reader, Class<T> clazz, final ObjectMapper objectMapper) {
        return deserialize(clazz, () -> objectMapper.readValue(reader, clazz));
    }

    public static <T> T toObj(Reader reader, Type type) {
        return toObj(reader, type, getObjectMapper());
    }

    public static <T> T toObj(Reader reader, Type type, final ObjectMapper objectMapper) {
        return deserialize(type, () -> objectMapper.readValue(reader, objectMapper.constructType(type)));
    }

    public static <T> T toObj(Reader reader, TypeReference<T> typeReference) {
        return toObj(reader, typeReference, getObjectMapper());
    }

    public static <T> T toObj(Reader reader, TypeReference<T> typeReference, final ObjectMapper objectMapper) {
        return deserialize(typeReference.getType(), () -> objectMapper.readValue(reader, typeReference));
    }

    public static <T> T toObj(Reader reader, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(reader, getObjectMapper(), parametrized, parameterClasses);
    }

    public static <T> T toObj(Reader reader, final ObjectMapper objectMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        return deserialize(javaType, () -> objectMapper.readValue(reader, javaType));
    }

    // ====================== toObjectNode ======================

    public static ObjectNode toObjectNode(String json) {
        return toObjectNode(json, getObjectMapper());
    }

    public static ObjectNode toObjectNode(String json, final ObjectMapper objectMapper) {
        return toObj(json, ObjectNode.class, objectMapper);
    }

    public static ObjectNode toObjectNode(InputStream inputStream) {
        return toObjectNode(inputStream, getObjectMapper());
    }

    public static ObjectNode toObjectNode(InputStream inputStream, final ObjectMapper objectMapper) {
        return toObj(inputStream, ObjectNode.class, objectMapper);
    }

    public static ObjectNode toObjectNode(Reader reader) {
        return toObjectNode(reader, getObjectMapper());
    }

    public static ObjectNode toObjectNode(Reader reader, final ObjectMapper objectMapper) {
        return toObj(reader, ObjectNode.class, objectMapper);
    }

    // ====================== toCollection ======================

    /**
     * 将 JSON 字符串转换为指定集合类型
     */
    public static <C extends Collection<E>, E> C toCollection(String json, Class<C> collection, Class<E> element) {
        return toCollection(json, collection, element, getObjectMapper());
    }

    public static <C extends Collection<E>, E> C toCollection(String json, Class<C> collection, Class<E> element, final ObjectMapper objectMapper) {
        CollectionType type = objectMapper.getTypeFactory().constructCollectionType(collection, element);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new DeserializationException(type, e);
        }
    }

    public static <C extends Collection<E>, E> C toCollection(InputStream inputStream, Class<C> collection, Class<E> element) {
        return toCollection(inputStream, collection, element, getObjectMapper());
    }

    public static <C extends Collection<E>, E> C toCollection(InputStream inputStream, Class<C> collection, Class<E> element, final ObjectMapper objectMapper) {
        CollectionType type = objectMapper.getTypeFactory().constructCollectionType(collection, element);
        try {
            return objectMapper.readValue(inputStream, type);
        } catch (Exception e) {
            throw new DeserializationException(type, e);
        }
    }

    public static <C extends Collection<E>, E> C toCollection(Reader reader, Class<C> collection, Class<E> element) {
        return toCollection(reader, collection, element, getObjectMapper());
    }

    public static <C extends Collection<E>, E> C toCollection(Reader reader, Class<C> collection, Class<E> element, final ObjectMapper objectMapper) {
        CollectionType type = objectMapper.getTypeFactory().constructCollectionType(collection, element);
        try {
            return objectMapper.readValue(reader, type);
        } catch (Exception e) {
            throw new DeserializationException(type, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(String json, Class<E> element) {
        return toCollection(json, ArrayList.class, element, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(String json, Class<E> element, final ObjectMapper objectMapper) {
        return toCollection(json, ArrayList.class, element, objectMapper);
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(InputStream inputStream, Class<E> element) {
        return toCollection(inputStream, ArrayList.class, element, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(InputStream inputStream, Class<E> element, final ObjectMapper objectMapper) {
        return toCollection(inputStream, ArrayList.class, element, objectMapper);
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(Reader reader, Class<E> element) {
        return toCollection(reader, ArrayList.class, element, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> toCollection(Reader reader, Class<E> element, final ObjectMapper objectMapper) {
        return toCollection(reader, ArrayList.class, element, objectMapper);
    }

    // ====================== toMap ======================

    /**
     * 将 JSON 字符串转为 Map
     * <pre>
     * HashMap<String, String> data = toMap(jsonMap, HashMap.class, String.class, String.class);
     * </pre>
     *
     * @throws DeserializationException 如果解析失败
     */
    public static <K, V, H extends Map<K, V>> H toMap(String json, Class<H> map, Class<K> key, Class<V> value) {
        return toMap(json, map, key, value, getObjectMapper());
    }

    public static <K, V, H extends Map<K, V>> H toMap(String json, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        MapType type = objectMapper.getTypeFactory().constructMapType(map, key, value);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new DeserializationException(type, e);
        }
    }

    public static <K, V, H extends Map<K, V>> H toMap(InputStream inputStream, Class<H> map, Class<K> key, Class<V> value) {
        return toMap(inputStream, map, key, value, getObjectMapper());
    }

    public static <K, V, H extends Map<K, V>> H toMap(InputStream inputStream, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        MapType type = objectMapper.getTypeFactory().constructMapType(map, key, value);
        try {
            return objectMapper.readValue(inputStream, type);
        } catch (Exception e) {
            throw new DeserializationException(type, e);
        }
    }

    public static <K, V, H extends Map<K, V>> H toMap(Reader reader, Class<H> map, Class<K> key, Class<V> value) {
        return toMap(reader, map, key, value, getObjectMapper());
    }

    public static <K, V, H extends Map<K, V>> H toMap(Reader reader, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        MapType type = objectMapper.getTypeFactory().constructMapType(map, key, value);
        try {
            return objectMapper.readValue(reader, type);
        } catch (Exception e) {
            throw new DeserializationException(type, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(String json, Class<K> key, Class<V> value) {
        return toMap(json, HashMap.class, key, value, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(String json, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toMap(json, HashMap.class, key, value, objectMapper);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(InputStream inputStream, Class<K> key, Class<V> value) {
        return toMap(inputStream, HashMap.class, key, value, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(InputStream inputStream, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toMap(inputStream, HashMap.class, key, value, objectMapper);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(Reader reader, Class<K> key, Class<V> value) {
        return toMap(reader, HashMap.class, key, value, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> toMap(Reader reader, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toMap(reader, HashMap.class, key, value, objectMapper);
    }

    // ====================== toCollectionMap ======================

    /**
     * 将 JSON 字符串转换为指定集合Map类型
     */
    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(String json, Class<C> collection, Class<H> map, Class<K> key, Class<V> value) {
        return toCollectionMap(json, collection, map, key, value, getObjectMapper());
    }

    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(String json, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        MapType mapType = objectMapper.getTypeFactory().constructMapType(map, key, value);
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(collection, mapType);
        try {
            return objectMapper.readValue(json, collectionType);
        } catch (Exception e) {
            throw new DeserializationException(collectionType, e);
        }
    }

    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(InputStream inputStream, Class<C> collection, Class<H> map, Class<K> key, Class<V> value) {
        return toCollectionMap(inputStream, collection, map, key, value, getObjectMapper());
    }

    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(InputStream inputStream, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        MapType mapType = objectMapper.getTypeFactory().constructMapType(map, key, value);
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(collection, mapType);
        try {
            return objectMapper.readValue(inputStream, collectionType);
        } catch (Exception e) {
            throw new DeserializationException(collectionType, e);
        }
    }

    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(Reader reader, Class<C> collection, Class<H> map, Class<K> key, Class<V> value) {
        return toCollectionMap(reader, collection, map, key, value, getObjectMapper());
    }

    public static <K, V, H extends Map<K, V>, C extends Collection<H>> C toCollectionMap(Reader reader, Class<C> collection, Class<H> map, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        MapType mapType = objectMapper.getTypeFactory().constructMapType(map, key, value);
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(collection, mapType);
        try {
            return objectMapper.readValue(reader, collectionType);
        } catch (Exception e) {
            throw new DeserializationException(collectionType, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(String json, Class<K> key, Class<V> value) {
        return toCollectionMap(json, ArrayList.class, HashMap.class, key, value, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(String json, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toCollectionMap(json, ArrayList.class, HashMap.class, key, value, objectMapper);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(InputStream inputStream, Class<K> key, Class<V> value) {
        return toCollectionMap(inputStream, ArrayList.class, HashMap.class, key, value, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(InputStream inputStream, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toCollectionMap(inputStream, ArrayList.class, HashMap.class, key, value, objectMapper);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(Reader reader, Class<K> key, Class<V> value) {
        return toCollectionMap(reader, ArrayList.class, HashMap.class, key, value, getObjectMapper());
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<HashMap<K, V>> toCollectionMap(Reader reader, Class<K> key, Class<V> value, final ObjectMapper objectMapper) {
        return toCollectionMap(reader, ArrayList.class, HashMap.class, key, value, objectMapper);
    }

    // ====================== 其他 ======================

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

    public static void registerSubtype(Class<?> clazz, String type, boolean format) {
        registerSubtype(clazz, type, getObjectMapper(format));
    }

    public static void registerSubtype(Class<?> clazz, String type, final ObjectMapper objectMapper) {
        objectMapper.registerSubtypes(new NamedType(clazz, type));
    }

    public static ObjectNode createEmptyJsonNode() {
        return createEmptyJsonNode(getObjectMapper());
    }

    public static ObjectNode createEmptyJsonNode(final ObjectMapper objectMapper) {
        return new ObjectNode(objectMapper.getNodeFactory());
    }

    public static ArrayNode createEmptyArrayNode() {
        return createEmptyArrayNode(getObjectMapper());
    }

    public static ArrayNode createEmptyArrayNode(final ObjectMapper objectMapper) {
        return new ArrayNode(objectMapper.getNodeFactory());
    }

    public static JsonNode toJsonNode(Object obj) {
        return toJsonNode(obj, getObjectMapper());
    }

    public static JsonNode toJsonNode(Object obj, final ObjectMapper objectMapper) {
        return objectMapper.valueToTree(obj);
    }

    public static JavaType constructJavaType(Type type) {
        return constructJavaType(type, getObjectMapper());
    }

    public static JavaType constructJavaType(Type type, final ObjectMapper objectMapper) {
        return objectMapper.constructType(type);
    }
}
