package io.ituknown.jackson;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class JacksonXmlUtilsTest {

    @Setter
    @Getter
    @JacksonXmlRootElement(localName = "user")
    static class User {
        private String name;
        private int age;
        private BigDecimal amount;

        User() {}

        User(String name, int age, BigDecimal amount) {
            this.name = name;
            this.age = age;
            this.amount = amount;
        }

    }

    // ========== XmlMapper ==========

    @Test
    void getXmlMapper_default() {
        assertNotNull(JacksonXmlUtils.getXmlMapper());
    }

    @Test
    void getXmlMapper_withDeclaration_returnsDifferentInstance() {
        assertNotSame(
                JacksonXmlUtils.getXmlMapper(true),
                JacksonXmlUtils.getXmlMapper(false)
        );
    }

    @Test
    void createXmlMapper_createsNewInstance() {
        assertNotSame(
                JacksonXmlUtils.createXmlMapper(false, false),
                JacksonXmlUtils.createXmlMapper(false, false)
        );
    }

    // ========== toXml ==========

    @Test
    void toXml_basicObject() {
        User user = new User("test", 20, new BigDecimal("99.99"));
        String xml = JacksonXmlUtils.toXml(user);
        assertTrue(xml.contains("<name>test</name>"));
        assertTrue(xml.contains("<age>20</age>"));
        assertTrue(xml.contains("<amount>99.99</amount>"));
    }

    @Test
    void toXml_withoutDeclaration() {
        User user = new User("test", 20, new BigDecimal("1.00"));
        String xml = JacksonXmlUtils.toXml(user, false);
        assertFalse(xml.startsWith("<?xml"));
    }

    @Test
    void toXml_withDeclaration() {
        User user = new User("test", 20, new BigDecimal("1.00"));
        String xml = JacksonXmlUtils.toXml(user, true);
        assertTrue(xml.startsWith("<?xml"));
    }

    @Test
    void toXmlBytes_basicObject() {
        User user = new User("test", 20, new BigDecimal("1.00"));
        byte[] bytes = JacksonXmlUtils.toXmlBytes(user);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void toXml_withCustomMapper() {
        XmlMapper mapper = JacksonXmlUtils.createXmlMapper(false, false);
        User user = new User("test", 20, new BigDecimal("1.00"));
        String xml = JacksonUtils.toJson(user);
        assertNotNull(xml);
    }

    @Test
    void toXml_throwsSerializationException_onFailure() {
        assertThrows(SerializationException.class, () -> JacksonXmlUtils.toXml(new Object()));
    }

    // ========== getXmlMapper with format ==========

    @Test
    void getXmlMapper_withFormat_returnsDifferentInstance() {
        XmlMapper formatted = JacksonXmlUtils.getXmlMapper(false, true);
        XmlMapper unformatted = JacksonXmlUtils.getXmlMapper(false, false);
        assertNotSame(formatted, unformatted);
    }

    @Test
    void getXmlMapper_withFormat_producesIndentedXml() {
        User user = new User("test", 20, new BigDecimal("1.00"));
        String formattedXml = JacksonXmlUtils.toXml(user, JacksonXmlUtils.getXmlMapper(true, true));
        String unformattedXml = JacksonXmlUtils.toXml(user, JacksonXmlUtils.getXmlMapper(true, false));

        assertTrue(formattedXml.contains("\n"));
        assertTrue(formattedXml.startsWith("<?xml"));
        assertFalse(unformattedXml.contains("\n"));
    }

    // ========== toObj ==========

    @Test
    void toObj_fromString_byClass() {
        User user = new User("test", 20, new BigDecimal("99.99"));
        String xml = JacksonXmlUtils.toXml(user);
        User result = JacksonXmlUtils.toObj(xml, User.class);
        assertEquals("test", result.getName());
        assertEquals(20, result.getAge());
        assertEquals(new BigDecimal("99.99"), result.getAmount());
    }

    @Test
    void toObj_fromBytes_byClass() {
        User user = new User("test", 20, new BigDecimal("1.00"));
        byte[] xml = JacksonXmlUtils.toXmlBytes(user);
        User result = JacksonXmlUtils.toObj(xml, User.class);
        assertEquals("test", result.getName());
        assertEquals(20, result.getAge());
    }

    @Test
    void toObj_fromInputStream_byClass() throws IOException {
        User user = new User("test", 20, new BigDecimal("1.00"));
        byte[] xml = JacksonXmlUtils.toXmlBytes(user);
        try (ByteArrayInputStream is = new ByteArrayInputStream(xml)) {
            User result = JacksonXmlUtils.toObj(is, User.class);
            assertEquals("test", result.getName());
            assertEquals(20, result.getAge());
        }
    }

    @Test
    void toObj_fromReader_byClass() throws IOException {
        User user = new User("test", 20, new BigDecimal("1.00"));
        String xml = JacksonXmlUtils.toXml(user);
        try (StringReader reader = new StringReader(xml)) {
            User result = JacksonXmlUtils.toObj(reader, User.class);
            assertEquals("test", result.getName());
            assertEquals(20, result.getAge());
        }
    }

    @Test
    void toObj_throwsDeserializationException_onInvalidXml() {
        String invalidXml = "not xml";
        assertThrows(DeserializationException.class, () -> JacksonXmlUtils.toObj(invalidXml, User.class));
    }
}