package io.ituknown.jackson.serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BigDecimalAsStringJsonSerializerTest {

    private final BigDecimalAsStringJsonSerializer serializer = new BigDecimalAsStringJsonSerializer();

    private String serialize(BigDecimal value) throws Exception {
        StringWriter writer = new StringWriter();
        JsonGenerator gen = new JsonFactory().createGenerator(writer);
        gen.writeStartObject();
        gen.writeFieldName("value");
        serializer.serialize(value, gen, null);
        gen.writeEndObject();
        gen.flush();
        return writer.toString();
    }

    @Test
    void serialize_normalValue() throws Exception {
        String result = serialize(new BigDecimal("1.01"));
        assertEquals("{\"value\":\"1.01\"}", result);
    }

    @Test
    void serialize_zero() throws Exception {
        String result = serialize(BigDecimal.ZERO);
        assertEquals("{\"value\":\"0\"}", result);
    }

    @Test
    void serialize_negativeValue() throws Exception {
        String result = serialize(new BigDecimal("-99.99"));
        assertEquals("{\"value\":\"-99.99\"}", result);
    }

    @Test
    void serialize_largePrecision() throws Exception {
        String result = serialize(new BigDecimal("123456789.123456789"));
        assertEquals("{\"value\":\"123456789.123456789\"}", result);
    }

    @Test
    void serialize_nullValue() throws Exception {
        String result = serialize(null);
        assertEquals("{\"value\":null}", result);
    }

    @Test
    void serialize_scientificNotationValue() throws Exception {
        String result = serialize(new BigDecimal("1E+10"));
        assertEquals("{\"value\":\"10000000000\"}", result);
    }
}
