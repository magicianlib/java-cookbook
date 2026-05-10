package io.ituknown.jackson.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.ituknown.jackson.JacksonConfig;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JacksonBeanNullValueSerializerModifierTest {

    @Setter
    @Getter
    static class NullBean {
        private String str;
        private Integer num;
        private Boolean flag;
        private BigDecimal amount;
        private Map<String, String> map;
        private List<String> list;

        NullBean() {}

    }

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = JsonMapper.builder()
                .disable(SerializationFeature.INDENT_OUTPUT)
                .build();
        JacksonConfig.configureNullValueSerialization(mapper);
    }

    @Test
    void nullString_serializedAsEmpty() throws Exception {
        NullBean bean = new NullBean();
        String json = mapper.writeValueAsString(bean);
        assertTrue(json.contains("\"str\":\"\""));
    }

    @Test
    void nullNumber_serializedAsZero() throws Exception {
        NullBean bean = new NullBean();
        String json = mapper.writeValueAsString(bean);
        assertTrue(json.contains("\"num\":0"));
    }

    @Test
    void nullBoolean_serializedAsFalse() throws Exception {
        NullBean bean = new NullBean();
        String json = mapper.writeValueAsString(bean);
        assertTrue(json.contains("\"flag\":false"));
    }

    @Test
    void nullBigDecimal_serializedAsStringZero() throws Exception {
        NullBean bean = new NullBean();
        String json = mapper.writeValueAsString(bean);
        assertTrue(json.contains("\"amount\":\"0\""));
    }

    @Test
    void nullMap_serializedAsEmptyObject() throws Exception {
        NullBean bean = new NullBean();
        String json = mapper.writeValueAsString(bean);
        assertTrue(json.contains("\"map\":{}"));
    }

    @Test
    void nullList_serializedAsEmptyArray() throws Exception {
        NullBean bean = new NullBean();
        String json = mapper.writeValueAsString(bean);
        assertTrue(json.contains("\"list\":[]"));
    }

    @Test
    void allNullFields_noNullInOutput() throws Exception {
        NullBean bean = new NullBean();
        String json = mapper.writeValueAsString(bean);
        assertFalse(json.contains(":null"));
    }
}