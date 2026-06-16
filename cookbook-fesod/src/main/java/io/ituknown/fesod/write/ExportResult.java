package io.ituknown.fesod.write;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 导出结果，统一承载<b>过程上下文</b>与<b>产物文件</b>。
 * <p>
 * {@link AbstractExporter#write} 的返回类型，让调用方一次拿到导出统计（行数/耗时，以及
 * {@link ExporterContext#accumulate} 累积的业务数据）和（文件模式下的）产物文件。
 * <ul>
 *   <li><b>流式模式</b>（{@code write(String, OutputStream)}）：写入调用方的流，{@link #file()} 为 empty；</li>
 *   <li><b>文件模式</b>（{@code write(String)}）：框架生成临时文件，{@link #file()} 有值。</li>
 * </ul>
 * <p>
 * 文件模式下，本对象<b>拥有</b>临时文件的生命周期：实现 {@link AutoCloseable}，
 * 调用方用 {@code try-with-resources} 即可在用完后自动删除，避免临时文件泄漏。
 * 流式模式下 {@link #close()} 为空操作（无文件可删）。
 *
 * @author magicianlib@gmail.com
 */
public final class ExportResult implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportResult.class);

    private final ExporterContext context;

    /** 产物文件；{@code null} 表示流式模式无文件 */
    private final Path file;

    ExportResult(ExporterContext context, Path file) {
        this.context = context;
        this.file = file;
    }

    /** 流式构造：无产物文件 */
    static ExportResult ofStream(ExporterContext context) {
        return new ExportResult(context, null);
    }

    /** 文件构造：带产物文件（所有权转交本对象，{@link #close()} 时删除） */
    static ExportResult ofFile(ExporterContext context, Path file) {
        return new ExportResult(context, file);
    }

    /** 过程上下文：统计（行数/耗时）+ accumulate 累积的业务数据 */
    public ExporterContext context() {
        return context;
    }

    /** 流式模式返回 empty；文件模式返回框架生成的临时文件 */
    public Optional<Path> file() {
        return Optional.ofNullable(file);
    }

    /** 是否为文件模式（即 {@link #file()} 有值） */
    public boolean hasFile() {
        return file != null;
    }

    @Override
    public void close() {
        if (file == null) {
            return;                            // 流式模式：无文件可删
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warn("删除临时文件失败: {}", file, e);   // 不掩盖业务异常
        }
    }
}
