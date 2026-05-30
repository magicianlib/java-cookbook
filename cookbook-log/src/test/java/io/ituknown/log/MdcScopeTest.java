package io.ituknown.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MdcScopeTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // ===== of(key, value) =====

    @Test
    void of_single_setsValue() {
        try (MdcScope scope = MdcScope.of("userId", "123")) {
            assertEquals("123", MDC.get("userId"));
        }
    }

    @Test
    void of_single_removesOnClose() {
        try (MdcScope scope = MdcScope.of("userId", "123")) {
            // inside scope
        }
        assertNull(MDC.get("userId"));
    }

    @Test
    void of_single_restoresPreviousValue() {
        MDC.put("userId", "old");
        try (MdcScope scope = MdcScope.of("userId", "new")) {
            assertEquals("new", MDC.get("userId"));
        }
        assertEquals("old", MDC.get("userId"));
    }

    // ===== of(key1, val1, key2, val2) =====

    @Test
    void of_pair_setsBothValues() {
        try (MdcScope scope = MdcScope.of("userId", "123", "tenantId", "t1")) {
            assertEquals("123", MDC.get("userId"));
            assertEquals("t1", MDC.get("tenantId"));
        }
    }

    @Test
    void of_pair_removesBothOnClose() {
        try (MdcScope scope = MdcScope.of("userId", "123", "tenantId", "t1")) {
            // inside scope
        }
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("tenantId"));
    }

    // ===== of(Map) =====

    @Test
    void of_map_setsAllValues() {
        try (MdcScope scope = MdcScope.of(Map.of("k1", "v1", "k2", "v2"))) {
            assertEquals("v1", MDC.get("k1"));
            assertEquals("v2", MDC.get("k2"));
        }
    }

    @Test
    void of_map_removesAllOnClose() {
        try (MdcScope scope = MdcScope.of(Map.of("k1", "v1", "k2", "v2"))) {
            // inside scope
        }
        assertNull(MDC.get("k1"));
        assertNull(MDC.get("k2"));
    }

    // ===== wrap =====

    @Test
    void wrap_executesWithinScope() {
        boolean[] executed = {false};
        MdcScope.wrap("userId", "123", () -> {
            assertEquals("123", MDC.get("userId"));
            executed[0] = true;
        });
        assertTrue(executed[0]);
        assertNull(MDC.get("userId"));
    }

    @Test
    void wrap_cleansUpOnException() {
        assertThrows(RuntimeException.class, () ->
            MdcScope.wrap("userId", "123", () -> {
                throw new RuntimeException("test");
            })
        );
        assertNull(MDC.get("userId"));
    }

    // ===== nested scope =====

    @Test
    void nestedScope_restoresOuterValue() {
        MDC.put("userId", "outer");
        try (MdcScope inner = MdcScope.of("userId", "inner")) {
            assertEquals("inner", MDC.get("userId"));
            try (MdcScope deepest = MdcScope.of("userId", "deepest")) {
                assertEquals("deepest", MDC.get("userId"));
            }
            assertEquals("inner", MDC.get("userId"));
        }
        assertEquals("outer", MDC.get("userId"));
    }
}
