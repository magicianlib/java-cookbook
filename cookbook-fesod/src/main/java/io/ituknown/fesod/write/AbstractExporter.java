package io.ituknown.fesod.write;

import org.apache.fesod.sheet.support.ExcelTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 同步导出抽象基类（公共骨架）。
 * <p>
 * 定义子类公共契约，并提供统一的写入调度（{@link #write(String, OutputStream)} /
 * {@link #write(String)}），二者均返回 {@link ExportResult}。具体写入方式（Class 注解逐行写入、
 * 模板填充）由模式基类 {@link ClassExporter} / {@link TemplateExporter} 通过重写
 * {@link #doWrite} 多态实现。
 * <p>
 * 本类<b>不负责"按业务类型定位子类"</b>——定位方式由外部决定；
 * 外部可通过 {@link #bizType()} 读取子类声明的业务类型来构建自己的映射。
 * <p>
 * 本类为 {@code sealed}，<b>只允许</b> {@link ClassExporter} / {@link TemplateExporter} 继承——
 * 业务实现必须从这两个模式基类入手，不能直接继承本类。两个模式基类为 {@code non-sealed}，
 * 可被业务任意继承。
 * <p>
 * <b>子类需实现：</b>
 * <ul>
 *   <li>{@link #bizType()} —— 声明业务类型（供外部定位）；</li>
 *   <li>{@link #buildPayload(String)} —— 将外部字符串参数转换为请求对象 P；</li>
 *   <li>{@link #loadData(Object, ExporterContext)} —— 加载一批数据，返回空 {@code List} 表示无更多数据。
 *       分页与游标推进完全由子类内部自行处理；{@code ctx} 供累积中间结果；</li>
 *   <li>{@link #doWrite(Object, OutputStream, ExporterContext)} —— 具体写入方式（通常由模式基类实现，
 *       业务子类无需直接重写）。</li>
 * </ul>
 *
 * @param <P> 请求对象（payload）类型
 * @param <D> 行 Bean 类型（字段标注 {@link org.apache.fesod.sheet.annotation.ExcelProperty}）
 * @author magicianlib@gmail.com
 */
public abstract sealed class AbstractExporter<P, D>
        permits ClassExporter, TemplateExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExporter.class);

    /**
     * 业务类型，供外部定位子类
     */
    public abstract ExportBizType bizType();

    /**
     * 将外部传入的 bizParams 字符串转换为请求对象 P
     */
    protected abstract P buildPayload(String bizParams);

    /**
     * 加载一批数据。
     * <p>返回空 {@code List} 表示无更多数据，框架据此结束写入循环。
     * 分页/游标推进由子类内部实现。
     * <p>{@code ctx} 为贯穿全程的过程上下文：子类可在此调用 {@link ExporterContext#accumulate}
     * 累积中间结果（如金额合计），供 {@link TemplateExporter#summary} 等后续钩子经
     * {@link ExporterContext#get} 读取。
     *
     * @param payload 由 {@link #buildPayload} 转换而来的请求对象
     * @param ctx     本次导出的过程上下文
     */
    protected abstract List<D> loadData(P payload, ExporterContext ctx);

    /**
     * 具体写入方式。由模式基类（{@link ClassExporter} / {@link TemplateExporter}）多态实现，
     * 业务子类一般无需直接重写。
     *
     * @param payload 由 {@link #buildPayload} 转换而来的请求对象
     * @param out     目标输出流
     * @param ctx     本次导出统计上下文
     */
    protected abstract void doWrite(P payload, OutputStream out, ExporterContext ctx);

    /**
     * 默认 sheet 名
     */
    protected String sheetName() {
        return "Sheet1";
    }

    /**
     * 默认 Excel 类型
     */
    protected ExcelTypeEnum excelType() {
        return ExcelTypeEnum.XLSX;
    }

    /**
     * 模式 A：写入调用方传入的 OutputStream。
     * <p>{@code final} —— 分发逻辑由框架控制，子类通过 {@link #doWrite} 注入具体写法。
     * <p>无产物文件，返回的 {@link ExportResult#file()} 为 empty；但仍带 {@link ExporterContext}
     * （统计 + 累积的业务数据）。
     *
     * @return 导出结果，含过程上下文（无文件）
     */
    public final ExportResult write(String bizParams, OutputStream out) {
        P payload = buildPayload(bizParams);
        ExporterContext ctx = ExporterContext.start(bizType());
        doWrite(payload, out, ctx);
        ctx.finish();
        return ExportResult.ofStream(ctx);
    }

    /**
     * 模式 B：框架生成临时文件并写入，返回 {@link ExportResult}（含产物文件 + 过程上下文）。
     * <p>{@code final} —— 复用 {@link #write(String, OutputStream)}，分发自动生效。
     * <p><b>文件所有权</b>转交给返回的 {@link ExportResult}：调用方应在用完文件后
     * {@link ExportResult#close()}（推荐 {@code try-with-resources}）以删除临时文件、避免泄漏；
     * 写入失败时由本方法立即清理临时文件。{@code deleteOnExit} 兜底应对 JVM 异常退出。
     *
     * @return 导出结果，含产物文件与过程上下文
     */
    public final ExportResult write(String bizParams) {
        Path temp;
        try {
            temp = Files.createTempFile("fesod-write-", "." + excelType().getValue());
        } catch (IOException e) {
            throw new IllegalStateException("创建临时文件失败", e);
        }
        temp.toFile().deleteOnExit();            // JVM 退出兜底（应对 Windows 文件锁）

        try (OutputStream out = Files.newOutputStream(temp)) {
            ExportResult result = write(bizParams, out);    // 复用流式写入
            out.flush();
            return ExportResult.ofFile(result.context(), temp);   // 成功：所有权转交 ExportResult
        } catch (Exception e) {                              // IO 或运行时异常均清理
            deleteTempFile(temp);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("写入临时文件失败", e);
        }
    }

    /**
     * 静默删除临时文件，失败仅告警、不抛异常
     */
    private static void deleteTempFile(Path temp) {
        try {
            Files.deleteIfExists(temp);
        } catch (IOException e) {
            LOGGER.warn("删除临时文件失败: {}", temp, e);
        }
    }
}