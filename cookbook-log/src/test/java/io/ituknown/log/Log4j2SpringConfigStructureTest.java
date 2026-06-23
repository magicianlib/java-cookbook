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
