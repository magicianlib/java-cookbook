package io.ituknown.redis;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrottleStatusTest {

    @Test
    void from_allowed_decodes_all_fields() {
        ThrottleStatus r = ThrottleStatus.from(List.of(0L, 4L, 3L, -1L, 3L));

        assertTrue(r.allowed());
        assertEquals(4L, r.limit());
        assertEquals(3L, r.remaining());
        assertEquals(-1L, r.retryAfter());
        assertEquals(3L, r.resetAfter());
    }

    @Test
    void from_denied_flags_not_allowed_and_keeps_retryAfter() {
        ThrottleStatus r = ThrottleStatus.from(List.of(1L, 4L, 0L, 2L, 8L));

        assertFalse(r.allowed());
        assertEquals(2L, r.retryAfter());
    }

    @Test
    void from_accepts_integer_elements() {
        // 服务端可能把整数解码为 Integer，此处兼容
        ThrottleStatus r = ThrottleStatus.from(Arrays.asList(0, 4, 3, -1, 3));

        assertTrue(r.allowed());
        assertEquals(4L, r.limit());
    }
}
