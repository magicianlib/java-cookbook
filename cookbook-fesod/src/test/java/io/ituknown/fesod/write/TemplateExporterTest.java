package io.ituknown.fesod.write;

import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link TemplateExporter}：模板填充调用链路与边界。
 * <p>模板在 {@link BeforeAll} 中用 fesod 程序化生成（含列表占位符 {@code {.name}}/{@code {.amount}}
 * 与单条占位符 {@code {total}}），夹具通过重写 {@link TemplateExporter#openClasspathTemplate}
 * 注入该模板流，避免依赖 classpath 二进制文件。
 *
 * @author magicianlib@gmail.com
 */
class TemplateExporterTest {

    /** 程序生成的模板字节（所有用例共享） */
    private static byte[] templateBytes;

    @BeforeAll
    static void buildTemplate() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ExcelWriter writer = FesodSheet.write(out, TplRow.class).build()) {
            WriteSheet sheet = FesodSheet.writerSheet().sheetName("Sheet1").build();
            writer.write(List.of(
                    new TplRow("{.name}", "{.amount}"),     // 列表占位符行
                    new TplRow("合计", "{total}")),          // 单条占位符行
                    sheet);
            writer.finish();
        }
        templateBytes = out.toByteArray();
    }

    @Test
    void fill_loadsData_callsSummary_propagatesCtxAccumulation() {
        OrderTemplateExporter exporter = new OrderTemplateExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ExportResult result = exporter.write("any", out);

        assertTrue(exporter.summaryCalled, "summary 钩子应被调用");
        assertEquals(new BigDecimal("150"), exporter.summaryReceivedTotal,
                "summary 应读到 loadData 累积到 ctx 的合计值");
        assertEquals(2, result.context().getRows(), "应累计 2 行");
        assertTrue(out.toByteArray().length > 0, "fill 应产出有效文件");
    }

    @Test
    void summaryDefaultEmpty_fillOnlyList_succeeds() {
        // 不重写 summary（默认 empty）→ 仅 fill 列表，不 fill 单条，应正常完成。
        TemplateExporter<OrderQuery, OrderRow> exporter = new TemplateExporter<>() {
            private boolean produced = false;

            @Override public ExportBizType bizType() { return ExportBizType.ORDER_EXPORT; }
            @Override public OrderQuery buildPayload(String bizParams) { return new OrderQuery(); }
            @Override public String headTemplate() { return "injected"; }
            @Override public InputStream openClasspathTemplate(String path) {
                return new ByteArrayInputStream(templateBytes);
            }
            @Override public List<OrderRow> loadData(OrderQuery payload, ExporterContext ctx) {
                if (produced) return List.of();
                produced = true;
                return List.of(new OrderRow("甲", new BigDecimal("1")));
            }
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ExportResult result = exporter.write("any", out);

        assertEquals(1, result.context().getRows());
        assertTrue(out.toByteArray().length > 0);
    }

    @Test
    void headTemplate_pathMissing_throwsIllegalState() {
        // 用默认 openClasspathTemplate（读 classpath），路径不存在应抛 IllegalStateException。
        TemplateExporter<OrderQuery, OrderRow> exporter = new TemplateExporter<>() {
            @Override public ExportBizType bizType() { return ExportBizType.ORDER_EXPORT; }
            @Override public OrderQuery buildPayload(String bizParams) { return new OrderQuery(); }
            @Override public String headTemplate() { return "not/exist/template.xlsx"; }
            @Override public List<OrderRow> loadData(OrderQuery payload, ExporterContext ctx) { return List.of(); }
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThrows(IllegalStateException.class, () -> exporter.write("x", out));
    }

    // ===== 夹具 =====

    /** 模板行：两列，值为占位符文本 */
    public static class TplRow {
        @ExcelProperty("c1")
        private String c1;
        @ExcelProperty("c2")
        private String c2;

        TplRow() {
        }

        TplRow(String c1, String c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        public String getC1() { return c1; }
        public void setC1(String c1) { this.c1 = c1; }
        public String getC2() { return c2; }
        public void setC2(String c2) { this.c2 = c2; }
    }

    /** 请求对象（demo 无字段） */
    public record OrderQuery() {
    }

    /** 列表数据行（字段名匹配模板占位符 {.name}/{.amount}） */
    public static class OrderRow {
        private String name;
        private BigDecimal amount;

        OrderRow(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() { return name; }
        public BigDecimal getAmount() { return amount; }
    }

    /** 单条汇总（字段名匹配占位符 {total}） */
    public record OrderSummary(BigDecimal total) {
    }

    /** 演示完整链路的模板导出器：注入模板、loadData 累积合计、summary 读取 */
    public static class OrderTemplateExporter extends TemplateExporter<OrderQuery, OrderRow> {

        boolean summaryCalled = false;
        BigDecimal summaryReceivedTotal;

        private boolean produced = false;

        @Override
        public ExportBizType bizType() {
            return ExportBizType.ORDER_EXPORT;
        }

        @Override
        public OrderQuery buildPayload(String bizParams) {
            return new OrderQuery();
        }

        @Override
        public String headTemplate() {
            return "injected";   // 路径无意义：下方 override 了模板读取
        }

        @Override
        public InputStream openClasspathTemplate(String path) {
            return new ByteArrayInputStream(templateBytes);   // 注入生成的模板
        }

        @Override
        public List<OrderRow> loadData(OrderQuery payload, ExporterContext ctx) {
            if (produced) {
                return List.of();
            }
            produced = true;
            // 累积金额合计，供 summary 经 ctx 读取
            ctx.accumulate("totalAmount", new BigDecimal("150"));
            return List.of(
                    new OrderRow("张三", new BigDecimal("100")),
                    new OrderRow("李四", new BigDecimal("50")));
        }

        @Override
        protected Optional<Object> summary(OrderQuery payload, ExporterContext ctx) {
            summaryCalled = true;
            summaryReceivedTotal = ctx.get("totalAmount", BigDecimal.class, BigDecimal.ZERO);
            return Optional.of(new OrderSummary(summaryReceivedTotal));
        }
    }
}
