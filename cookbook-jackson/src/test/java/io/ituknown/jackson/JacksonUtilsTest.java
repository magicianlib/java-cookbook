package io.ituknown.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JacksonUtilsTest {

    @Setter
    @Getter
    static class Sample {
        private String name;
        private int age;
        private BigDecimal amount;

        Sample() {}

        Sample(String name, int age, BigDecimal amount) {
            this.name = name;
            this.age = age;
            this.amount = amount;
        }

    }

    static class DateSample {
        private LocalDateTime dateTime;
        private LocalDate date;
        private LocalTime time;
        private OffsetDateTime offsetDateTime;

        DateSample() {}

        DateSample(LocalDateTime dateTime, LocalDate date, LocalTime time, OffsetDateTime offsetDateTime) {
            this.dateTime = dateTime;
            this.date = date;
            this.time = time;
            this.offsetDateTime = offsetDateTime;
        }

        public LocalDateTime getDateTime() { return dateTime; }
        public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public LocalTime getTime() { return time; }
        public void setTime(LocalTime time) { this.time = time; }
        public OffsetDateTime getOffsetDateTime() { return offsetDateTime; }
        public void setOffsetDateTime(OffsetDateTime offsetDateTime) { this.offsetDateTime = offsetDateTime; }
    }

    // ========== ObjectMapper ==========

    @Test
    void getObjectMapper_default() {
        ObjectMapper mapper = JacksonUtils.getObjectMapper();
        assertNotNull(mapper);
    }

    @Test
    void getObjectMapper_format() {
        ObjectMapper formatMapper = JacksonUtils.getObjectMapper(true);
        ObjectMapper noFormatMapper = JacksonUtils.getObjectMapper(false);
        assertNotSame(formatMapper, noFormatMapper);
    }

    @Test
    void createObjectMapper_createsNewInstance() {
        ObjectMapper m1 = JacksonUtils.createObjectMapper(false);
        ObjectMapper m2 = JacksonUtils.createObjectMapper(false);
        assertNotSame(m1, m2);
    }

    // ========== Serialization ==========

    @Test
    void toJson_basicObject() {
        Sample sample = new Sample("test", 20, new BigDecimal("99.99"));
        String json = JacksonUtils.toJson(sample);
        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"age\":20"));
        assertTrue(json.contains("\"amount\":99.99"));
    }

    @Test
    void toJson_withFormat() {
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        String json = JacksonUtils.toJson(sample, true);
        assertTrue(json.contains("\n"));
    }

    @Test
    void toJson_withCustomMapper() {
        ObjectMapper mapper = JacksonUtils.createObjectMapper(false);
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        String json = JacksonUtils.toJson(sample, mapper);
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"test\""));
    }

    @Test
    void toJsonBytes_basicObject() {
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        byte[] bytes = JacksonUtils.toJsonBytes(sample);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void toJson_throwsSerializationException_onFailure() {
        Object obj = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("test error");
            }
        };
        assertThrows(SerializationException.class, () -> JacksonUtils.toJson(obj));
    }

    // ========== Deserialization ==========

    @Test
    void toObj_fromString_byClass() {
        String json = "{\"name\":\"test\",\"age\":20,\"amount\":\"1.00\"}";
        Sample result = JacksonUtils.toObj(json, Sample.class);
        assertEquals("test", result.getName());
        assertEquals(20, result.getAge());
        assertEquals(new BigDecimal("1.00"), result.getAmount());
    }

    @Test
    void toObj_fromBytes_byClass() {
        byte[] json = "{\"name\":\"test\",\"age\":20,\"amount\":\"1.00\"}".getBytes();
        Sample result = JacksonUtils.toObj(json, Sample.class);
        assertEquals("test", result.getName());
        assertEquals(20, result.getAge());
    }

    @Test
    void toObj_fromInputStream_byClass() throws IOException {
        byte[] data = "{\"name\":\"test\",\"age\":20,\"amount\":\"1.00\"}".getBytes();
        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            Sample result = JacksonUtils.toObj(is, Sample.class);
            assertEquals("test", result.getName());
        }
    }

    @Test
    void toObj_fromString_byType() {
        String json = "10";
        Integer result = JacksonUtils.toObj(json, Integer.class);
        assertEquals(10, result);
    }

    @Test
    void toObj_fromString_byTypeReference() {
        String json = "[\"a\",\"b\",\"c\"]";
        List<String> result = JacksonUtils.toObj(json, new TypeReference<List<String>>() {});
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    void toObj_fromBytes_byTypeReference() {
        byte[] json = "[1,2,3]".getBytes();
        List<Integer> result = JacksonUtils.toObj(json, new TypeReference<List<Integer>>() {});
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    void toObj_parametrizedType() {
        String json = "[{\"name\":\"test\",\"age\":20,\"amount\":\"1.00\"}]";
        List<Sample> result = JacksonUtils.toObj(json, List.class, Sample.class);
        assertEquals(1, result.size());
    }

    @Test
    void toObj_throwsDeserializationException_onInvalidJson() {
        String invalidJson = "not json";
        assertThrows(DeserializationException.class, () -> JacksonUtils.toObj(invalidJson, Sample.class));
    }

    // ========== Collection ==========

    @Test
    void toCollection_withExplicitType() {
        String json = "[1,2,3]";
        List<Integer> result = JacksonUtils.toCollection(json, ArrayList.class, Integer.class);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    void toCollection_defaultArrayList() {
        String json = "[\"a\",\"b\"]";
        ArrayList<String> result = JacksonUtils.toCollection(json, String.class);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
    }

    // ========== Map ==========

    @Test
    void toMap_withExplicitType() {
        String json = "{\"key\":\"value\"}";
        HashMap<String, String> result = JacksonUtils.toMap(json, HashMap.class, String.class, String.class);
        assertEquals("value", result.get("key"));
    }

    @Test
    void toMap_defaultHashMap() {
        String json = "{\"a\":1,\"b\":2}";
        HashMap<String, Integer> result = JacksonUtils.toMap(json, String.class, Integer.class);
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    // ========== CollectionMap ==========

    @Test
    void toCollectionMap_defaultTypes() {
        String json = "[{\"a\":1},{\"b\":2}]";
        ArrayList<HashMap<String, Integer>> result = JacksonUtils.toCollectionMap(json, String.class, Integer.class);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("a"));
    }

    // ========== Node operations ==========

    @Test
    void toObjectNode() {
        String json = "{\"key\":\"value\"}";
        ObjectNode node = JacksonUtils.toObjectNode(json);
        assertEquals("value", node.get("key").asText());
    }

    @Test
    void createEmptyJsonNode() {
        ObjectNode node = JacksonUtils.createEmptyJsonNode();
        assertNotNull(node);
        assertEquals(0, node.size());
    }

    @Test
    void createEmptyArrayNode() {
        ArrayNode node = JacksonUtils.createEmptyArrayNode();
        assertNotNull(node);
        assertEquals(0, node.size());
    }

    @Test
    void toJsonNode() {
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        JsonNode node = JacksonUtils.toJsonNode(sample);
        assertEquals("test", node.get("name").asText());
        assertEquals(20, node.get("age").asInt());
        assertEquals(0, new BigDecimal("1.00").compareTo(node.get("amount").decimalValue()));
    }

    // ========== Date format ==========

    @Test
    void toJson_localDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        DateSample sample = new DateSample(ldt, null, null, null);
        String json = JacksonUtils.toJson(sample);
        assertTrue(json.contains("\"dateTime\":\"2025-06-15 10:30:45\""));
    }

    @Test
    void toJson_localDate() {
        LocalDate ld = LocalDate.of(2025, 6, 15);
        DateSample sample = new DateSample(null, ld, null, null);
        String json = JacksonUtils.toJson(sample);
        assertTrue(json.contains("\"date\":\"2025-06-15\""));
    }

    @Test
    void toJson_localTime() {
        LocalTime lt = LocalTime.of(10, 30, 45);
        DateSample sample = new DateSample(null, null, lt, null);
        String json = JacksonUtils.toJson(sample);
        assertTrue(json.contains("\"time\":\"10:30:45\""));
    }

    // ========== BigDecimal ==========

    @Test
    void toJson_bigDecimal_serializedAsNumber() {
        Sample sample = new Sample("test", 20, new BigDecimal("1.01"));
        String json = JacksonUtils.toJson(sample);
        assertTrue(json.contains("\"amount\":1.01"));
    }

    @Test
    void roundTrip_preservesData() {
        Sample original = new Sample("test", 20, new BigDecimal("99.99"));
        String json = JacksonUtils.toJson(original);
        Sample restored = JacksonUtils.toObj(json, Sample.class);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getAge(), restored.getAge());
        assertEquals(original.getAmount(), restored.getAmount());
    }

    // ========== Ignore unknown properties ==========

    @Test
    void toObj_ignoresUnknownProperties() {
        String json = "{\"name\":\"test\",\"age\":20,\"amount\":\"1.00\",\"unknown\":\"field\"}";
        Sample result = JacksonUtils.toObj(json, Sample.class);
        assertEquals("test", result.getName());
        assertEquals(20, result.getAge());
    }
}