package io.ituknown.fesod.write;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link ExportResult}：流式/文件两种构造、{@link AutoCloseable#close()} 行为。
 * <p>不依赖 fesod，纯单元测试。
 *
 * @author magicianlib@gmail.com
 */
class ExportResultTest {

    @Test
    void ofStream_hasNoFile_contextExposed_closeIsNoop() {
        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);
        ExportResult result = ExportResult.ofStream(ctx);

        assertFalse(result.hasFile(), "流式模式无产物文件");
        assertTrue(result.file().isEmpty(), "file() 应为 empty");
        assertSame(ctx, result.context(), "context() 应原样返回");
        assertDoesNotThrow(result::close, "流式 close 应为空操作，不抛异常");
    }

    @Test
    void ofFile_hasFile_closeDeletesIt(@TempDir Path dir) throws IOException {
        Path file = Files.createFile(dir.resolve("out.xlsx"));
        Files.writeString(file, "payload");
        assertTrue(Files.exists(file), "前置：文件应存在");

        ExporterContext ctx = ExporterContext.start(ExportBizType.USER_EXPORT);
        ExportResult result = ExportResult.ofFile(ctx, file);

        assertTrue(result.hasFile(), "文件模式应有产物文件");
        assertEquals(file, result.file().orElseThrow(), "file() 应返回构造传入的路径");

        result.close();

        assertFalse(Files.exists(file), "close 后临时文件应被删除");
    }

    @Test
    void close_isIdempotent(@TempDir Path dir) throws IOException {
        Path file = Files.createFile(dir.resolve("out.xlsx"));
        ExportResult result = ExportResult.ofFile(
                ExporterContext.start(ExportBizType.USER_EXPORT), file);

        result.close();

        assertDoesNotThrow(result::close, "重复 close 不应抛异常（deleteIfExists 幂等）");
    }
}
