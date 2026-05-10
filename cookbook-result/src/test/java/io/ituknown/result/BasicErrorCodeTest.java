package io.ituknown.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicErrorCodeTest {

    @Test
    void success_codeAndMessage() {
        assertEquals(0, BasicErrorCode.SUCCESS.code());
        assertEquals("Success", BasicErrorCode.SUCCESS.message());
        assertTrue(BasicErrorCode.SUCCESS.success());
    }

    @Test
    void failure_codeAndMessage() {
        assertEquals(1, BasicErrorCode.FAILURE.code());
        assertEquals("Failure", BasicErrorCode.FAILURE.message());
        assertFalse(BasicErrorCode.FAILURE.success());
    }

    @Test
    void implementsErrorCode() {
        assertInstanceOf(ErrorCode.class, BasicErrorCode.SUCCESS);
        assertInstanceOf(ErrorCode.class, BasicErrorCode.FAILURE);
    }
}
