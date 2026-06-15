package io.ituknown.fesod.write;

import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

/**
 * 模板填充模式导出基类。
 * <p>
 * 以 classpath 上一份带占位符的 {@code .xlsx} 模板作为表头/样式来源，通过 fesod 的 {@code fill}
 * 把数据填入模板占位符：
 * <ul>
 *   <li>列表占位符（如 {@code {.name}}）—— 由 {@link #loadData} 分批返回的 {@code List} 填充；</li>
 *   <li>单条/汇总占位符（如 {@code {total}}）—— 由 {@link #summary} 返回的对象填充（可选）。</li>
 * </ul>
 *
 * <h3>分批 fill 的两条潜规则（已在本类处理）</h3>
 * <ol>
 *   <li><b>必须 {@code forceNewRow(true)}</b>：否则首批 fill 会把列表占位符冲掉，
 *       后续批次找不到占位符，会从模板顶部覆盖首批数据。本类 fill 列表时一律带
 *       {@code FillConfig.builder().forceNewRow(true).build()}。</li>
 *   <li><b>列表数据必须先于单条数据 fill</b>：一旦 fill 了单条/汇总数据，模板占位符解析进入收尾，
 *       后续再分批 fill 列表会定位错乱。本类先循环 fill 完所有列表批次，最后才 fill
 *       {@link #summary}。</li>
 * </ol>
 *
 * @param <P> 请求对象（payload）类型
 * @param <D> 列表数据行类型（行 Bean 或填充对象）
 * @author magicianlib@gmail.com
 */
public abstract non-sealed class TemplateExporter<P, D> extends AbstractExporter<P, D> {

    /** 列表 fill 固定开启 forceNewRow，保证多批不覆盖、模板下方内容持续下推 */
    private static final FillConfig FORCE_NEW_ROW = FillConfig.builder().forceNewRow(true).build();

    /** 模板文件 classpath 路径，如 {@code "templates/order.xlsx"} */
    public abstract String headTemplate();

    /**
     * 单条/汇总数据（模板中非列表占位符，如 {@code {total}}、{@code {signer}}）。
     * <p>默认空表示无单条数据。返回的对象由 fesod 反射读字段填入对应占位符。
     * <p><b>顺序</b>：本类在所有列表批次 fill 完成后才 fill 此 summary，避免列表/单条 fill 顺序错乱。
     * <p>{@code ctx} 承载 {@link #loadData} 过程中累积的中间结果（经 {@link ExporterContext#accumulate} 写入），
     * 本方法可经 {@link ExporterContext#get} 读出用于汇总计算。
     *
     * @param payload 由 {@link #buildPayload} 转换而来的请求对象
     * @param ctx     本次导出的过程上下文
     */
    protected Optional<Object> summary(P payload, ExporterContext ctx) {
        return Optional.empty();
    }

    @Override
    protected void doWrite(P payload, OutputStream out, ExporterContext ctx) {
        String path = headTemplate();
        try (InputStream template = openClasspathTemplate(path);
             ExcelWriter writer = FesodSheet.write(out)
                     .withTemplate(template)
                     .excelType(excelType())
                     .build()) {
            WriteSheet sheet = FesodSheet.writerSheet()
                    .sheetName(sheetName())
                    .build();

            // 规则 2：先 fill 完所有列表批次；loadData 可在此向 ctx 累积合计等
            while (true) {
                List<D> batch = loadData(payload, ctx);
                if (batch.isEmpty()) {
                    break;                       // 无更多数据 → 结束
                }
                writer.fill(batch, FORCE_NEW_ROW, sheet);   // 规则 1：forceNewRow
                ctx.addRows(batch.size());
            }

            // 规则 2：列表 fill 完后，最后 fill 单条/汇总数据；summary 从 ctx 读中间结果
            Optional<Object> summary = summary(payload, ctx);
            if (summary.isPresent()) {
                writer.fill(summary.get(), sheet);
            }

            writer.finish();
        } catch (IOException e) {
            throw new IllegalStateException("模板填充失败：" + path, e);
        }
    }

    /** 从 classpath 读取模板流，路径不存在时抛 {@link IllegalStateException} */
    protected InputStream openClasspathTemplate(String path) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("classpath 模板不存在：" + path);
        }
        return in;
    }
}
