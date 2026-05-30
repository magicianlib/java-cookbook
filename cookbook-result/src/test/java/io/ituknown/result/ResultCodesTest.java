package io.ituknown.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultCodesTest {

    @Test
    void success_codeAndMessage() {
        assertEquals("00000", ResultCodes.SUCCESS.code());
        assertEquals("success", ResultCodes.SUCCESS.message());
        assertTrue(ResultCodes.SUCCESS.success());
    }

    @Test
    void failure_codeAndMessage() {
        assertEquals("00001", ResultCodes.FAILURE.code());
        assertEquals("failure", ResultCodes.FAILURE.message());
        assertFalse(ResultCodes.FAILURE.success());
    }

    @Test
    void implementsResultCode() {
        assertInstanceOf(ResultCode.class, ResultCodes.SUCCESS);
        assertInstanceOf(ResultCode.class, ResultCodes.FAILURE);
    }
}
