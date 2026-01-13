package io.ituknown.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.ituknown.datetime.DateFormatUtils;
import io.ituknown.jackson.serializer.BigDecimalAsStringJsonSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

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
        XmlMapper mapper = new XmlMapper();

        // 格式化输出
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, format);

        // XML 声明
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, includeDeclaration);

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
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }

    // endregion toXml
    // region toObj

    public static <T> T toObj(byte[] xml, Class<T> clazz) {
        return toObj(xml, clazz, false);
    }

    public static <T> T toObj(byte[] xml, Class<T> clazz, boolean includeDeclaration) {
        return toObj(xml, clazz, getXmlMapper(includeDeclaration));
    }

    public static <T> T toObj(byte[] xml, Class<T> clazz, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.readValue(xml, clazz);
        } catch (IOException e) {
            throw new DeserializationException(clazz, e);
        }
    }

    public static <T> T toObj(byte[] xml, Type type) {
        return toObj(xml, type, false);
    }

    public static <T> T toObj(byte[] xml, Type type, boolean includeDeclaration) {
        return toObj(xml, type, getXmlMapper(includeDeclaration));
    }

    public static <T> T toObj(byte[] xml, Type type, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.readValue(xml, xmlMapper.constructType(type));
        } catch (IOException e) {
            throw new DeserializationException(type, e);
        }
    }

    public static <T> T toObj(byte[] xml, TypeReference<T> typeReference) {
        return toObj(xml, typeReference, false);
    }

    public static <T> T toObj(byte[] xml, TypeReference<T> typeReference, boolean includeDeclaration) {
        return toObj(xml, typeReference, getXmlMapper(includeDeclaration));
    }

    public static <T> T toObj(byte[] xml, TypeReference<T> typeReference, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.readValue(xml, typeReference);
        } catch (IOException e) {
            throw new DeserializationException(typeReference.getClass(), e);
        }
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz) {
        return toObj(inputStream, clazz, false);
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz, boolean includeDeclaration) {
        return toObj(inputStream, clazz, getXmlMapper(includeDeclaration));
    }

    public static <T> T toObj(InputStream inputStream, Class<T> clazz, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new DeserializationException(e);
        }
    }

    public static <T> T toObj(String xml, Type type) {
        return toObj(xml, type, false);
    }

    public static <T> T toObj(String xml, Type type, boolean includeDeclaration) {
        return toObj(xml, type, getXmlMapper(includeDeclaration));
    }

    public static <T> T toObj(String xml, Type type, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.readValue(xml, xmlMapper.constructType(type));
        } catch (IOException e) {
            throw new DeserializationException(e);
        }
    }

    public static <T> T toObj(String xml, Class<T> clazz) {
        return toObj(xml, clazz, false);
    }

    public static <T> T toObj(String xml, Class<T> clazz, boolean includeDeclaration) {
        return toObj(xml, clazz, getXmlMapper(includeDeclaration));
    }

    public static <T> T toObj(String xml, Class<T> clazz, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.readValue(xml, clazz);
        } catch (IOException e) {
            throw new DeserializationException(clazz, e);
        }
    }

    public static <T> T toObj(String xml, TypeReference<T> typeReference) {
        return toObj(xml, typeReference, false);
    }

    public static <T> T toObj(String xml, TypeReference<T> typeReference, boolean includeDeclaration) {
        return toObj(xml, typeReference, getXmlMapper(includeDeclaration));
    }

    public static <T> T toObj(String xml, TypeReference<T> typeReference, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.readValue(xml, typeReference);
        } catch (IOException e) {
            throw new DeserializationException(typeReference.getClass(), e);
        }
    }

    public static <T> T toObj(InputStream inputStream, Type type) {
        return toObj(inputStream, type, false);
    }

    public static <T> T toObj(InputStream inputStream, Type type, boolean includeDeclaration) {
        return toObj(inputStream, type, getXmlMapper(includeDeclaration));
    }

    public static <T> T toObj(InputStream inputStream, Type type, final XmlMapper xmlMapper) {
        try {
            return xmlMapper.readValue(inputStream, xmlMapper.constructType(type));
        } catch (IOException e) {
            throw new DeserializationException(type, e);
        }
    }

    public static <T> T toObj(String xml, Class<T> parametrized, Class<?>... parameterClasses) {
        return toObj(xml, false, parametrized, parameterClasses);
    }

    /**
     * XML 字符串反序列化为泛型类对象
     * <p><pre>
     *     String xml = "...";
     *     List<User> userList = toObj(xml, false, List.class, User.class);
     *     List<User<Role> userList = toObj(xml, false, List.class, User.class, Role.class);
     * </pre></p>
     *
     * @param xml                XML字符串
     * @param general            泛型类型
     * @param parameterClasses   泛型参数
     * @param includeDeclaration 是否包含 xml 声明
     * @return T
     * @throws DeserializationException if deserialize failed
     */
    public static <T> T toObj(String xml, boolean includeDeclaration, Class<T> general, Class<?>... parameterClasses) {
        return toObj(xml, getXmlMapper(includeDeclaration), general, parameterClasses);
    }

    public static <T> T toObj(String xml, final XmlMapper xmlMapper, Class<T> general, Class<?>... parameterClasses) {
        try {
            JavaType javaType = xmlMapper.getTypeFactory().constructParametricType(general, parameterClasses);
            return xmlMapper.readValue(xml, javaType);
        } catch (IOException e) {
            throw new DeserializationException(e);
        }
    }

    // endregion toObj
}