package io.ituknown.httpclient5.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinimalFieldTest {

    @Test
    void recordCreation() {
        MinimalField field = new MinimalField("Content-Type", "application/json");
        assertEquals("Content-Type", field.name());
        assertEquals("application/json", field.value());
    }

    @Test
    void toStringFormat() {
        MinimalField field = new MinimalField("Content-Type", "application/json");
        assertEquals("Content-Type: application/json", field.toString());
    }

    @Test
    void equality() {
        MinimalField a = new MinimalField("Key", "Value");
        MinimalField b = new MinimalField("Key", "Value");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequality() {
        MinimalField a = new MinimalField("Key", "Value1");
        MinimalField b = new MinimalField("Key", "Value2");
        assertNotEquals(a, b);
    }
}
