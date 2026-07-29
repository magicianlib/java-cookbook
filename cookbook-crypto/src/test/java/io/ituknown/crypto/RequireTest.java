package io.ituknown.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RequireTest {

    @Test
    public void testRequireNonNullReturnsValueAndRejectsNull() {
        assertEquals("x", Require.requireNonNull("x", "name"));
        assertThrows(IllegalArgumentException.class,
                () -> Require.requireNonNull(null, "name"));
    }
}
