package io.ituknown.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class MdcUtilsTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // ===== withTrace() =====

    @Test
    void withTrace_generatesRandomTraceId() {
        MdcUtils.withTrace();
        String traceId = MDC.get(MdcUtils.TRACE_ID);
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
        assertEquals(32, traceId.length());
    }

    @Test
    void withTrace_string_appendsToMdc() {
        MdcUtils.withTrace("abc123");
        assertEquals("abc123", MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void withTrace_string_chainsWithPipe() {
        MdcUtils.withTrace("first");
        MdcUtils.withTrace("second");
        assertEquals("first|second", MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void withTrace_string_null_ignored() {
        MdcUtils.withTrace((String) null);
        assertNull(MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void withTrace_string_empty_ignored() {
        MdcUtils.withTrace("");
        assertNull(MDC.get(MdcUtils.TRACE_ID));
    }

    // ===== withTraceScope =====

    @Test
    void withTraceScope_generatesTraceIdInsideScope() {
        String[] captured = new String[1];
        MdcUtils.withTraceScope(() -> captured[0] = MDC.get(MdcUtils.TRACE_ID));
        assertNotNull(captured[0]);
        assertEquals(32, captured[0].length());
    }

    @Test
    void withTraceScope_usesGivenTraceIdInsideScope() {
        MdcUtils.withTraceScope("scope123", () ->
                assertEquals("scope123", MDC.get(MdcUtils.TRACE_ID)));
    }

    @Test
    void withTraceScope_appendsWhenOuterTraceExists() {
        MdcUtils.withTrace("outer");
        MdcUtils.withTraceScope("inner", () ->
                assertEquals("outer|inner", MDC.get(MdcUtils.TRACE_ID)));
    }

    @Test
    void withTraceScope_restoresPreviousAfterScope() {
        MdcUtils.withTrace("outer");
        MdcUtils.withTraceScope("inner", () -> {
        });
        assertEquals("outer", MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void withTraceScope_clearsTraceWhenNoneExistedBefore() {
        MdcUtils.withTraceScope("inner", () -> {
        });
        assertNull(MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void withTraceScope_emptyTraceId_skipsSetButStillRestores() {
        MdcUtils.withTrace("outer");
        MdcUtils.withTraceScope("", () -> {
        });
        assertEquals("outer", MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void withTraceScope_restoresEvenWhenRunnableThrows() {
        MdcUtils.withTrace("outer");
        assertThrows(RuntimeException.class, () ->
                MdcUtils.withTraceScope("inner", () -> {
                    throw new RuntimeException("boom");
                }));
        assertEquals("outer", MDC.get(MdcUtils.TRACE_ID));
    }

    // ===== getMdc / setMdc =====

    @Test
    void getMdc_returnsValue() {
        MDC.put(MdcUtils.TRACE_ID, "abc");
        assertEquals("abc", MdcUtils.getMdc());
    }

    @Test
    void getMdc_returnsNullWhenMissing() {
        assertNull(MdcUtils.getMdc());
    }

    @Test
    void setMdc_setsValue() {
        MdcUtils.setMdc("xyz");
        assertEquals("xyz", MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void setMdc_overwrites() {
        MDC.put(MdcUtils.TRACE_ID, "old");
        MdcUtils.setMdc("new");
        assertEquals("new", MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void setMdc_null_ignored() {
        MdcUtils.setMdc(null);
        assertNull(MDC.get(MdcUtils.TRACE_ID));
    }

    @Test
    void setMdc_empty_ignored() {
        MdcUtils.setMdc("");
        assertNull(MDC.get(MdcUtils.TRACE_ID));
    }

    // ===== getUser / setUser =====

    @Test
    void getUser_returnsValue() {
        MDC.put(MdcUtils.USER_ID, "u001");
        assertEquals("u001", MdcUtils.getUser());
    }

    @Test
    void getUser_returnsNullWhenMissing() {
        assertNull(MdcUtils.getUser());
    }

    @Test
    void setUser_setsValue() {
        MdcUtils.setUser("u001");
        assertEquals("u001", MDC.get(MdcUtils.USER_ID));
    }

    @Test
    void setUser_null_ignored() {
        MdcUtils.setUser(null);
        assertNull(MDC.get(MdcUtils.USER_ID));
    }

    @Test
    void setUser_empty_ignored() {
        MdcUtils.setUser("");
        assertNull(MDC.get(MdcUtils.USER_ID));
    }

    // ===== get/put/remove/clear =====

    @Test
    void get_returnsValue() {
        MDC.put("testKey", "testValue");
        assertEquals("testValue", MdcUtils.get("testKey"));
    }

    @Test
    void get_returnsNullWhenMissing() {
        assertNull(MdcUtils.get("nonexistent"));
    }

    @Test
    void put_setsValue() {
        MdcUtils.put("key1", "value1");
        assertEquals("value1", MDC.get("key1"));
    }

    @Test
    void put_nullKey_ignored() {
        assertDoesNotThrow(() -> MdcUtils.put(null, "value"));
    }

    @Test
    void put_emptyKey_ignored() {
        MdcUtils.put("", "value");
        assertNull(MDC.get(""));
    }

    @Test
    void put_nullValue_ignored() {
        MdcUtils.put("key", null);
        assertNull(MDC.get("key"));
    }

    @Test
    void put_emptyValue_ignored() {
        MdcUtils.put("key", "");
        assertNull(MDC.get("key"));
    }

    @Test
    void remove_clearsKey() {
        MDC.put("key1", "value1");
        MdcUtils.remove("key1");
        assertNull(MDC.get("key1"));
    }

    @Test
    void clear_removesAll() {
        MDC.put("a", "1");
        MDC.put("b", "2");
        MdcUtils.clear();
        assertNull(MDC.get("a"));
        assertNull(MDC.get("b"));
    }
}
