package io.ituknown.httpclient5.response;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class HeadersTest {

    @Test
    void addFieldAndGetField() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("Content-Type", "application/json"));

        MinimalField field = headers.getField("Content-Type");
        assertNotNull(field);
        assertEquals("Content-Type", field.name());
        assertEquals("application/json", field.value());
    }

    @Test
    void getFieldCaseInsensitive() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("Content-Type", "application/json"));

        assertEquals("application/json", headers.getField("content-type").value());
        assertEquals("application/json", headers.getField("CONTENT-TYPE").value());
    }

    @Test
    void getFieldReturnsFirstMatch() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("X-Custom", "value1"));
        headers.addField(new MinimalField("X-Custom", "value2"));

        MinimalField field = headers.getField("X-Custom");
        assertNotNull(field);
        assertEquals("value1", field.value());
    }

    @Test
    void getFieldNotFound() {
        Headers headers = new Headers();
        assertNull(headers.getField("Not-Exist"));
    }

    @Test
    void getFieldNull() {
        Headers headers = new Headers();
        assertNull(headers.getField(null));
    }

    @Test
    void getFieldsByName() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("Set-Cookie", "a=1"));
        headers.addField(new MinimalField("Set-Cookie", "b=2"));

        List<MinimalField> fields = headers.getFields("Set-Cookie");
        assertEquals(2, fields.size());
        assertEquals("a=1", fields.get(0).value());
        assertEquals("b=2", fields.get(1).value());
    }

    @Test
    void getFieldsByNameNotFound() {
        Headers headers = new Headers();
        List<MinimalField> fields = headers.getFields("Not-Exist");
        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    @Test
    void getFieldsByNameNull() {
        Headers headers = new Headers();
        List<MinimalField> fields = headers.getFields(null);
        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    @Test
    void getFieldsByNameReturnsUnmodifiableList() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("Set-Cookie", "a=1"));
        headers.addField(new MinimalField("Set-Cookie", "b=2"));

        List<MinimalField> fields = headers.getFields("Set-Cookie");
        assertEquals(2, fields.size());
        assertThrows(UnsupportedOperationException.class,
                () -> fields.add(new MinimalField("Set-Cookie", "c=3")));
        assertThrows(UnsupportedOperationException.class, () -> fields.clear());

        // 内部状态未被破坏：依然两条
        assertEquals(2, headers.getFields("Set-Cookie").size());
    }

    @Test
    void addFieldNull() {
        Headers headers = new Headers();
        headers.addField(null);
        assertTrue(headers.getFields().isEmpty());
    }

    @Test
    void getFieldsReturnsUnmodifiableList() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("Key", "Value"));

        List<MinimalField> fields = headers.getFields();
        assertThrows(UnsupportedOperationException.class, () -> fields.add(new MinimalField("Key2", "Value2")));
    }

    @Test
    void iteratorIsUnmodifiable() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("Key", "Value"));

        assertThrows(UnsupportedOperationException.class, () -> headers.iterator().remove());
    }

    @Test
    void iteratorTraversesAllFields() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("A", "1"));
        headers.addField(new MinimalField("B", "2"));
        headers.addField(new MinimalField("C", "3"));

        int count = 0;
        for (MinimalField ignored : headers) {
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    void emptyHeadersIterator() {
        Headers headers = new Headers();
        assertFalse(headers.iterator().hasNext());
        assertThrows(NoSuchElementException.class, () -> headers.iterator().next());
    }

    @Test
    void toStringFormat() {
        Headers headers = new Headers();
        headers.addField(new MinimalField("Content-Type", "text/html"));

        String str = headers.toString();
        assertTrue(str.contains("Content-Type"));
        assertTrue(str.contains("text/html"));
    }
}
