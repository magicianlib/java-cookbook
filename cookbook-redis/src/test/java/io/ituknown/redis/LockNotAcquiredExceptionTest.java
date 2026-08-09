package io.ituknown.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockNotAcquiredExceptionTest {

    @Test
    void carries_key_and_is_runtime() {
        LockNotAcquiredException ex = new LockNotAcquiredException("Foo#bar");

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("Foo#bar", ex.getKey());
        assertTrue(ex.getMessage().contains("Foo#bar"));
    }
}
