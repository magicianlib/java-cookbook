package io.ituknown.fesod.example;

import io.ituknown.fesod.write.ClassExporter;
import io.ituknown.fesod.write.ExporterContext;
import io.ituknown.fesod.write.ExportBizType;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.util.List;

/**
 * Excel 导出示例（Demo）—— Class 注解模式。
 * <p>
 * 演示如何继承 {@link ClassExporter}：声明业务类型、定义请求对象与行 Bean、
 * 实现 {@link #buildPayload}、{@link #headClass} 与 {@link #loadData}。
 * <p>
 * Demo 内置一份小数据源（{@link #loadData} 仅返回一批即停止），便于直接跑出非空 Excel。
 * 实际使用时，在 {@link #loadData} 中从数据源（数据库、远程接口等）加载一批数据，
 * 返回空 {@code List} 表示无更多数据；分页/游标推进由本子类自行处理。
 * <p>
 * 本类位于 {@code example} 包，与 {@code io.ituknown.fesod.write} 框架核心分离，
 * 仅作演示；实际业务的导出器应放在业务自身的包中。
 *
 * @author magicianlib@gmail.com
 */
public class ExampleExcelExporter extends ClassExporter<ExampleExcelExporter.ExamplePayload, ExampleExcelExporter.ExampleRow> {

    /** 单例：外部可通过 INSTANCE 引用本执行器 */
    public static final ExampleExcelExporter INSTANCE = new ExampleExcelExporter();

    /** demo 内部游标，演示"返回一批后再返回空表示结束" */
    private boolean produced = false;

    @Override
    public ExportBizType bizType() {
        return ExportBizType.USER_EXPORT;
    }

    @Override
    public ExamplePayload buildPayload(String bizParams) {
        // Demo：原样保留参数；实际使用时按需解析（如 JSON 反序列化为查询条件）
        return new ExamplePayload(bizParams);
    }

    @Override
    public Class<ExampleRow> headClass() {
        return ExampleRow.class;
    }

    @Override
    public List<ExampleRow> loadData(ExamplePayload payload, ExporterContext ctx) {
        // Demo 小数据源：返回一批示例数据，再次调用即返回空（表示无更多数据）。
        // 实际使用时在此从数据源分批加载，并在本子类内部推进游标/分页。
        // （本示例不使用 ctx；模板模式子类可在 loadData 中 ctx.accumulate 累积汇总供 summary 读取）
        if (produced) {
            return List.of();
        }
        produced = true;
        return List.of(
                row("张三", 28),
                row("李四", 35),
                row("王五", 42));
    }

    private static ExampleRow row(String name, int age) {
        ExampleRow row = new ExampleRow();
        row.setName(name);
        row.setAge(age);
        return row;
    }

    /** 示例请求对象 */
    public record ExamplePayload(String raw) {
    }

    /** 示例行 Bean */
    public static class ExampleRow {
        @ExcelProperty("姓名")
        private String name;
        @ExcelProperty("年龄")
        private Integer age;

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
}
