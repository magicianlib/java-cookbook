package io.ituknown.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultUtilsTest {

    // ========== createSuccess ==========

    @Test
    void createSuccess_noData() {
        PageResult<Void> result = PageResultUtils.createSuccess();
        assertTrue(result.isSuccess());
        assertEquals(0, result.getCode());
        assertEquals("Success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void createSuccess_withData() {
        PageResult<String> result = PageResultUtils.createSuccess(List.of("a", "b"));
        assertTrue(result.isSuccess());
        assertEquals(List.of("a", "b"), result.getData());
    }

    @Test
    void createSuccess_withPagination() {
        PageResult<String> result = PageResultUtils.createSuccess(List.of("a", "b"), 1, 10, 25);
        assertTrue(result.isSuccess());
        assertEquals(List.of("a", "b"), result.getData());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
        assertEquals(25, result.getTotalCount());
        assertEquals(3, result.getTotalPage());
    }

    @Test
    void createSuccess_withPagination_exactDivision() {
        PageResult<String> result = PageResultUtils.createSuccess(List.of("a"), 2, 10, 20);
        assertEquals(2, result.getTotalPage());
    }

    // ========== createFailure ==========

    @Test
    void createFailure_noArgs() {
        PageResult<Void> result = PageResultUtils.createFailure();
        assertFalse(result.isSuccess());
        assertEquals(1, result.getCode());
        assertEquals("Failure", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void createFailure_customMessage() {
        PageResult<Void> result = PageResultUtils.createFailure("查询失败");
        assertFalse(result.isSuccess());
        assertEquals("查询失败", result.getMessage());
    }

    @Test
    void createFailure_withData() {
        PageResult<String> result = PageResultUtils.createFailure(List.of("partial"));
        assertFalse(result.isSuccess());
        assertEquals(List.of("partial"), result.getData());
    }

    @Test
    void createFailure_messageAndData() {
        PageResult<Integer> result = PageResultUtils.createFailure("超时", List.of(1, 2));
        assertEquals("超时", result.getMessage());
        assertEquals(List.of(1, 2), result.getData());
    }

    // ========== create (custom ErrorCode) ==========

    @Test
    void create_customErrorCode() {
        ErrorCode custom = new TestErrorCode(20001, "自定义分页错误", false);
        PageResult<Void> result = PageResultUtils.create(custom);
        assertFalse(result.isSuccess());
        assertEquals(20001, result.getCode());
        assertEquals("自定义分页错误", result.getMessage());
    }

    @Test
    void create_customErrorCodeWithData() {
        ErrorCode custom = new TestErrorCode(20002, "错误", false);
        PageResult<String> result = PageResultUtils.create(custom, List.of("item"));
        assertEquals(20002, result.getCode());
        assertEquals(List.of("item"), result.getData());
    }

    @Test
    void create_customErrorCodeWithMessage() {
        ErrorCode custom = new TestErrorCode(20003, "原始", false);
        PageResult<Void> result = PageResultUtils.create(custom, "覆盖");
        assertEquals(20003, result.getCode());
        assertEquals("覆盖", result.getMessage());
    }

    @Test
    void create_customErrorCodeWithMessageAndData() {
        ErrorCode custom = new TestErrorCode(20004, "原始", true);
        PageResult<Integer> result = PageResultUtils.create(custom, "覆盖", List.of(1));
        assertTrue(result.isSuccess());
        assertEquals("覆盖", result.getMessage());
        assertEquals(List.of(1), result.getData());
    }

    record TestErrorCode(int code, String message, boolean success) implements ErrorCode {}
}
