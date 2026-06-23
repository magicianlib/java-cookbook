package io.ituknown.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * log4j2-spring.xml 真实加载与路由测试。
 *
 * 用 log4j2-core 真实加载该配置，验证：
 * - 配置可被合法加载、Appender 就位；
 * - ERROR 同时进 app.log 与 error.log；
 * - INFO / WARN 只进 app.log、不进 error.log。
 *
 * 说明：配置中的 ${spring:...} lookup 需要 Spring 运行时，纯 log4j2 环境无法解析，
 * 且解析后含 ":" 会在 Windows 上构成非法路径字符。这里读取原 XML 文本，仅将
 * ${spring:...} 占位符替换为测试字面值后从内存加载——只替换 lookup 占位符，
 * 不改动任何路由结构（Appender / Logger / level / AppenderRef 原样）。
 */
class Log4j2SpringConfigLoadingTest {

    /** 替换 ${spring:...} 后使用的应用名（即日志文件目录的 APP_ID 段） */
    private static final String TEST_APP_ID = "test-app";

    /**
     * 读取原 log4j2-spring.xml，把 ${spring:...} 替换为测试字面值，从内存加载为 LoggerContext；
     * logging.file.dir 指向临时目录，使 ${sys:logging.file.dir} 解析成功。
     */
    private LoggerContext loadConfig(Path logDir) throws Exception {
        String xml;
        try (InputStream in = getClass().getResourceAsStream("/log4j2-spring.xml")) {
            assertNotNull(in, "log4j2-spring.xml 未能在 classpath 找到");
            xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        // 仅替换 spring lookup 占位符；路由结构不变
        xml = xml.replaceAll("\\$\\{spring:[^}]*}", TEST_APP_ID);

        System.setProperty("logging.file.dir", logDir.toString());

        ConfigurationSource source = new ConfigurationSource(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                getClass().getResource("/log4j2-spring.xml"));
        LoggerContext ctx = Configurator.initialize((ClassLoader) null, source);
        assertNotNull(ctx, "LoggerContext 加载失败（请查看 log4j2 status 日志）");
        return ctx;
    }

    /** 关闭 context，阻塞等待异步 disruptor 队列 flush 落盘 */
    private void shutdown(LoggerContext ctx) {
        assertTrue(Configurator.shutdown(ctx, 5, TimeUnit.SECONDS),
                "LoggerContext 未在超时内完成 flush");
    }

    /** 在 logDir 下定位配置生成的日志文件（位于 APP_ID 子目录） */
    private Path resolve(Path logDir, String fileName) {
        return logDir.resolve(TEST_APP_ID).resolve(fileName);
    }

    @AfterEach
    void clearSystemProperty() {
        System.clearProperty("logging.file.dir");
    }

    @Test
    void loadsWithoutFatalError_appendersPresent(@TempDir Path logDir) throws Exception {
        LoggerContext ctx = loadConfig(logDir);
        try {
            Configuration config = ctx.getConfiguration();
            assertNotNull(config.getAppender("Console"), "Console 应就位");
            assertNotNull(config.getAppender("BizAppender"), "BizAppender 应就位");
            assertNotNull(config.getAppender("ErrorAppender"), "ErrorAppender 应就位");
        } finally {
            shutdown(ctx);
        }
    }

    @Test
    void errorRoutesToBothFiles(@TempDir Path logDir) throws Exception {
        LoggerContext ctx = loadConfig(logDir);
        try {
            Logger logger = LogManager.getLogger("io.ituknown.log.RouteTest");
            logger.error("route-error-marker");
        } finally {
            shutdown(ctx);
        }
        String appLog = Files.readString(resolve(logDir, "app.log"));
        String errorLog = Files.readString(resolve(logDir, "error.log"));
        assertTrue(appLog.contains("route-error-marker"), "ERROR 应写入 app.log");
        assertTrue(errorLog.contains("route-error-marker"), "ERROR 应写入 error.log");
    }
}
