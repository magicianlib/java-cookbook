# log4j2-spring.xml 测试用例 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 cookbook-log 模块的 `log4j2-spring.xml` 补充两类测试——结构校验(DOM)与真实加载路由验证(log4j2-core),作为配置回归保护。

**Architecture:** 结构校验用 JDK 内置 DOM 解析 XML 断言 Appender/Logger/Property/引用完整性;加载测试用 log4j2-core 真实加载该 XML(读取原文件文本、把 `${spring:...}` 占位符替换为测试字面值后从内存加载,避开纯 log4j2 无 spring lookup 在 Windows 上产生的非法路径),触发不同级别日志后用带超时的 `Configurator.shutdown` flush 异步队列,再读 `app.log`/`error.log` 断言路由。

**Tech Stack:** Java 21、JUnit 5.12.2(已有)、JDK 内置 `javax.xml`(DOM)、Apache Log4j2 Core(版本由 spring-boot-dependencies 3.5.9 BOM 管理)、Maven。

**Spec:** `docs/superpowers/specs/2026-06-23-log4j2-spring-config-tests-design.md`

---

## File Structure

| 文件 | 动作 | 职责 |
|---|---|---|
| `cookbook-log/pom.xml` | 修改 | 新增 `log4j-core`(test scope) |
| `cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigStructureTest.java` | 新建 | DOM 结构校验 |
| `cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigLoadingTest.java` | 新建 | log4j2 真实加载 + 路由验证 |

**运行命令约定:** 项目无 mvnw,从仓库根目录 `E:\JetBrains\java-cookbook` 运行 Maven,用 `-pl cookbook-log` 限定模块。

---

## Task 1: 引入 log4j-core 测试依赖

**Files:**
- Modify: `cookbook-log/pom.xml`(在现有 test 依赖块内追加)

- [ ] **Step 1: 在 `<dependencies>` 的 test 区追加 log4j-core**

在 `cookbook-log/pom.xml` 中,找到现有的 test 依赖块(以 `<!-- Test -->` 注释开头、含 `junit-jupiter` 与 `logback-classic`),在 `logback-classic` 依赖**之后**、`</dependencies>` **之前**插入:

```xml
        <!-- log4j2-spring.xml 加载/路由测试：版本由 spring-boot-dependencies BOM 管理 -->
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: 验证依赖可解析、测试可编译**

Run: `mvn -pl cookbook-log -q test-compile`
Expected: BUILD SUCCESS(无编译错误;此时还未写加载测试,仅验证依赖引入不破坏构建)

- [ ] **Step 3: Commit**

```bash
git add cookbook-log/pom.xml
git commit -m "build(log): 引入 log4j-core 测试依赖用于加载 log4j2-spring.xml" -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 2: 结构校验测试(DOM)

**Files:**
- Create: `cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigStructureTest.java`

被测对象 `log4j2-spring.xml` 已存在且预期正确,因此测试写完即应通过;若失败说明配置或断言有误,需核对。

- [ ] **Step 1: 创建完整的结构校验测试类**

写入 `cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigStructureTest.java`:

```java
package io.ituknown.log;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * log4j2-spring.xml 结构校验测试。
 *
 * 通过 JDK 内置 DOM 解析配置文件，断言 Appender / Logger / Property 结构正确、
 * AppenderRef 引用完整、关键 level 与 MDC 占位符未被改坏，作为配置回归保护。
 */
class Log4j2SpringConfigStructureTest {

    private static Element configuration;

    @BeforeAll
    static void parseXml() throws Exception {
        try (InputStream in = Log4j2SpringConfigStructureTest.class.getResourceAsStream("/log4j2-spring.xml")) {
            assertNotNull(in, "log4j2-spring.xml 未能在 classpath 找到");
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            configuration = builder.parse(in).getDocumentElement();
        }
    }

    // ===== Appenders =====

    @Test
    void appenders_consoleBizError_allDefined() {
        assertEquals("Configuration", configuration.getTagName());
        Element appenders = singleChild(configuration, "Appenders");
        assertNotNull(appender(appenders, "Console"), "缺少 Console");
        assertNotNull(appender(appenders, "BizAppender"), "缺少 BizAppender");
        assertNotNull(appender(appenders, "ErrorAppender"), "缺少 ErrorAppender");
    }

    @Test
    void appenders_bizAndError_areRollingFilesWithCorrectNames() {
        Element appenders = singleChild(configuration, "Appenders");
        Element biz = appender(appenders, "BizAppender");
        Element err = appender(appenders, "ErrorAppender");
        assertEquals("RollingFile", biz.getTagName());
        assertEquals("RollingFile", err.getTagName());
        assertTrue(biz.getAttribute("fileName").endsWith("app.log"),
                "BizAppender fileName 应以 app.log 结尾");
        assertTrue(err.getAttribute("fileName").endsWith("error.log"),
                "ErrorAppender fileName 应以 error.log 结尾");
    }

    @Test
    void appenders_bizAndError_haveRollingPolicies() {
        Element appenders = singleChild(configuration, "Appenders");
        for (String name : new String[]{"BizAppender", "ErrorAppender"}) {
            Element appender = appender(appenders, name);
            Element policies = singleChild(appender, "Policies");
            assertNotNull(singleChild(policies, "TimeBasedTriggeringPolicy"),
                    name + " 缺少 TimeBasedTriggeringPolicy");
            Element sizePolicy = singleChild(policies, "SizeBasedTriggeringPolicy");
            assertNotNull(sizePolicy, name + " 缺少 SizeBasedTriggeringPolicy");
            assertEquals("10MB", sizePolicy.getAttribute("size"), name + " size 应为 10MB");

            Element strategy = singleChild(appender, "DefaultRolloverStrategy");
            assertNotNull(strategy, name + " 缺少 DefaultRolloverStrategy");
            assertEquals("30", strategy.getAttribute("max"), name + " max 应为 30");
        }
    }

    // ===== Properties =====

    @Test
    void properties_appIdLogFileDirPattern_definedAndPatternHasMdc() {
        Element props = singleChild(configuration, "Properties");
        assertNotNull(property(props, "APP_ID"), "缺少 APP_ID");
        assertNotNull(property(props, "LOG_FILE_DIR"), "缺少 LOG_FILE_DIR");
        Element pattern = property(props, "PATTERN");
        assertNotNull(pattern, "缺少 PATTERN");
        String value = pattern.getTextContent().trim();
        assertTrue(value.contains("%notEmpty"), "PATTERN 应含 %notEmpty");
        assertTrue(value.contains("%X{traceId}"), "PATTERN 应含 traceId 占位符");
        assertTrue(value.contains("%X{userId}"), "PATTERN 应含 userId 占位符");
    }

    // ===== Loggers =====

    @Test
    void loggers_hibernateAndHikari_infoAdditivityFalse() {
        Element loggers = singleChild(configuration, "Loggers");
        Element hibernate = logger(loggers, "org.hibernate.SQL");
        assertEquals("INFO", hibernate.getAttribute("level"), "org.hibernate.SQL level 应为 INFO");
        assertEquals("false", hibernate.getAttribute("additivity"), "org.hibernate.SQL additivity 应为 false");
        assertRefs(hibernate, "Console", "BizAppender");

        Element hikari = logger(loggers, "com.zaxxer.hikari");
        assertEquals("INFO", hikari.getAttribute("level"), "com.zaxxer.hikari level 应为 INFO");
        assertEquals("false", hikari.getAttribute("additivity"), "com.zaxxer.hikari additivity 应为 false");
        assertRefs(hikari, "Console", "BizAppender");
    }

    @Test
    void loggers_springInfo() {
        Element spring = logger(singleChild(configuration, "Loggers"), "org.springframework");
        assertNotNull(spring, "缺少 org.springframework Logger");
        assertEquals("INFO", spring.getAttribute("level"));
    }

    @Test
    void asyncRoot_infoWithErrorAppenderAtErrorLevel() {
        Element root = singleChild(singleChild(configuration, "Loggers"), "AsyncRoot");
        assertEquals("INFO", root.getAttribute("level"));
        assertRefs(root, "Console", "BizAppender");
        Element errorRef = appenderRef(root, "ErrorAppender");
        assertNotNull(errorRef, "AsyncRoot 应引用 ErrorAppender");
        assertEquals("ERROR", errorRef.getAttribute("level"), "ErrorAppender 应以 level=ERROR 限定");
    }

    @Test
    void appenderRefs_allResolveToDefinedAppenders() {
        Element appenders = singleChild(configuration, "Appenders");
        Set<String> defined = new HashSet<>();
        for (Element child : directChildren(appenders)) {
            if (!child.getTagName().equals("AppenderRef")) {
                defined.add(child.getAttribute("name"));
            }
        }
        Element loggers = singleChild(configuration, "Loggers");
        for (Element loggerEl : directChildren(loggers)) {
            for (Element ref : directChildren(loggerEl)) {
                if (!ref.getTagName().equals("AppenderRef")) {
                    continue;
                }
                String refName = ref.getAttribute("ref");
                assertTrue(defined.contains(refName),
                        "AppenderRef 引用了未定义的 Appender: " + refName);
            }
        }
    }

    // ===== helpers =====

    /** 在 parent 的直接子元素中，按 tagName 取唯一元素；多于一个则失败 */
    private static Element singleChild(Element parent, String tagName) {
        Element found = null;
        for (Element child : directChildren(parent)) {
            if (child.getTagName().equals(tagName)) {
                if (found != null) {
                    fail("期望唯一子节点 " + tagName + "，但找到多个");
                }
                found = child;
            }
        }
        return found;
    }

    /** 在 parent 直接子元素中，按 name 属性取元素（Appender / Property / Logger） */
    private static Element namedChild(Element parent, String name) {
        for (Element child : directChildren(parent)) {
            if (name.equals(child.getAttribute("name"))) {
                return child;
            }
        }
        return null;
    }

    private static Element appender(Element appendersEl, String name) {
        return namedChild(appendersEl, name);
    }

    private static Element property(Element propsEl, String name) {
        return namedChild(propsEl, name);
    }

    private static Element logger(Element loggersEl, String name) {
        return namedChild(loggersEl, name);
    }

    /** 在 logger 元素的直接子元素中，按 ref 取 AppenderRef */
    private static Element appenderRef(Element loggerEl, String ref) {
        for (Element child : directChildren(loggerEl)) {
            if (child.getTagName().equals("AppenderRef") && ref.equals(child.getAttribute("ref"))) {
                return child;
            }
        }
        return null;
    }

    private static void assertRefs(Element loggerEl, String... refs) {
        for (String ref : refs) {
            assertNotNull(appenderRef(loggerEl, ref), "Logger 应引用 Appender: " + ref);
        }
    }

    /** parent 的直接子元素（跳过文本节点等非 Element） */
    private static java.util.List<Element> directChildren(Element parent) {
        java.util.List<Element> elements = new java.util.ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element e) {
                elements.add(e);
            }
        }
        return elements;
    }
}
```

- [ ] **Step 2: 运行测试验证通过**

Run: `mvn -pl cookbook-log -q test -Dtest=Log4j2SpringConfigStructureTest`
Expected: BUILD SUCCESS,Tests run: 7(全部 PASS)。若某断言失败,核对 `log4j2-spring.xml` 与断言是否一致并修正。

- [ ] **Step 3: Commit**

```bash
git add cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigStructureTest.java
git commit -m "test(log): DOM 结构校验 log4j2-spring.xml 的 Appender/Logger/Property 与引用完整性" -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: 加载测试骨架 + 加载成功用例

**Files:**
- Create: `cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigLoadingTest.java`

- [ ] **Step 1: 创建加载测试类骨架与第一个用例**

写入 `cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigLoadingTest.java`:

```java
package io.ituknown.log;

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
}
```

- [ ] **Step 2: 运行测试验证通过**

Run: `mvn -pl cookbook-log -q test -Dtest=Log4j2SpringConfigLoadingTest`
Expected: BUILD SUCCESS,Tests run: 1(PASS)。若加载失败(ctx 为 null 或抛异常),查看 log4j2 status 输出排查(常见原因:替换正则未生效、系统属性未设)。

- [ ] **Step 3: Commit**

```bash
git add cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigLoadingTest.java
git commit -m "test(log): log4j2-spring.xml 可被 log4j2 合法加载且 Appender 就位" -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: ERROR 路由用例

**Files:**
- Modify: `cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigLoadingTest.java`

- [ ] **Step 1: 补充 import**

在文件顶部 import 区追加:

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.file.Files;
```

- [ ] **Step 2: 在 `loadsWithoutFatalError_appendersPresent` 方法之后、`}` 结束类之前，插入新用例**

```java
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
```

- [ ] **Step 3: 运行测试验证通过**

Run: `mvn -pl cookbook-log -q test -Dtest=Log4j2SpringConfigLoadingTest`
Expected: BUILD SUCCESS,Tests run: 2(均 PASS)。若 app.log/error.log 未含 marker,通常是异步队列未 flush——确认 `shutdown` 用的是带超时版本且返回 true。

- [ ] **Step 4: Commit**

```bash
git add cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigLoadingTest.java
git commit -m "test(log): 验证 ERROR 同时路由进 app.log 与 error.log" -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 5: INFO/WARN 路由用例 + 全量回归

**Files:**
- Modify: `cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigLoadingTest.java`

- [ ] **Step 1: 在 `errorRoutesToBothFiles` 方法之后插入新用例**

```java
    @Test
    void infoRoutesOnlyToAppLog(@TempDir Path logDir) throws Exception {
        LoggerContext ctx = loadConfig(logDir);
        try {
            Logger logger = LogManager.getLogger("io.ituknown.log.RouteTest");
            logger.info("route-info-marker");
            logger.warn("route-warn-marker");
        } finally {
            shutdown(ctx);
        }
        String appLog = Files.readString(resolve(logDir, "app.log"));
        assertTrue(appLog.contains("route-info-marker"), "INFO 应写入 app.log");
        assertTrue(appLog.contains("route-warn-marker"), "WARN 应写入 app.log");

        // error.log 仅收 ERROR；本轮无 ERROR，文件可能为空或未创建
        Path errorLogPath = resolve(logDir, "error.log");
        if (Files.exists(errorLogPath)) {
            String errorLog = Files.readString(errorLogPath);
            assertFalse(errorLog.contains("route-info-marker"), "INFO 不应写入 error.log");
            assertFalse(errorLog.contains("route-warn-marker"), "WARN 不应写入 error.log");
        }
    }
```

- [ ] **Step 2: 运行该模块全部测试做回归**

Run: `mvn -pl cookbook-log -q test`
Expected: BUILD SUCCESS;`Log4j2SpringConfigStructureTest`(7)、`Log4j2SpringConfigLoadingTest`(3)、`MdcScopeTest`、`MdcUtilsTest` 全部 PASS,无相互污染。

- [ ] **Step 3: Commit**

```bash
git add cookbook-log/src/test/java/io/ituknown/log/Log4j2SpringConfigLoadingTest.java
git commit -m "test(log): 验证 INFO/WARN 仅路由进 app.log 不进 error.log" -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review

**1. Spec coverage(对照 spec 各节):**
- spec §4 Appenders/Properties/Loggers/引用完整性 → Task 2 的 7 个测试方法逐条覆盖。✅
- spec §5.1 三项技术处理(spring 替换 / sys 属性 / 异步 flush)→ Task 3 的 `loadConfig`/`shutdown`/`System.setProperty` 覆盖。✅
- spec §5.2 三个加载用例(loads / error / info)→ Task 3 / Task 4 / Task 5 分别覆盖。✅
- spec §5.3 隔离(`@AfterEach` 清系统属性 + slf4j/logback 不受影响)→ Task 3 的 `clearSystemProperty` 与 Task 5 全量回归验证。✅
- spec §6 依赖变更 → Task 1。✅

**2. Placeholder scan:** 无 TBD/TODO;每个含代码的步骤都有完整可运行代码。✅

**3. Type consistency:** helper 名跨任务一致——结构测试 `singleChild`/`namedChild`/`appender`/`property`/`logger`/`appenderRef`/`assertRefs`/`directChildren`;加载测试 `TEST_APP_ID`/`loadConfig`/`shutdown`/`resolve` 在 Task 3 定义、Task 4/5 复用,签名一致。✅
