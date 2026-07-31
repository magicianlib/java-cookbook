package io.ituknown.payload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 双向游标分页测试
 */
@DisplayName("双向游标分页测试")
class BidirectionalCursorPaginationTest {

    @Nested
    @DisplayName("向前翻页测试")
    class ForwardPaginationTests {

        @Test
        @DisplayName("首次请求：有更多数据")
        void testFirstPageWithMoreData() {
            // 准备数据：11 条（pageSize=10，多 1 条用于判断是否有下一页）
            List<Integer> data = IntStream.rangeClosed(1, 11).boxed().toList();

            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity(), CursorDirection.FORWARD, null
            );

            assertEquals(10, page.list().size(), "应返回 10 条数据");
            assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), page.list(), "数据顺序应正确");
            assertTrue(page.pagination().hasNext(), "应有下一页");
            assertEquals(10, page.pagination().nextCursor(), "下一页游标应为第 10 条数据的 ID");
            assertNull(page.pagination().prevCursor(), "首次请求无上一页游标");
            assertFalse(page.pagination().hasPrev(), "首次请求无上一页");
            assertEquals(10, page.pagination().pageSize(), "每页数量应为 10");
        }

        @Test
        @DisplayName("首次请求：无更多数据")
        void testFirstPageWithoutMoreData() {
            // 准备数据：5 条（少于 pageSize=10）
            List<Integer> data = IntStream.rangeClosed(1, 5).boxed().toList();

            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity(), CursorDirection.FORWARD, null
            );

            assertEquals(5, page.list().size(), "应返回所有 5 条数据");
            assertEquals(List.of(1, 2, 3, 4, 5), page.list(), "数据顺序应正确");
            assertFalse(page.pagination().hasNext(), "应无下一页");
            assertNull(page.pagination().nextCursor(), "无下一页游标");
            assertNull(page.pagination().prevCursor(), "首次请求无上一页游标");
            assertFalse(page.pagination().hasPrev(), "首次请求无上一页");
            assertEquals(10, page.pagination().pageSize(), "每页数量应为 10");
        }

        @Test
        @DisplayName("后续翻页：有更多数据")
        void testNextPageWithMoreData() {
            // 准备数据：从游标 11 开始的 11 条数据
            // SQL: WHERE id > 10 ORDER BY id ASC LIMIT 11
            List<Integer> data = IntStream.rangeClosed(11, 21).boxed().toList();

            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity(), CursorDirection.FORWARD, 10
            );

            assertEquals(10, page.list().size(), "应返回 10 条数据");
            assertEquals(List.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 20), page.list(), "数据顺序应正确");
            assertTrue(page.pagination().hasNext(), "应有下一页");
            assertTrue(page.pagination().hasPrev(), "应有上一页");
            assertEquals(20, page.pagination().nextCursor(), "下一页游标应为第 20 条数据的 ID");
            assertEquals(10, page.pagination().prevCursor(), "上一页游标应为请求的游标");
            assertEquals(10, page.pagination().pageSize(), "每页数量应为 10");
        }

        @Test
        @DisplayName("后续翻页：无更多数据")
        void testNextPageWithoutMoreData() {
            // 准备数据：从游标 91 开始的 5 条数据（最后一页）
            // SQL: WHERE id > 90 ORDER BY id ASC LIMIT 6 (实际只返回5条)
            List<Integer> data = IntStream.rangeClosed(91, 95).boxed().toList();

            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity(), CursorDirection.FORWARD, 90
            );

            assertEquals(5, page.list().size(), "应返回所有 5 条数据");
            assertEquals(List.of(91, 92, 93, 94, 95), page.list(), "数据顺序应正确");
            assertFalse(page.pagination().hasNext(), "应无下一页");
            assertTrue(page.pagination().hasPrev(), "应有上一页");
            assertNull(page.pagination().nextCursor(), "无下一页游标");
            assertEquals(90, page.pagination().prevCursor(), "上一页游标应为请求的游标");
            assertEquals(10, page.pagination().pageSize(), "每页数量应为 10");
        }
    }

    @Nested
    @DisplayName("向后翻页测试")
    class BackwardPaginationTests {

        @Test
        @DisplayName("有前一页：数据顺序正确")
        void testPrevPageWithData() {
            // 准备数据：数据库向前查询返回 11 条（多 1 条用于判断是否有上一页）
            // SQL: WHERE id < 21 ORDER BY id DESC LIMIT 11
            // 数据是倒序的：20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10
            List<Integer> data = new ArrayList<>(IntStream.rangeClosed(10, 20).boxed().toList());
            java.util.Collections.reverse(data);

            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity(), CursorDirection.BACKWARD, 21
            );

            assertEquals(10, page.list().size(), "应返回 10 条数据");
            // 数据应该被反转回正确顺序：[10, 11, 12, 13, 14, 15, 16, 17, 18, 19]
            assertEquals(List.of(10, 11, 12, 13, 14, 15, 16, 17, 18, 19), page.list(), "数据顺序应正确");
            assertTrue(page.pagination().hasPrev(), "应有上一页");
            assertTrue(page.pagination().hasNext(), "应有下一页");
            assertEquals(21, page.pagination().nextCursor(), "下一页游标应为请求的游标");
            assertEquals(10, page.pagination().prevCursor(), "上一页游标应为第 10 条数据的 ID");
            assertEquals(10, page.pagination().pageSize(), "每页数量应为 10");
        }

        @Test
        @DisplayName("无前一页：数据顺序正确")
        void testPrevPageWithoutData() {
            // 准备数据：数据库向前查询返回 5 条（少于 pageSize=10，说明是第一页）
            // SQL: WHERE id < 6 ORDER BY id DESC LIMIT 11 (实际只返回5条)
            // 数据是倒序的：5, 4, 3, 2, 1
            List<Integer> data = new ArrayList<>(IntStream.rangeClosed(1, 5).boxed().toList());
            java.util.Collections.reverse(data);

            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity(), CursorDirection.BACKWARD, 6
            );

            assertEquals(5, page.list().size(), "应返回所有 5 条数据");
            // 数据应该被反转回正确顺序：1, 2, 3, 4, 5
            assertEquals(List.of(1, 2, 3, 4, 5), page.list(), "数据顺序应正确");
            assertFalse(page.pagination().hasPrev(), "应无上一页");
            assertTrue(page.pagination().hasNext(), "应有下一页");
            assertNull(page.pagination().prevCursor(), "无上一页游标");
            assertEquals(6, page.pagination().nextCursor(), "下一页游标应为请求的游标");
            assertEquals(10, page.pagination().pageSize(), "每页数量应为 10");
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空列表处理")
        void testEmptyList() {
            List<Integer> emptyList = List.of();

            CursorPage<Integer, Integer> page = CursorPage.of(
                emptyList, 10, Function.identity(), CursorDirection.FORWARD, null
            );

            assertEquals(0, page.list().size(), "应返回空列表");
            assertFalse(page.pagination().hasNext(), "应无下一页");
            assertFalse(page.pagination().hasPrev(), "应无上一页");
            assertNull(page.pagination().nextCursor(), "无下一页游标");
            assertNull(page.pagination().prevCursor(), "无上一页游标");
            assertEquals(10, page.pagination().pageSize(), "每页数量应为 10");
        }

        @Test
        @DisplayName("null 列表处理")
        void testNullList() {
            CursorPage<Integer, Integer> page = CursorPage.of(
                null, 10, Function.identity(), CursorDirection.FORWARD, null
            );

            assertEquals(0, page.list().size(), "应返回空列表");
            assertFalse(page.pagination().hasNext(), "应无下一页");
            assertFalse(page.pagination().hasPrev(), "应无上一页");
            assertNull(page.pagination().nextCursor(), "无下一页游标");
            assertNull(page.pagination().prevCursor(), "无上一页游标");
            assertEquals(10, page.pagination().pageSize(), "每页数量应为 10");
        }

        @Test
        @DisplayName("正好等于 pageSize 的列表")
        void testExactPageSize() {
            List<Integer> data = IntStream.rangeClosed(1, 10).boxed().toList();

            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity(), CursorDirection.FORWARD, null
            );

            assertEquals(10, page.list().size(), "应返回所有 10 条数据");
            assertFalse(page.pagination().hasNext(), "应无下一页");
            assertFalse(page.pagination().hasPrev(), "应无上一页");
            assertNull(page.pagination().nextCursor(), "无下一页游标");
            assertNull(page.pagination().prevCursor(), "首次请求无上一页游标");
        }
    }

    @Nested
    @DisplayName("向后兼容性测试")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("旧版 of 方法仍然可用")
        void testLegacyOfMethod() {
            List<Integer> data = IntStream.rangeClosed(1, 11).boxed().toList();

            // 调用旧版方法（无 direction 和 cursor 参数）
            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity()
            );

            assertEquals(10, page.list().size(), "应返回 10 条数据");
            assertTrue(page.pagination().hasNext(), "应有下一页");
            assertFalse(page.pagination().hasPrev(), "应无上一页");
            assertEquals(10, page.pagination().nextCursor(), "下一页游标应为第 10 条数据的 ID");
            assertNull(page.pagination().prevCursor(), "首次请求无上一页游标");
        }

        @Test
        @DisplayName("新版方法默认 FORWARD 行为")
        void testNewMethodDefaultForward() {
            List<Integer> data = IntStream.rangeClosed(1, 11).boxed().toList();

            CursorPage<Integer, Integer> page = CursorPage.of(
                data, 10, Function.identity(), CursorDirection.FORWARD, null
            );

            assertEquals(10, page.list().size(), "应返回 10 条数据");
            assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), page.list(), "数据顺序应正确");
            assertTrue(page.pagination().hasNext(), "应有下一页");
            assertFalse(page.pagination().hasPrev(), "应无上一页");
            assertEquals(10, page.pagination().nextCursor(), "下一页游标应为第 10 条数据的 ID");
            assertNull(page.pagination().prevCursor(), "首次请求无上一页游标");
        }
    }
}