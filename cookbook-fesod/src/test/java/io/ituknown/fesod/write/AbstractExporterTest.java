package io.ituknown.fesod.write;

import io.ituknown.fesod.example.ExampleExcelExporter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractExporterTest {

    @Test
    void write_stream_writesAllRows_multiPage() {
        TestExcelExporter exporter = new TestExcelExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.write("7", out);   // total 7, batch 3 → 3 + 3 + 1

        List<TestRow> rows = readTestRows(out.toByteArray());
        assertEquals(7, rows.size());
        assertEquals("name-0", rows.get(0).getName());
        assertEquals("name-6", rows.get(6).getName());
    }

    @Test
    void write_stream_writesAllRows_exactPages() {
        TestExcelExporter exporter = new TestExcelExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.write("6", out);   // total 6, batch 3 → 3 + 3，整除，末页满页

        List<TestRow> rows = readTestRows(out.toByteArray());
        assertEquals(6, rows.size());
        assertEquals("name-5", rows.get(5).getName());
    }

    @Test
    void write_tempFile_readableInCallbackAndDeletedAfter() {
        TestExcelExporter exporter = new TestExcelExporter();
        AtomicReference<Path> pathRef = new AtomicReference<>();

        exporter.write("5", path -> {
            pathRef.set(path);
            assertTrue(Files.exists(path), "回调内临时文件应存在");
            try {
                assertEquals(5, readTestRows(Files.readAllBytes(path)).size(), "回调内应能读出全部行");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertFalse(Files.exists(pathRef.get()), "回调返回后临时文件应已被删除");
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
}
