package io.ituknown.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultUtilsTest {

    // ========== createSuccess ==========

    @Test
    void createSuccess_noData() {
        Result<Void> result = ResultUtils.createSuccess();
        assertTrue(result.isSuccess());
        assertEquals(0, result.getCode());
        assertEquals("Success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void createSuccess_withData() {
        Result<String> result = ResultUtils.createSuccess("hello");
        assertTrue(result.isSuccess());
        assertEquals(0, result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    void createSuccess_withNullData() {
        Result<String> result = ResultUtils.createSuccess(null);
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    // ========== createFailure ==========

    @Test
    void createFailure_noArgs() {
        Result<Void> result = ResultUtils.createFailure();
        assertFalse(result.isSuccess());
        assertEquals(1, result.getCode());
        assertEquals("Failure", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void createFailure_customMessage() {
        Result<Void> result = ResultUtils.createFailure("参数错误");
        assertFalse(result.isSuccess());
        assertEquals(1, result.getCode());
        assertEquals("参数错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void createFailure_withData() {
        Result<String> result = ResultUtils.createFailure("error detail");
        assertFalse(result.isSuccess());
        assertEquals("error detail", result.getMessage());
    }

    @Test
    void createFailure_messageAndData() {
        Result<Integer> result = ResultUtils.createFailure("操作失败", 404);
        assertFalse(result.isSuccess());
        assertEquals("操作失败", result.getMessage());
        assertEquals(404, result.getData());
    }

    // ========== create (custom ErrorCode) ==========

    @Test
    void create_customErrorCode() {
        ErrorCode custom = new TestErrorCode(10001, "自定义错误", false);
        Result<Void> result = ResultUtils.create(custom);
        assertFalse(result.isSuccess());
        assertEquals(10001, result.getCode());
        assertEquals("自定义错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void create_customErrorCodeWithData() {
        ErrorCode custom = new TestErrorCode(10002, "带数据", false);
        Result<Integer> result = ResultUtils.create(custom, 42);
        assertEquals(10002, result.getCode());
        assertEquals(42, result.getData());
    }

    @Test
    void create_customErrorCodeWithMessage() {
        ErrorCode custom = new TestErrorCode(10003, "原始信息", false);
        Result<Void> result = ResultUtils.create(custom, "覆盖信息");
        assertEquals(10003, result.getCode());
        assertEquals("覆盖信息", result.getMessage());
    }

    @Test
    void create_customErrorCodeWithMessageAndData() {
        ErrorCode custom = new TestErrorCode(10004, "原始信息", true);
        Result<Long> result = ResultUtils.create(custom, "覆盖信息", 123L);
        assertTrue(result.isSuccess());
        assertEquals(10004, result.getCode());
        assertEquals("覆盖信息", result.getMessage());
        assertEquals(123L, result.getData());
    }

    // ========== Result toString ==========

    @Test
    void toString_validJson() {
        Result<String> result = ResultUtils.createSuccess("test");
        String str = result.toString();
        assertTrue(str.contains("\"success\":true"));
        assertTrue(str.contains("\"data\":\"test\""));
    }

    // ========== helper ==========

    record TestErrorCode(int code, String message, boolean success) implements ErrorCode {}
}
