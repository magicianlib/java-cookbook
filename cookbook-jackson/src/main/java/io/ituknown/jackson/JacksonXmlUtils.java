package io.ituknown.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import java.io.*;
import java.lang.reflect.Type;

import static io.ituknown.jackson.JacksonConfig.applyCommonBuilderConfig;
import static io.ituknown.jackson.JacksonConfig.configureObjectMapperForJsr310;
import static io.ituknown.jackson.JacksonDeserializeSupport.deserialize;

/**
 * Jackson XML 序列化/反序列化工具类
 *
 * @author magicianlib@gmail.com
 * @see com.fasterxml.jackson.dataformat.xml.annotation
 */
public enum JacksonXmlUtils {
    ;
    /**
     * 用于生成带 XML 声明的 XML
     */
    private static final XmlMapper XML_WITH_DECLARATION = createXmlMapper(true, false);
    /**
     * 用于生成不带 XML 声明的 XML
     */
    private static final XmlMapper XML_WITHOUT_DECLARATION = createXmlMapper(false, false);
    /**
     * 用于生成带 XML 声明的格式化 XML
     */
    private static final XmlMapper XML_FORMATTED_WITH_DECLARATION = createXmlMapper(true, true);
    /**
     * 用于生成不带 XML 声明的格式化 XML
     */
    private static final XmlMapper XML_FORMATTED_WITHOUT_DECLARATION = createXmlMapper(false, true);

    public static XmlMapper getXmlMapper() {
        return getXmlMapper(false);
    }

    /**
     * 获取XML实例，自己指定是否需要XML声明：
     * <p><pre>
     *     <?xml version="1.0" encoding="UTF-8"?>
     * </pre></p>
     */
    public static XmlMapper getXmlMapper(boolean includeDeclaration) {
        return includeDeclaration ? XML_WITH_DECLARATION : XML_WITHOUT_DECLARATION;
    }

    /**
     * 获取 XML 实例，指定是否需要 XML 声明和格式化输出
     *
     * @param includeDeclaration 是否包含 XML 声明
     * @param format             是否开启格式化输出
     * @return 配置好的 XmlMapper 实例
     */
    public static XmlMapper getXmlMapper(boolean includeDeclaration, boolean format) {
        if (format) {
            return includeDeclaration ? XML_FORMATTED_WITH_DECLARATION : XML_FORMATTED_WITHOUT_DECLARATION;
        }
        return includeDeclaration ? XML_WITH_DECLARATION : XML_WITHOUT_DECLARATION;
    }

    /**
     * 创建并配置 XmlMapper 实例
     *
     * <p>
     * 如果 {@code includeDeclaration} 为 true，在转XML时会在顶部增加XML声明：
     * <pre>
     * <?xml version="1.0" encoding="UTF-8"?>
     * </pre>
     * </p>
     *
     * @param includeDeclaration 是否包含 XML 声明
     * @param format             配置格式化输出
     * @return 配置好的 XmlMapper 实例
     */
    public static XmlMapper createXmlMapper(boolean includeDeclaration, boolean format) {
        XmlMapper.Builder builder = XmlMapper.builder();

        // XML 声明
        builder.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, includeDeclaration);

        // 通用 builder 配置
        applyCommonBuilderConfig(builder, format);

        XmlMapper mapper = builder.build();
        configureObjectMapperForJsr310(mapper);

        return mapper;
    }


    // ====================== XmlMapper ======================

    // region toXml

    /**
     * 对象转 XML 字符串
     *
     * @param obj obj
     * @return xml 字符串
     * @throws SerializationException if transfer failed
     */
    public static String toXml(Object obj) {
        return toXml(obj, getXmlMapper());
    }

    public static String toXml(Object obj, boolean includeDeclaration) {
        return toXml(obj, getXmlMapper(includeDeclaration));
    }

    public static String toXml(Object obj, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new SerializationException(obj.getClass(), e);
        }
    }

    public static byte[] toXmlBytes(Object obj) {
        return toXmlBytes(obj, false);
    }

    public static byte[] toXmlBytes(Object obj, boolean includeDeclaration) {
        return toXmlBytes(obj, getXmlMapper(includeDeclaration));
    }

    public static byte[] toXmlBytes(Object obj, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new SerializationException(obj.getClass(), e);
        }
    }

    public static void toXml(Writer w, Object obj) {
        toXml(w, obj, false);
    }

    public static void toXml(Writer w, Object obj, boolean includeDeclaration) {
        toXml(w, obj, getXmlMapper(includeDeclaration));
    }

    public static void toXml(Writer w, Object obj, final XmlMapper xmlMapper) {
        try {
            xmlMapper.writeValue(w, obj);
        } catch (IOException e) {
            throw new SerializationException(obj.getClass(), e);
        }
    }

    public static void toXml(OutputStream out, Object obj) {
        toXml(out, obj, false);
    }

    public static void toXml(OutputStream out, Object obj, boolean includeDeclaration) {
        toXml(out, obj, getXmlMapper(includeDeclaration));
    }

    public static void toXml(OutputStream out, Object obj, final XmlMapper xmlMapper) {
        try {
            xmlMapper.writeValue(out, obj);
        } catch (IOException e) {
            throw new SerializationException(obj.getClass(), e);
        }
    }

    // endregion toXml
    // region toObj

    public static <T> T toObj(byte[] xml, Class<T> clazz) {
        return toObj(xml, clazz, getXmlMapper());
    }

    public static <T> T toObj(byte[] xml, Class<T> clazz, final XmlMapper xmlMapper) {
        return deserialize(clazz, () -> xmlMapper.readValue(xml, clazz));
    }

    public static <T> T toObj(byte[] xml, Type type) {
        return toObj(xml, type, getXmlMapper());
    }

    public static <T> T toObj(byte[] xml, Type type, final XmlMapper xmlMapper) {
        return deserialize(type, () -> xmlMapper.readValue(xml, xmlMapper.constructType(type)));
    }

    public static <T> T toObj(byte[] xml, TypeReference<T> typeReference) {
        return toObj(xml, typeReference, getXmlMapper());
    }

    public static <T> T toObj(byte[] xml, TypeReference<T> typeReference, final XmlMapper xmlMapper) {
        return deserialize(typeReference.getType(), () -> xmlMapper.readValue(xml, typeReference));
    }

    public static <T> T toObj(String xml, Class<T> clazz) {
        return toObj(xml, clazz, getXmlMapper());
    }

    public static <T> T toObj(String xml, Class<T> clazz, final XmlMapper xmlMapper) {
        return deserialize(clazz, () -> xmlMapper.readValue(xml, clazz));
    }

    public static <T> T toObj(String xml, Type type) {
        return toObj(xml, type, getXmlMapper());
    }

    public static <T> T toObj(String xml, Type type, final XmlMapper xmlMapper) {
        return deserialize(type, () -> xmlMapper.readValue(xml, xmlMapper.constructType(type)));
    }

    public static <T> T toObj(String xml, TypeReference<T> typeReference) {
        return toObj(xml, typeReference, getXmlMapper());
    }

    public static <T> T toObj(String xml, TypeReference<T> typeReference, final XmlMapper xmlMapper) {
        return deserialize(typeReference.getType(), () -> xmlMapper.readValue(xml, typeReference));
    }

    public static <T> T toObj(String xml, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(xml, getXmlMapper(), parametrized, parameterClasses);
    }

    public static <T> T toObj(String xml, final XmlMapper xmlMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        JavaType javaType = xmlMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        return deserialize(javaType, () -> xmlMapper.readValue(xml, javaType));
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz) {
        return toObj(inputStream, clazz, getXmlMapper());
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz, final XmlMapper xmlMapper) {
        return deserialize(clazz, () -> xmlMapper.readValue(inputStream, clazz));
    }

    public static <T> T toObj(InputStream inputStream, Type type) {
        return toObj(inputStream, type, getXmlMapper());
    }

    public static <T> T toObj(InputStream inputStream, Type type, final XmlMapper xmlMapper) {
        return deserialize(type, () -> xmlMapper.readValue(inputStream, xmlMapper.constructType(type)));
    }

    public static <T> T toObj(InputStream inputStream, TypeReference<T> typeReference) {
        return toObj(inputStream, typeReference, getXmlMapper());
    }

    public static <T> T toObj(InputStream inputStream, TypeReference<T> typeReference, final XmlMapper xmlMapper) {
        return deserialize(typeReference.getType(), () -> xmlMapper.readValue(inputStream, typeReference));
    }

    public static <T> T toObj(InputStream inputStream, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(inputStream, getXmlMapper(), parametrized, parameterClasses);
    }

    public static <T> T toObj(InputStream inputStream, final XmlMapper xmlMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        JavaType javaType = xmlMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        return deserialize(javaType, () -> xmlMapper.readValue(inputStream, javaType));
    }

    public static <T> T toObj(Reader reader, Class<T> clazz) {
        return toObj(reader, clazz, getXmlMapper());
    }

    public static <T> T toObj(Reader reader, Class<T> clazz, final XmlMapper xmlMapper) {
        return deserialize(clazz, () -> xmlMapper.readValue(reader, clazz));
    }

    public static <T> T toObj(Reader reader, Type type) {
        return toObj(reader, type, getXmlMapper());
    }

    public static <T> T toObj(Reader reader, Type type, final XmlMapper xmlMapper) {
        return deserialize(type, () -> xmlMapper.readValue(reader, xmlMapper.constructType(type)));
    }

    public static <T> T toObj(Reader reader, TypeReference<T> typeReference) {
        return toObj(reader, typeReference, getXmlMapper());
    }

    public static <T> T toObj(Reader reader, TypeReference<T> typeReference, final XmlMapper xmlMapper) {
        return deserialize(typeReference.getType(), () -> xmlMapper.readValue(reader, typeReference));
    }

    public static <T> T toObj(Reader reader, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(reader, getXmlMapper(), parametrized, parameterClasses);
    }

    public static <T> T toObj(Reader reader, final XmlMapper xmlMapper, Class<T> parametrized, Class<?>... parameterClasses) {
        JavaType javaType = xmlMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        return deserialize(javaType, () -> xmlMapper.readValue(reader, javaType));
    }

    // endregion toObj
}