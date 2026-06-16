package io.ituknown.fesod.write;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 {@link ExporterContext} 的过程属性容器（accumulate/get）。
 * <p>不依赖 fesod，纯单元测试。
 *
 * @author magicianlib@gmail.com
 */
class ExporterContextTest {

    @Test
    void accumulate_thenGet_returnsValue() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);

        ctx.accumulate("totalAmount", new BigDecimal("12.50"));

        assertEquals(new BigDecimal("12.50"),
                ctx.get("totalAmount", BigDecimal.class, BigDecimal.ZERO));
    }

    @Test
    void get_missingKey_returnsDefault() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);

        assertEquals(0, ctx.get("absent", Integer.class, 0));
    }

    @Test
    void accumulate_isChainable_overwritesSameKey() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);

        ctx.accumulate("count", 1)
           .accumulate("count", 2);

        assertEquals(2, ctx.get("count", Integer.class, 0));
    }

    @Test
    void get_typeMismatch_throwsClassCast() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);
        ctx.accumulate("k", "not-a-number");

        assertThrows(ClassCastException.class, () -> ctx.get("k", Integer.class, 0));
    }

    @Test
    void start_setsBizType() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.ORDER_EXPORT);

        assertEquals(ExportBizType.ORDER_EXPORT, ctx.getBizType());
    }

    @Test
    void rows_startsAtZero_thenAccumulatesByAddRows() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);

        assertEquals(0, ctx.getRows(), "初始行数应为 0");
        ctx.addRows(3);
        ctx.addRows(2);
        ctx.addRows(0);                     // 0 不影响
        assertEquals(5, ctx.getRows(), "多次 addRows 应累加");
    }

    @Test
    void finish_computesElapsedMs() throws InterruptedException {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);

        assertEquals(0, ctx.getElapsedMs(), "finish 前耗时字段应为 0");
        Thread.sleep(2L);                    // 制造可测的时间差
        ctx.finish();

        assertTrue(ctx.getElapsedMs() >= 1, "finish 后耗时应被计算且 >= 1ms");
    }

    @Test
    void accumulate_customObjectType_roundTrips() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);
        Summary summary = new Summary(100, "ok");

        ctx.accumulate("summary", summary);

        Summary got = ctx.get("summary", Summary.class, null);
        assertSame(summary, got, "应原样返回同一对象");
        assertEquals(100, got.count());
    }

    @Test
    void accumulate_multipleKeysCoexist() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);

        ctx.accumulate("a", 1)
           .accumulate("b", 2);

        assertEquals(1, ctx.get("a", Integer.class, 0));
        assertEquals(2, ctx.get("b", Integer.class, 0));
    }

    /** 自定义汇总对象（演示 accumulate 自定义类型） */
    private record Summary(int count, String status) {
    }
}
