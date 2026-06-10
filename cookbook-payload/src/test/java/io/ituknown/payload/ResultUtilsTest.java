package io.ituknown.payload;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultUtilsTest {

    // ========== success ==========

    @Test
    void success_noData() {
        Result<Void> result = ResultUtils.success();
        assertEquals("00000", result.getCode());
        assertEquals("success", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void success_withData() {
        Result<String> result = ResultUtils.success("hello");
        assertEquals("00000", result.getCode());
        assertEquals("success", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    void success_withList() {
        Result<List<Integer>> result = ResultUtils.success(List.of(1, 2, 3));
        assertEquals("00000", result.getCode());
        assertEquals(List.of(1, 2, 3), result.getData());
    }

    // ========== successPage ==========

    @Test
    void successPage() {
        List<String> list = List.of("a", "b");
        Result<Page<String>> result = ResultUtils.successPage(list, 1, 10, 25);

        assertEquals("00000", result.getCode());
        assertEquals("success", result.getMsg());

        Page<String> data = result.getData();
        assertEquals(list, data.list());

        Pagination pagination = data.pagination();
        assertEquals(25, pagination.getTotal());
        assertEquals(10, pagination.getPageSize());
        assertEquals(1, pagination.getCurrent());
        assertEquals(3, pagination.getPages());
    }

    @Test
    void successPage_exactDivision() {
        Result<Page<String>> result = ResultUtils.successPage(List.of("a"), 2, 10, 20);
        assertEquals(2, result.getData().pagination().getPages());
    }

    @Test
    void successPage_nullList_defaultsToEmpty() {
        Result<Page<String>> result = ResultUtils.successPage(null, 1, 10, 0);
        assertNotNull(result.getData().list());
        assertTrue(result.getData().list().isEmpty());
    }

    @Test
    void page_nullPagination_throwsNPE() {
        assertThrows(NullPointerException.class, () -> new Page<>(List.of("a"), (Pagination) null));
    }

    // ========== successCursor ==========

    @Test
    void successCursor_hasMore() {
        List<String> list = List.of("a", "b");
        Result<CursorPage<String, String>> result = ResultUtils.successCursor(list, "eyJpZCI6MTB9", true, 20);

        assertEquals("00000", result.getCode());
        assertEquals("success", result.getMsg());

        CursorPage<String, String> data = result.getData();
        assertEquals(list, data.list());

        CursorPagination<String> pagination = data.pagination();
        assertTrue(pagination.hasMore());
        assertEquals("eyJpZCI6MTB9", pagination.nextCursor());
        assertEquals(20, pagination.pageSize());
    }

    @Test
    void successCursor_noMore() {
        List<String> list = List.of("last");
        Result<CursorPage<String, String>> result = ResultUtils.successCursor(list, null, false, 10);

        CursorPagination<String> pagination = result.getData().pagination();
        assertFalse(pagination.hasMore());
        assertNull(pagination.nextCursor());
        assertEquals(10, pagination.pageSize());
    }

    @Test
    void successCursor_longCursor() {
        Result<CursorPage<String, Long>> result = ResultUtils.successCursor(List.of("a"), 42L, true, 20);

        CursorPagination<Long> pagination = result.getData().pagination();
        assertEquals(42L, pagination.nextCursor());
    }

    @Test
    void successCursor_nullList_defaultsToEmpty() {
        Result<CursorPage<String, String>> result = ResultUtils.successCursor(null, null, false, 10);
        assertNotNull(result.getData().list());
        assertTrue(result.getData().list().isEmpty());
    }

    @Test
    void cursorPage_nullPagination_throwsNPE() {
        assertThrows(NullPointerException.class, () -> new CursorPage<>(List.of("a"), (CursorPagination<String>) null));
    }

    // ========== failure ==========

    @Test
    void failure_noArgs() {
        Result<Void> result = ResultUtils.failure();
        assertEquals("00001", result.getCode());
        assertEquals("failure", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void failure_customMsg() {
        Result<Void> result = ResultUtils.failure("参数错误");
        assertEquals("00001", result.getCode());
        assertEquals("参数错误", result.getMsg());
        assertNull(result.getData());
    }

    // ========== create (custom ResultCode) ==========

    @Test
    void create_customResultCode() {
        ResultCode custom = new TestResultCode("10001", "自定义错误", false);
        Result<Void> result = ResultUtils.create(custom);
        assertEquals("10001", result.getCode());
        assertEquals("自定义错误", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void create_customResultCodeWithData() {
        ResultCode custom = new TestResultCode("10002", "带数据", false);
        Result<Integer> result = ResultUtils.create(custom, 42);
        assertEquals("10002", result.getCode());
        assertEquals("带数据", result.getMsg());
        assertEquals(42, result.getData());
    }

    @Test
    void create_customResultCodeWithMsg() {
        ResultCode custom = new TestResultCode("10003", "原始信息", false);
        Result<Void> result = ResultUtils.create(custom, "覆盖信息");
        assertEquals("10003", result.getCode());
        assertEquals("覆盖信息", result.getMsg());
    }

    @Test
    void create_customResultCodeWithMsgAndData() {
        ResultCode custom = new TestResultCode("10004", "原始信息", true);
        Result<Long> result = ResultUtils.create(custom, "覆盖信息", 123L);
        assertEquals("10004", result.getCode());
        assertEquals("覆盖信息", result.getMsg());
        assertEquals(123L, result.getData());
    }

    // ========== toString ==========

    @Test
    void toString_validJson() {
        Result<String> result = ResultUtils.success("test");
        String str = result.toString();
        assertTrue(str.contains("\"code\":\"00000\""));
        assertTrue(str.contains("\"msg\":\"success\""));
        assertTrue(str.contains("\"data\":\"test\""));
    }

    // ========== helper ==========

    record TestResultCode(String code, String message, boolean success) implements ResultCode {}
}