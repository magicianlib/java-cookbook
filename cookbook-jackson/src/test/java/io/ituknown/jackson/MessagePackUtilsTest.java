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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MessagePackUtilsTest {

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

    // ========== ObjectMapper ==========

    @Test
    void getObjectMapper_default() {
        ObjectMapper mapper = MessagePackUtils.getObjectMapper();
        assertNotNull(mapper);
    }

    @Test
    void getObjectMapper_format() {
        ObjectMapper formatMapper = MessagePackUtils.getObjectMapper(true);
        ObjectMapper noFormatMapper = MessagePackUtils.getObjectMapper(false);
        assertNotSame(formatMapper, noFormatMapper);
    }

    @Test
    void createObjectMapper_createsNewInstance() {
        ObjectMapper m1 = MessagePackUtils.createObjectMapper(false);
        ObjectMapper m2 = MessagePackUtils.createObjectMapper(false);
        assertNotSame(m1, m2);
    }

    // ========== toBytes 序列化 ==========

    @Test
    void toBytes_basicObject() {
        Sample sample = new Sample("test", 20, new BigDecimal("99.99"));
        byte[] bytes = MessagePackUtils.toBytes(sample);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void toBytes_withFormat() {
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        byte[] bytes = MessagePackUtils.toBytes(sample, true);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void toBytes_withCustomMapper() {
        ObjectMapper mapper = MessagePackUtils.createObjectMapper(false);
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        byte[] bytes = MessagePackUtils.toBytes(sample, mapper);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void toBytes_throwsSerializationException_onFailure() {
        Object obj = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("test error");
            }
        };
        assertThrows(SerializationException.class, () -> MessagePackUtils.toBytes(obj));
    }

    // ========== toBase64 序列化 ==========

    @Test
    void toBase64_basicObject() {
        Sample sample = new Sample("test", 20, new BigDecimal("99.99"));
        String base64 = MessagePackUtils.toBase64(sample);
        assertNotNull(base64);
        assertFalse(base64.isEmpty());
        // Base64 编码后应能正常解码
        assertNotNull(Base64.getDecoder().decode(base64));
    }

    @Test
    void toBase64_withFormat() {
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        String base64 = MessagePackUtils.toBase64(sample, true);
        assertNotNull(base64);
        assertFalse(base64.isEmpty());
    }

    @Test
    void toBase64_withCustomMapper() {
        ObjectMapper mapper = MessagePackUtils.createObjectMapper(false);
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        String base64 = MessagePackUtils.toBase64(sample, mapper);
        assertNotNull(base64);
        assertFalse(base64.isEmpty());
    }

    @Test
    void toBase64_consistentWithToBytes() {
        Sample sample = new Sample("test", 20, new BigDecimal("99.99"));
        byte[] bytes = MessagePackUtils.toBytes(sample);
        String base64 = MessagePackUtils.toBase64(sample);
        assertArrayEquals(bytes, Base64.getDecoder().decode(base64));
    }

    // ========== toObj (Class) - byte[] ==========

    @Test
    void toObj_fromBytes_byClass() {
        Sample original = new Sample("test", 20, new BigDecimal("99.99"));
        byte[] bytes = MessagePackUtils.toBytes(original);
        Sample result = MessagePackUtils.toObj(bytes, Sample.class);
        assertEquals("test", result.getName());
        assertEquals(20, result.getAge());
        assertEquals(new BigDecimal("99.99"), result.getAmount());
    }

    @Test
    void toObj_fromBytes_byClass_withFormat() {
        Sample original = new Sample("test", 20, new BigDecimal("1.00"));
        byte[] bytes = MessagePackUtils.toBytes(original, true);
        Sample result = MessagePackUtils.toObj(bytes, Sample.class, true);
        assertEquals("test", result.getName());
    }

    @Test
    void toObj_fromBytes_byClass_withCustomMapper() {
        ObjectMapper mapper = MessagePackUtils.createObjectMapper(false);
        Sample original = new Sample("test", 20, new BigDecimal("1.00"));
        byte[] bytes = MessagePackUtils.toBytes(original, mapper);
        Sample result = MessagePackUtils.toObj(bytes, Sample.class, mapper);
        assertEquals("test", result.getName());
    }

    @Test
    void toObj_fromBytes_throwsDeserializationException_onInvalidData() {
        byte[] invalidData = "not msgpack".getBytes();
        assertThrows(DeserializationException.class, () -> MessagePackUtils.toObj(invalidData, Sample.class));
    }

    // ========== toObj (Class) - base64 ==========

    @Test
    void toObj_fromBase64_byClass() {
        Sample original = new Sample("test", 20, new BigDecimal("99.99"));
        String base64 = MessagePackUtils.toBase64(original);
        Sample result = MessagePackUtils.toObj(base64, Sample.class);
        assertEquals("test", result.getName());
        assertEquals(20, result.getAge());
        assertEquals(new BigDecimal("99.99"), result.getAmount());
    }

    @Test
    void toObj_fromBase64_byClass_withFormat() {
        Sample original = new Sample("test", 20, new BigDecimal("1.00"));
        String base64 = MessagePackUtils.toBase64(original, true);
        Sample result = MessagePackUtils.toObj(base64, Sample.class, true);
        assertEquals("test", result.getName());
    }

    @Test
    void toObj_fromBase64_byClass_withCustomMapper() {
        ObjectMapper mapper = MessagePackUtils.createObjectMapper(false);
        Sample original = new Sample("test", 20, new BigDecimal("1.00"));
        String base64 = MessagePackUtils.toBase64(original, mapper);
        Sample result = MessagePackUtils.toObj(base64, Sample.class, mapper);
        assertEquals("test", result.getName());
    }

    @Test
    void toObj_fromBase64_throwsDeserializationException_onInvalidData() {
        String invalidBase64 = Base64.getEncoder().encodeToString("not msgpack".getBytes());
        assertThrows(DeserializationException.class, () -> MessagePackUtils.toObj(invalidBase64, Sample.class));
    }

    // ========== toObj (InputStream + Class) ==========

    @Test
    void toObj_fromInputStream_byClass() throws IOException {
        Sample original = new Sample("test", 20, new BigDecimal("99.99"));
        byte[] bytes = MessagePackUtils.toBytes(original);
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            Sample result = MessagePackUtils.toObj(is, Sample.class);
            assertEquals("test", result.getName());
            assertEquals(20, result.getAge());
        }
    }

    @Test
    void toObj_fromInputStream_byClass_withFormat() throws IOException {
        Sample original = new Sample("test", 20, new BigDecimal("1.00"));
        byte[] bytes = MessagePackUtils.toBytes(original, true);
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            Sample result = MessagePackUtils.toObj(is, Sample.class, true);
            assertEquals("test", result.getName());
        }
    }

    @Test
    void toObj_fromInputStream_byClass_withCustomMapper() throws IOException {
        ObjectMapper mapper = MessagePackUtils.createObjectMapper(false);
        Sample original = new Sample("test", 20, new BigDecimal("1.00"));
        byte[] bytes = MessagePackUtils.toBytes(original, mapper);
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            Sample result = MessagePackUtils.toObj(is, Sample.class, mapper);
            assertEquals("test", result.getName());
        }
    }

    // ========== toObj (Type) - byte[] ==========

    @Test
    void toObj_fromBytes_byType() {
        byte[] bytes = MessagePackUtils.toBytes(10);
        Integer result = MessagePackUtils.toObj(bytes, Integer.class);
        assertEquals(10, result);
    }

    @Test
    void toObj_fromBase64_byType() {
        String base64 = MessagePackUtils.toBase64(10);
        Integer result = MessagePackUtils.toObj(base64, Integer.class);
        assertEquals(10, result);
    }

    // ========== toObj (TypeReference) - byte[] ==========

    @Test
    void toObj_fromBytes_byTypeReference() {
        List<String> original = Arrays.asList("a", "b", "c");
        byte[] bytes = MessagePackUtils.toBytes(original);
        List<String> result = MessagePackUtils.toObj(bytes, new TypeReference<List<String>>() {});
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("c", result.get(2));
    }

    @Test
    void toObj_fromBase64_byTypeReference() {
        List<Integer> original = Arrays.asList(1, 2, 3);
        String base64 = MessagePackUtils.toBase64(original);
        List<Integer> result = MessagePackUtils.toObj(base64, new TypeReference<List<Integer>>() {});
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
    }

    // ========== toObj (InputStream + Type) ==========

    @Test
    void toObj_fromInputStream_byType() throws IOException {
        byte[] bytes = MessagePackUtils.toBytes(42);
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            Integer result = MessagePackUtils.toObj(is, Integer.class);
            assertEquals(42, result);
        }
    }

    // ========== toObj (参数化类型) - byte[] ==========

    @Test
    void toObj_parametrizedType_fromBytes() {
        List<Sample> original = Collections.singletonList(new Sample("test", 20, new BigDecimal("1.00")));
        byte[] bytes = MessagePackUtils.toBytes(original);
        List<Sample> result = MessagePackUtils.toObj(bytes, List.class, Sample.class);
        assertEquals(1, result.size());
        assertEquals("test", result.get(0).getName());
    }

    @Test
    void toObj_parametrizedType_fromBase64() {
        List<Sample> original = Collections.singletonList(new Sample("test", 20, new BigDecimal("1.00")));
        String base64 = MessagePackUtils.toBase64(original);
        List<Sample> result = MessagePackUtils.toObj(base64, List.class, Sample.class);
        assertEquals(1, result.size());
        assertEquals("test", result.get(0).getName());
    }

    // ========== toObjectNode ==========

    @Test
    void toObjectNode_fromBytes() {
        Sample original = new Sample("test", 20, new BigDecimal("1.00"));
        byte[] bytes = MessagePackUtils.toBytes(original);
        ObjectNode node = MessagePackUtils.toObjectNode(bytes);
        assertEquals("test", node.get("name").asText());
        assertEquals(20, node.get("age").asInt());
        assertEquals("1.00", node.get("amount").asText());
    }

    @Test
    void toObjectNode_fromBase64() {
        Sample original = new Sample("test", 20, new BigDecimal("1.00"));
        String base64 = MessagePackUtils.toBase64(original);
        ObjectNode node = MessagePackUtils.toObjectNode(base64);
        assertEquals("test", node.get("name").asText());
    }

    // ========== toCollection ==========

    @Test
    void toCollection_fromBytes_withExplicitType() {
        List<Integer> original = Arrays.asList(1, 2, 3);
        byte[] bytes = MessagePackUtils.toBytes(original);
        ArrayList<Integer> result = MessagePackUtils.toCollection(bytes, ArrayList.class, Integer.class);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    void toCollection_fromBytes_defaultArrayList() {
        List<String> original = Arrays.asList("a", "b");
        byte[] bytes = MessagePackUtils.toBytes(original);
        ArrayList<String> result = MessagePackUtils.toCollection(bytes, String.class);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    void toCollection_fromBase64_withExplicitType() {
        List<Integer> original = Arrays.asList(1, 2, 3);
        String base64 = MessagePackUtils.toBase64(original);
        ArrayList<Integer> result = MessagePackUtils.toCollection(base64, ArrayList.class, Integer.class);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    void toCollection_fromBase64_defaultArrayList() {
        List<String> original = Arrays.asList("a", "b");
        String base64 = MessagePackUtils.toBase64(original);
        ArrayList<String> result = MessagePackUtils.toCollection(base64, String.class);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
    }

    // ========== toMap ==========

    @Test
    void toMap_fromBytes_withExplicitType() {
        Map<String, String> original = new LinkedHashMap<>();
        original.put("key", "value");
        byte[] bytes = MessagePackUtils.toBytes(original);
        HashMap<String, String> result = MessagePackUtils.toMap(bytes, HashMap.class, String.class, String.class);
        assertEquals("value", result.get("key"));
    }

    @Test
    void toMap_fromBytes_defaultHashMap() {
        Map<String, Integer> original = new LinkedHashMap<>();
        original.put("a", 1);
        original.put("b", 2);
        byte[] bytes = MessagePackUtils.toBytes(original);
        HashMap<String, Integer> result = MessagePackUtils.toMap(bytes, String.class, Integer.class);
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    void toMap_fromBase64_withExplicitType() {
        Map<String, String> original = new LinkedHashMap<>();
        original.put("key", "value");
        String base64 = MessagePackUtils.toBase64(original);
        HashMap<String, String> result = MessagePackUtils.toMap(base64, HashMap.class, String.class, String.class);
        assertEquals("value", result.get("key"));
    }

    @Test
    void toMap_fromBase64_defaultHashMap() {
        Map<String, Integer> original = new LinkedHashMap<>();
        original.put("a", 1);
        original.put("b", 2);
        String base64 = MessagePackUtils.toBase64(original);
        HashMap<String, Integer> result = MessagePackUtils.toMap(base64, String.class, Integer.class);
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    // ========== toCollectionMap ==========

    @Test
    void toCollectionMap_fromBytes_defaultTypes() {
        List<Map<String, Integer>> original = new ArrayList<>();
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("b", 2);
        original.add(m1);
        original.add(m2);
        byte[] bytes = MessagePackUtils.toBytes(original);

        ArrayList<HashMap<String, Integer>> result = MessagePackUtils.toCollectionMap(bytes, String.class, Integer.class);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("a"));
        assertEquals(2, result.get(1).get("b"));
    }

    @Test
    void toCollectionMap_fromBase64_defaultTypes() {
        List<Map<String, Integer>> original = new ArrayList<>();
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("b", 2);
        original.add(m1);
        original.add(m2);
        String base64 = MessagePackUtils.toBase64(original);

        ArrayList<HashMap<String, Integer>> result = MessagePackUtils.toCollectionMap(base64, String.class, Integer.class);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("a"));
        assertEquals(2, result.get(1).get("b"));
    }

    // ========== 节点工厂 ==========

    @Test
    void createEmptyJsonNode() {
        ObjectNode node = MessagePackUtils.createEmptyJsonNode();
        assertNotNull(node);
        assertEquals(0, node.size());
    }

    @Test
    void createEmptyArrayNode() {
        ArrayNode node = MessagePackUtils.createEmptyArrayNode();
        assertNotNull(node);
        assertEquals(0, node.size());
    }

    @Test
    void toJsonNode() {
        Sample sample = new Sample("test", 20, new BigDecimal("1.00"));
        JsonNode node = MessagePackUtils.toJsonNode(sample);
        assertEquals("test", node.get("name").asText());
        assertEquals(20, node.get("age").asInt());
        assertEquals("1.00", node.get("amount").asText());
    }

    // ========== roundTrip 完整往返测试 ==========

    @Test
    void roundTrip_bytes_byClass() {
        Sample original = new Sample("test", 20, new BigDecimal("99.99"));
        byte[] bytes = MessagePackUtils.toBytes(original);
        Sample restored = MessagePackUtils.toObj(bytes, Sample.class);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getAge(), restored.getAge());
        assertEquals(original.getAmount(), restored.getAmount());
    }

    @Test
    void roundTrip_base64_byClass() {
        Sample original = new Sample("test", 20, new BigDecimal("99.99"));
        String base64 = MessagePackUtils.toBase64(original);
        Sample restored = MessagePackUtils.toObj(base64, Sample.class);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getAge(), restored.getAge());
        assertEquals(original.getAmount(), restored.getAmount());
    }

    @Test
    void roundTrip_bytes_byTypeReference() {
        List<String> original = Arrays.asList("a", "b", "c");
        byte[] bytes = MessagePackUtils.toBytes(original);
        List<String> restored = MessagePackUtils.toObj(bytes, new TypeReference<List<String>>() {});
        assertEquals(original, restored);
    }

    @Test
    void roundTrip_base64_byTypeReference() {
        List<Integer> original = Arrays.asList(1, 2, 3);
        String base64 = MessagePackUtils.toBase64(original);
        List<Integer> restored = MessagePackUtils.toObj(base64, new TypeReference<List<Integer>>() {});
        assertEquals(original, restored);
    }

    // ========== 忽略未知属性 ==========

    @Test
    void toObj_ignoresUnknownProperties() {
        // 先序列化一个包含更多字段的对象
        @Getter
        @Setter
        class Full {
            private String name = "test";
            private int age = 20;
            private BigDecimal amount = new BigDecimal("1.00");
            private String extra = "unknown";
        }
        byte[] bytes = MessagePackUtils.toBytes(new Full());
        // 反序列化为只有 name/age/amount 的 Sample，应不报错
        Sample result = MessagePackUtils.toObj(bytes, Sample.class);
        assertEquals("test", result.getName());
        assertEquals(20, result.getAge());
    }
}
