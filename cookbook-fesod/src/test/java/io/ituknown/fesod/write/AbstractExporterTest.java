package io.ituknown.fesod.write;

import io.ituknown.fesod.example.ExampleExcelExporter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractExporterTest {

    @Test
    void write_stream_writesAllRows_multiPage() {
        TestExcelExporter exporter = new TestExcelExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExportResult result = exporter.write("7", out);   // total 7, batch 3 → 3 + 3 + 1

        List<TestRow> rows = readTestRows(out.toByteArray());
        assertEquals(7, rows.size());
        assertEquals("name-0", rows.get(0).getName());
        assertEquals("name-6", rows.get(6).getName());
        assertEquals(7, result.context().getRows(), "返回的 ctx 应累计写入行数");
        assertFalse(result.hasFile(), "流式模式无产物文件");
    }

    @Test
    void write_stream_writesAllRows_exactPages() {
        TestExcelExporter exporter = new TestExcelExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExportResult result = exporter.write("6", out);   // total 6, batch 3 → 3 + 3，整除，末页满页

        List<TestRow> rows = readTestRows(out.toByteArray());
        assertEquals(6, rows.size());
        assertEquals("name-5", rows.get(5).getName());
        assertEquals(6, result.context().getRows());
    }

    @Test
    void write_file_readableAndDeletedAfterClose() throws IOException {
        TestExcelExporter exporter = new TestExcelExporter();

        Path file;
        try (ExportResult result = exporter.write("5")) {     // 文件模式，try-with-resources 自动清理
            file = result.file().orElseThrow(() -> new AssertionError("文件模式应有产物文件"));
            assertTrue(Files.exists(file), "close 前临时文件应存在");
            assertEquals(5, readTestRows(Files.readAllBytes(file)).size(), "文件内应能读出全部行");
            assertEquals(5, result.context().getRows());
        }

        assertFalse(Files.exists(file), "close 后临时文件应已被删除");
    }

    @Test
    void exampleExporter_writesDemoData() {
        // Demo 内置一批示例数据（3 行），写出后应能回读出全部行。
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExampleExcelExporter.INSTANCE.write("demo", out);

        List<ExampleExcelExporter.ExampleRow> rows = FesodSheet.read(new ByteArrayInputStream(out.toByteArray()))
                .head(ExampleExcelExporter.ExampleRow.class)
                .sheet()
                .doReadSync();
        assertEquals(3, rows.size());
    }

    @Test
    void write_stream_emptyData_producesValidFile() {
        // total=0 → loadData 首批即返回空，框架应正常产出文件、不抛异常。
        TestExcelExporter exporter = new TestExcelExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ExportResult result = exporter.write("0", out);

        assertTrue(out.toByteArray().length > 0, "即使无数据也应产出有效的 Excel 文件");
        assertEquals(0, result.context().getRows(), "无数据时行数应为 0");
    }

    @Test
    void write_stream_singleBatch_terminates() {
        // total=3，batch=3 → 一批写完即结束（验证单页终止条件）。
        TestExcelExporter exporter = new TestExcelExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        exporter.write("3", out);

        assertEquals(3, readTestRows(out.toByteArray()).size());
    }

    @Test
    void write_usesOverriddenSheetName() {
        // 子类覆写 sheetName() → 写出的 sheet 应用自定义名（按该名读取应能读到数据）。
        CustomSheetExporter exporter = new CustomSheetExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        exporter.write("2", out);

        List<TestRow> rows = FesodSheet.read(new ByteArrayInputStream(out.toByteArray()))
                .head(TestRow.class)
                .sheet("Custom")
                .doReadSync();
        assertEquals(2, rows.size(), "按覆写的 sheet 名应能读出全部行");
    }

    @Test
    void write_file_propagatesFailure() {
        // doWrite（经 loadData）抛异常时，模式 B 应原样抛出 RuntimeException（失败清理路径被执行）。
        FailingExporter exporter = new FailingExporter();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> exporter.write("x"));

        assertEquals("boom", ex.getMessage(), "运行时异常应原样传播");
    }

    private static List<TestRow> readTestRows(byte[] bytes) {
        return FesodSheet.read(new ByteArrayInputStream(bytes))
                .head(TestRow.class)
                .sheet()
                .doReadSync();
    }

    // ===== 测试夹具 =====

    /** 测试行 Bean */
    public static class TestRow {
        @ExcelProperty("姓名")
        private String name;
        @ExcelProperty("年龄")
        private Integer age;

        public TestRow() {
        }

        public TestRow(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    /** 测试请求对象 */
    public record TestPayload(int total) {
    }

    /** 测试执行器：bizParams 为总条数，每批 3 条，子类内部自管游标推进。 */
    public static class TestExcelExporter extends ClassExporter<TestPayload, TestRow> {

        private int cursor = 0;

        @Override
        public ExportBizType bizType() {
            return ExportBizType.USER_EXPORT;
        }

        @Override
        public TestPayload buildPayload(String bizParams) {
            return new TestPayload(Integer.parseInt(bizParams));
        }

        @Override
        public Class<TestRow> headClass() {
            return TestRow.class;
        }

        @Override
        public List<TestRow> loadData(TestPayload payload, ExporterContext ctx) {
            if (cursor >= payload.total()) {
                return List.of();                 // 无更多数据
            }
            int end = Math.min(cursor + 3, payload.total());
            List<TestRow> rows = new ArrayList<>();
            for (int i = cursor; i < end; i++) {
                rows.add(new TestRow("name-" + i, i));
            }
            cursor = end;
            return rows;
        }
    }

    /** 覆写 sheetName 的夹具，用于验证写出 sheet 名生效。 */
    public static class CustomSheetExporter extends ClassExporter<TestPayload, TestRow> {

        private boolean produced = false;

        @Override
        public ExportBizType bizType() {
            return ExportBizType.USER_EXPORT;
        }

        @Override
        public TestPayload buildPayload(String bizParams) {
            return new TestPayload(Integer.parseInt(bizParams));
        }

        @Override
        public Class<TestRow> headClass() {
            return TestRow.class;
        }

        @Override
        public String sheetName() {
            return "Custom";
        }

        @Override
        public List<TestRow> loadData(TestPayload payload, ExporterContext ctx) {
            if (produced) {
                return List.of();
            }
            produced = true;
            return List.of(new TestRow("a", 1), new TestRow("b", 2));
        }
    }

    /** loadData 抛异常的夹具，用于验证模式 B 失败清理路径。 */
    public static class FailingExporter extends ClassExporter<TestPayload, TestRow> {

        @Override
        public ExportBizType bizType() {
            return ExportBizType.USER_EXPORT;
        }

        @Override
        public TestPayload buildPayload(String bizParams) {
            return new TestPayload(0);
        }

        @Override
        public Class<TestRow> headClass() {
            return TestRow.class;
        }

        @Override
        public List<TestRow> loadData(TestPayload payload, ExporterContext ctx) {
            throw new RuntimeException("boom");
        }
    }
}
