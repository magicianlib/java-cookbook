package io.ituknown.fesod.write;

import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.metadata.WriteSheet;

import java.io.OutputStream;
import java.util.List;

/**
 * Class 注解模式导出基类。
 * <p>
 * 以行 Bean 的 {@link Class}（字段标注 {@link org.apache.fesod.sheet.annotation.ExcelProperty}）
 * 作为表头来源，逐行写入。子类实现 {@link #headClass()} 返回行 Bean 的 Class 即可，
 * 写入逻辑由本类 {@link #doWrite} 完成。
 *
 * @param <P> 请求对象（payload）类型
 * @param <D> 行 Bean 类型
 * @author magicianlib@gmail.com
 */
public abstract non-sealed class ClassExporter<P, D> extends AbstractExporter<P, D> {

    /** 行 Bean 的 Class（与 {@link #loadData} 的 {@code D} 编译期对齐） */
    public abstract Class<D> headClass();

    @Override
    protected void doWrite(P payload, OutputStream out, ExporterContext ctx) {
        try (ExcelWriter writer = FesodSheet.write(out, headClass())
                .excelType(excelType())
                .build()) {
            WriteSheet sheet = FesodSheet.writerSheet()
                    .sheetName(sheetName())
                    .build();
            while (true) {
                List<D> batch = loadData(payload, ctx);
                if (batch.isEmpty()) {
                    break;                       // 无更多数据 → 结束
                }
                writer.write(batch, sheet);
                ctx.addRows(batch.size());
            }
            writer.finish();
        }
    }
}
