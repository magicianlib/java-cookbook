# log4j2-spring.xml 测试用例设计

- 日期:2026-06-23
- 模块:cookbook-log
- 被测文件:`src/main/resources/log4j2-spring.xml`

## 1. 背景

cookbook-log 模块现有测试(`MdcScopeTest`、`MdcUtilsTest`)只覆盖 MDC 工具类,尚未覆盖模块内的 `log4j2-spring.xml` 配置文件。该配置是 Spring Boot 环境下的 Log4j2 参考配置,含 3 个 Appender(`Console`、`BizAppender`→`app.log`、`ErrorAppender`→`error.log`)、`AsyncRoot` 异步根日志,以及 MDC(`traceId`/`userId`)输出格式。需为该配置补充测试用例,作为回归保护。

模块 main 依赖刻意精简(仅 `slf4j-api` + `disruptor`),test scope 用 `logback-classic` 跑 MDC 单测。该 XML 当前并非在模块内实际加载运行,而是参考配置。

## 2. 目标

- 防止配置被改坏:Appender 改名而 Logger 仍引用旧名、level 配错、MDC 占位符丢失、滚动策略参数变化等。
- 证明该配置能被 Log4j2 真实加载,且日志路由(`ERROR` 同时进 `app.log` 与 `error.log`,`INFO` 只进 `app.log`)符合预期。

## 3. 范围

新增两个测试类,均位于 `cookbook-log/src/test/java/io/ituknown/log/`:

1. `Log4j2SpringConfigStructureTest` —— 结构校验(纯 JDK DOM)
2. `Log4j2SpringConfigLoadingTest` —— 真实加载 + 路由验证(log4j2-core)

风格沿用现有约定:JUnit 5、`方法_场景_结果` 蛇形命名、中文注释分块、JUnit 原生 `Assertions`。

## 4. 方案 1:结构校验覆盖点

用 `javax.xml.parsers.DocumentBuilder` 解析 `src/main/resources/log4j2-spring.xml`。

- **Appenders**
  - `Console`,`target=SYSTEM_OUT`
  - `BizAppender`,`RollingFile`,`fileName` 含 `app.log`;含 `TimeBasedTriggeringPolicy`、`SizeBasedTriggeringPolicy size=10MB`、`DefaultRolloverStrategy max=30`、`Delete age=30d`
  - `ErrorAppender`,`RollingFile`,`fileName` 含 `error.log`;策略同上
- **Properties**:`APP_ID`、`LOG_FILE_DIR`、`PATTERN` 均存在;`PATTERN` 含 `%notEmpty`、`%X{traceId}`、`%X{userId}`
- **Loggers**
  - `org.hibernate.SQL`:`level=INFO`,`additivity=false`,引用 `Console`+`BizAppender`
  - `com.zaxxer.hikari`:`level=INFO`,`additivity=false`,引用 `Console`+`BizAppender`
  - `org.springframework`:`level=INFO`
  - `AsyncRoot`:`level=INFO`,引用 `Console`+`BizAppender`,`ErrorAppender` 以 `level=ERROR` 限定
- **引用完整性**:所有 `AppenderRef/@ref` 必须在 `<Appenders>` 中有定义

## 5. 方案 2:加载 + 真实路由覆盖点

用 log4j2-core 真实加载该 XML。

### 5.1 技术处理

- **`${spring:...}` lookup 阻断**:纯 log4j2 无 spring lookup,`APP_ID` 会保留含 `:` 的字面量,而 `:` 是 Windows 非法路径字符,导致 `RollingFile` 创建文件失败。处理:测试读取原 XML 文本,**仅将 `${spring:...}` 占位符替换为测试字面值**后从内存加载。只改 lookup 占位符,不动路由结构。
- **`${sys:logging.file.dir}`**:`@TempDir` 提供临时目录,`@BeforeEach` 用 `System.setProperty` 设 `logging.file.dir`,sys lookup 成功取值,不触发 spring fallback。
- **`AsyncRoot` 异步时序**:每个路由场景独立执行 `initialize` → 触发日志 → `Configurator.shutdown(ctx)`(drain disruptor 队列 + flush/close `RollingFile`)→ 读文件断言。

### 5.2 用例

- `loadsWithoutFatalError`:加载成功,`Configuration` 含 `BizAppender`/`ErrorAppender`,root logger 存在
- `errorRoutesToBothFiles`:触发 `ERROR`,断言 `app.log` 与 `error.log` 均含该消息
- `infoRoutesOnlyToAppLog`:触发 `INFO`(与 `WARN`),断言 `app.log` 含、`error.log` 不含

### 5.3 隔离

- 加载测试操作 log4j2 全局 `LogManager` context,与 `MdcScopeTest`/`MdcUtilsTest`(走 slf4j→logback)互不干扰。
- `@AfterEach` 还原系统属性。

## 6. 依赖变更

`cookbook-log/pom.xml` 新增(test scope,版本由 `spring-boot-dependencies 3.5.9` BOM 管理,无需写 `version`):

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <scope>test</scope>
</dependency>
```

## 7. 风险与边界

- 真实路由用例依赖文件 I/O + 异步 flush;`Configurator.shutdown` 是 log4j2 的确定行为,时序可控。CI 偶发抖动可重试。
- 结构校验是"配置正确性"的主力回归保护;加载路由验证额外确认 log4j2 按这份配置实际路由,两者互补。

## 8. 关键决策

- **`${spring:...}` 绕过**:采用"内存替换占位符"(简单可靠),不采用"自定义 spring Lookup plugin + 注解处理器生成 `Log4j2Plugins.dat`"(构建更重)。理由:前者仅替换 lookup 占位符,路由结构不变,验证有效性等同,且无需改动 maven-compiler-plugin 的 annotation processor 配置。
