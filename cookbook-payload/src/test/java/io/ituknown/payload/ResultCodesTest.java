package io.ituknown.payload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultCodesTest {

    @Test
    void success_codeAndMessage() {
        assertEquals("000000", ResultCodes.SUCCESS.code());
        assertEquals("success", ResultCodes.SUCCESS.message());
        assertTrue(ResultCodes.SUCCESS.success());
    }

    @Test
    void failure_codeAndMessage() {
        assertEquals("000500", ResultCodes.FAILURE.code());
        assertEquals("failure", ResultCodes.FAILURE.message());
        assertFalse(ResultCodes.FAILURE.success());
    }

    @Test
    void tooManyRequests_codeAndMessage() {
        assertEquals("000429", ResultCodes.TOO_MANY_REQUESTS.code());
        assertEquals("too many requests", ResultCodes.TOO_MANY_REQUESTS.message());
        assertFalse(ResultCodes.TOO_MANY_REQUESTS.success());
    }

    @Test
    void implementsResultCode() {
        assertInstanceOf(ResultCode.class, ResultCodes.SUCCESS);
        assertInstanceOf(ResultCode.class, ResultCodes.FAILURE);
    }
}
