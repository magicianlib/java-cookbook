package io.ituknown.redis;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitExceededExceptionTest {

    @Test
    void carries_result_fields_and_is_runtime() {
        ThrottleResult result = ThrottleResult.from(List.of(1L, 4L, 0L, 2L, 8L));

        RateLimitExceededException ex = new RateLimitExceededException("Foo#bar", result);

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("Foo#bar", ex.getKey());
        assertEquals(4L, ex.getLimit());
        assertEquals(0L, ex.getRemaining());
        assertEquals(2L, ex.getRetryAfter());
        assertEquals(8L, ex.getResetAfter());
        assertTrue(ex.getMessage().contains("Foo#bar"));
    }
}
