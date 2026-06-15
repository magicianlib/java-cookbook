package io.ituknown.fesod.write;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
