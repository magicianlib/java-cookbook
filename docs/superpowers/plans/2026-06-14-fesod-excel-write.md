# cookbook-fesod Excel 写功能 实现计划

> **修订说明（2026-06-14）**：本计划描述的是**初版**设计（注册表 + 静态门面 + 框架分页），
> 已按反馈重构为"纯抽象类 + 实例 write + 子类自管分页"。**最终设计以 spec 为准**：
> `docs/superpowers/specs/2026-06-14-fesod-excel-write-design.md`。下方任务步骤仅供追溯。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `cookbook-fesod` 实现可扩展的同步 Excel 写功能：按业务类型定位子类、子类把字符串参数转为请求对象并分页加载数据，框架逐页写入，提供「写入 OutputStream」与「临时文件 + 用完即删」两种输出方式。

**Architecture:** 抽象基类 `WriteExecutor<P, D>` 同时承载子类契约、私有静态注册表（构造自注册）与对外静态门面。模板方法 `doWrite` 驱动分页循环：`buildPayload` 转换字符串 → `loadPage` 分页加载 → `writer.write` 逐页落盘 → 三重条件终止。复用 `cookbook-payload` 的 `Page`/`PageRequest`/`Pagination` 与异常体系。基于 `fesod-sheet`（EasyExcel/FastExcel 更名版）。

**Tech Stack:** Java 21、`fesod-sheet` 2.0.2-incubating、`cookbook-payload`、slf4j、JUnit 5。无 Spring、无 Lombok（保持轻量）。

**对应 Spec：** `docs/superpowers/specs/2026-06-14-fesod-excel-write-design.md`

---

## 文件结构

| 文件 | 责任 | 动作 |
|---|---|---|
| `cookbook-fesod/pom.xml` | 声明 `cookbook-payload`、`slf4j-api` 依赖 | 修改 |
| `src/main/java/io/ituknown/fesod/Context.java` | 框架执行上下文：持有 bizType/bizParams/payload/startTime，`finish` 打印摘要日志 | 修改（补全） |
| `src/main/java/io/ituknown/fesod/write/WriteBizTypeEnums.java` | 业务类型枚举（应用侧扩展；cookbook 提供示例常量） | 修改（补常量） |
| `src/main/java/io/ituknown/fesod/write/WriteExecutor.java` | 抽象基类 + 注册表 + 静态门面 + 分页模板方法 + 临时文件模式 | 重写 |
| `src/test/java/io/ituknown/fesod/write/WriteExecutorTest.java` | 流式写入/分页终止/临时文件/注册表 测试 + 内嵌测试执行器与行 Bean | 新建 |
| `src/test/java/io/ituknown/fesod/App.java` | 旧的空测试桩 | 删除（避免混淆） |

**API 速查（已核实 fesod-sheet 2.0.2-incubating）：**
- `FastExcel.write(OutputStream, Class)` → `ExcelWriterBuilder`；`.excelType(ExcelTypeEnum)` → `.build()` → `ExcelWriter`
- `FastExcel.writerSheet()` → `ExcelWriterSheetBuilder`；`.sheetName(String)` → `.build()` → `WriteSheet`
- `ExcelWriter.write(Collection, WriteSheet)` → `ExcelWriter`；`.finish()` / `.close()`
- `FastExcel.read(InputStream).head(Class).sheet().doReadSync()` → `List<T>`（`head(Class)` 继承自 `AbstractParameterBuilder`）
- payload：`new Page<>(list, current, pageSize, total)`、`PageRequest`（Lombok 可变 Bean，`setCurrent`/`setPageSize`）、`Pagination.getPages()`、`BizNotFoundException(String)`

---

## Task 0：创建特性分支

当前在 `main`（默认分支），按约定先开分支再提交。

- [ ] **Step 1：创建并切换分支**

```bash
git checkout -b feat/fesod-excel-write
```

预期：`Switched to a new branch 'feat/fesod-excel-write'`

---

## Task 1：项目脚手架（依赖 + Context + 枚举常量）

**Files:**
- Modify: `cookbook-fesod/pom.xml`
- Modify: `cookbook-fesod/src/main/java/io/ituknown/fesod/Context.java`
- Modify: `cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteBizTypeEnums.java`
- Delete: `cookbook-fesod/src/test/java/io/ituknown/fesod/App.java`

本任务是纯编译期脚手架（无独立可测行为）；测试从 Task 2 起进入 TDD。

- [ ] **Step 1：删除旧的空测试桩**

```bash
git rm cookbook-fesod/src/test/java/io/ituknown/fesod/App.java
```

预期：`rm 'cookbook-fesod/src/test/java/io/ituknown/fesod/App.java'`

- [ ] **Step 2：在 pom.xml 的 `<dependencies>` 中追加 `cookbook-payload` 与 `slf4j-api`**

在 `cookbook-fesod/pom.xml` 的 `<dependencies>` 块内、`junit-jupiter` 依赖**之前**插入：

```xml
        <dependency>
            <groupId>io.ituknown</groupId>
            <artifactId>cookbook-payload</artifactId>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>

```

> 版本由根 pom 的 Spring Boot BOM（`spring-boot-dependencies`）托管，无需写 `<version>`；`cookbook-payload` 版本由根 dependencyManagement 托管。

- [ ] **Step 3：补全 `Context.java`**

整体替换 `cookbook-fesod/src/main/java/io/ituknown/fesod/Context.java` 内容为：

```java
package io.ituknown.fesod;

import io.ituknown.fesod.write.WriteBizTypeEnums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Excel 写入执行上下文（框架内部使用）。
 * <p>
 * 由 {@link io.ituknown.fesod.write.WriteExecutor} 在开始写入时创建，
 * 持有业务类型、原始参数、解析后的请求对象与起始时间，
 * 在写入结束时打印一行摘要日志。
 *
 * @param <T> 请求对象（payload）类型
 * @author magicianlib@gmail.com
 */
public final class Context<T> {

    private static final Logger log = LoggerFactory.getLogger(Context.class);

    private final WriteBizTypeEnums bizType;
    private final String bizParams;
    private final T payload;
    private final long startTimeMs;

    private Context(WriteBizTypeEnums bizType, String bizParams, T payload, long startTimeMs) {
        this.bizType = bizType;
        this.bizParams = bizParams;
        this.payload = payload;
        this.startTimeMs = startTimeMs;
    }

    static <T> Context<T> start(WriteBizTypeEnums bizType, String bizParams, T payload) {
        return new Context<>(bizType, bizParams, payload, System.currentTimeMillis());
    }

    /** 返回解析后的请求对象 */
    public T payload() {
        return payload;
    }

    /** 写入结束时调用，打印耗时与行数摘要 */
    void finish(long rows) {
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        log.info("Excel 写入完成 | bizType={} | bizParams={} | rows={} | elapsedMs={}",
                bizType, bizParams, rows, elapsedMs);
    }
}
```

> `start` 与 `finish` 为包级可见。注意：`Context` 在 `io.ituknown.fesod` 包，`WriteExecutor` 在 `io.ituknown.fesod.write` 子包——跨包访问需 public，故 `Context` 为 public class；`start`/`finish` 虽是包级，但 `WriteExecutor` 不在同包，**因此需将 `start`/`finish` 改为 public**。修正见下。

- [ ] **Step 4：将 `Context.start` 与 `finish` 改为 public（跨包可访问）**

将 Step 3 代码中的两处签名改权限：

```java
    public static <T> Context<T> start(WriteBizTypeEnums bizType, String bizParams, T payload) {
        return new Context<>(bizType, bizParams, payload, System.currentTimeMillis());
    }
```

```java
    public void finish(long rows) {
```

- [ ] **Step 5：补全 `WriteBizTypeEnums.java`**

整体替换 `cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteBizTypeEnums.java` 为：

```java
package io.ituknown.fesod.write;

/**
 * Excel 写入业务类型枚举。
 * <p>
 * 实际应用中按自身业务替换或扩展常量；cookbook 中提供两个示例常量，
 * 供 {@link WriteExecutor} 的注册与门面查表使用。
 *
 * @author magicianlib@gmail.com
 */
public enum WriteBizTypeEnums {

    /** 示例：用户列表导出（测试执行器注册于此） */
    USER_EXPORT,

    /** 示例：订单导出（无注册执行器，用于测试未命中场景） */
    ORDER_EXPORT
}
```

- [ ] **Step 6：编译验证**

Run: `mvn -q -pl cookbook-fesod -am compile`
预期：BUILD SUCCESS（无编译错误）。`Context.java` 引用了尚未重写的 `WriteExecutor`？不会——Context 只引用 `WriteBizTypeEnums`，不引用 WriteExecutor，可独立编译。

- [ ] **Step 7：提交**

```bash
git add cookbook-fesod/pom.xml cookbook-fesod/src/main/java/io/ituknown/fesod/Context.java cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteBizTypeEnums.java
git commit -m "feat(fesod): add payload/slf4j deps, flesh out Context and WriteBizTypeEnums

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 2：WriteExecutor 注册表 + 门面骨架 + 查表未命中

**Files:**
- Create: `cookbook-fesod/src/test/java/io/ituknown/fesod/write/WriteExecutorTest.java`
- Rewrite: `cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteExecutor.java`

本任务实现注册表、构造自注册、`requireExecutor` 与两个静态门面（先委托给尚未实现的 `doWrite`，后者抛 `UnsupportedOperationException`）。可测行为：查表未命中抛 `BizNotFoundException`；重复注册抛 `IllegalStateException`。

- [ ] **Step 1：编写失败测试（查表未命中 + 重复注册 + 内嵌测试执行器）**

新建 `cookbook-fesod/src/test/java/io/ituknown/fesod/write/WriteExecutorTest.java`：

```java
package io.ituknown.fesod.write;

import io.ituknown.fesod.write.WriteExecutorTest.TestWriteExecutor.TestPayload;
import io.ituknown.payload.Page;
import io.ituknown.payload.PageRequest;
import io.ituknown.payload.exception.BizNotFoundException;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WriteExecutorTest {

    /** 引用即触发 TestWriteExecutor 类加载与自注册 */
    private static final TestWriteExecutor REGISTERED = TestWriteExecutor.INSTANCE;

    @Test
    void write_unknownBizType_throwsBizNotFound() {
        assertThrows(BizNotFoundException.class,
                () -> WriteExecutor.write(WriteBizTypeEnums.ORDER_EXPORT, "1", new ByteArrayOutputStream()));
    }

    @Test
    void duplicateRegistration_throwsIllegalState() {
        assertThrows(IllegalStateException.class, TestWriteExecutor::new);
    }

    // ===== 测试夹具 =====

    /** Excel 行 Bean */
    public static class TestRow {
        @ExcelProperty("姓名")
        private String name;
        @ExcelProperty("年龄")
        private Integer age;

        public TestRow() {
        }

        public TestRow(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

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

    /** 测试用执行器：bizParams 解析为总条数，按页生成连续行 */
    public static class TestWriteExecutor extends WriteExecutor<TestPayload, TestRow> {

        public static final TestWriteExecutor INSTANCE = new TestWriteExecutor();

        /** 构造包级可见，便于重复注册测试 */
        TestWriteExecutor() {
            super();
        }

        @Override
        public WriteBizTypeEnums bizType() {
            return WriteBizTypeEnums.USER_EXPORT;
        }

        @Override
        public TestPayload buildPayload(String bizParams) {
            return new TestPayload(Integer.parseInt(bizParams));
        }

        @Override
        public Class<TestRow> headType() {
            return TestRow.class;
        }

        @Override
        public Page<TestRow> loadPage(TestPayload payload, PageRequest req) {
            int start = (req.getCurrent() - 1) * req.getPageSize();
            int end = Math.min(start + req.getPageSize(), payload.total());
            List<TestRow> rows = new ArrayList<>();
            for (int i = start; i < end; i++) {
                rows.add(new TestRow("name-" + i, i));
            }
            return new Page<>(rows, req.getCurrent(), req.getPageSize(), payload.total());
        }

        @Override
        public int pageSize() {
            return 3;
        }

        /** 测试请求对象 */
        public record TestPayload(int total) {
        }
    }
}
```

- [ ] **Step 2：运行测试，验证编译失败（WriteExecutor 仍是旧骨架）**

Run: `mvn -q -pl cookbook-fesod -am test -Dtest=WriteExecutorTest`
预期：编译失败——`WriteExecutor` 缺少 `static write(...)`、`loadPage(...)` 签名不符、`TestWriteExecutor` 构造无法 `super()` 等。

- [ ] **Step 3：重写 `WriteExecutor.java`（注册表 + 门面骨架，doWrite 暂抛 UOE）**

整体替换 `cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteExecutor.java`：

```java
package io.ituknown.fesod.write;

import io.ituknown.fesod.Context;
import io.ituknown.payload.Page;
import io.ituknown.payload.PageRequest;
import io.ituknown.payload.exception.BizNotFoundException;
import org.apache.fesod.sheet.support.ExcelTypeEnum;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Excel 同步写执行器抽象基类，同时承载：
 * <ul>
 *   <li>子类契约（抽象方法）；</li>
 *   <li>私有静态注册表——子类构造即自注册；</li>
 *   <li>对外静态门面 {@link #write}——按业务类型查表并执行模板方法。</li>
 * </ul>
 *
 * <b>使用约定：</b>
 * <ol>
 *   <li>{@link #bizType()} 必须返回常量：构造期 {@code this} 尚未完全初始化，
 *       不能读取实例字段（仅返回枚举常量）。</li>
 *   <li>子类以 {@code public static final INSTANCE} 单例形式存在，引用 INSTANCE 即触发类加载与注册；
 *       应用启动时需引用到这些 INSTANCE 才会注册。</li>
 * </ol>
 *
 * @param <P> 请求对象（payload）类型，由 {@link #buildPayload} 从字符串转换而来
 * @param <D> Excel 行 Bean 类型（字段标注 {@link org.apache.fesod.sheet.annotation.ExcelProperty}）
 * @author magicianlib@gmail.com
 */
public abstract class WriteExecutor<P, D> {

    private static final ConcurrentHashMap<WriteBizTypeEnums, WriteExecutor<?, ?>> REGISTRY =
            new ConcurrentHashMap<>();

    /** 子类构造即自注册。要求 {@link #bizType()} 返回常量。 */
    protected WriteExecutor() {
        WriteExecutor<?, ?> prev = REGISTRY.putIfAbsent(bizType(), this);
        if (prev != null) {
            throw new IllegalStateException("业务类型已注册执行器: " + bizType());
        }
    }

    // ===== 子类契约 =====

    /** 业务类型标识，用于注册表定位 */
    public abstract WriteBizTypeEnums bizType();

    /** 将外部传入的 bizParams 字符串转换为「加载数据的请求对象」P */
    public abstract P buildPayload(String bizParams);

    /** Excel 行 Bean 的 Class（fesod 用它推断表头列结构） */
    public abstract Class<D> headType();

    /** 分页加载一页数据（同步） */
    public abstract Page<D> loadPage(P payload, PageRequest pageRequest);

    // ===== 可选覆盖（带默认值） =====

    /** 默认 sheet 名 */
    public String sheetName() {
        return "Sheet1";
    }

    /** 默认每页条数 */
    public int pageSize() {
        return 1000;
    }

    /** 默认 Excel 类型 */
    public ExcelTypeEnum excelType() {
        return ExcelTypeEnum.XLSX;
    }

    // ===== 对外门面 =====

    /** 模式 A：写入调用方传入的 OutputStream */
    public static void write(WriteBizTypeEnums bizType, String bizParams, OutputStream out) {
        WriteExecutor<?, ?> exec = requireExecutor(bizType);
        exec.doWrite(bizParams, out);
    }

    /** 模式 B：框架生成临时文件，写完后回调给调用方，回调返回后立即删除 */
    public static void write(WriteBizTypeEnums bizType, String bizParams, Consumer<Path> fileConsumer) {
        WriteExecutor<?, ?> exec = requireExecutor(bizType);
        exec.doWriteToTempFile(bizParams, fileConsumer);
    }

    private static WriteExecutor<?, ?> requireExecutor(WriteBizTypeEnums bizType) {
        WriteExecutor<?, ?> exec = REGISTRY.get(bizType);
        if (exec == null) {
            throw new BizNotFoundException("无匹配的执行器: " + bizType);
        }
        return exec;
    }

    // ===== 模板方法（Task 3/4 实现） =====

    private void doWrite(String bizParams, OutputStream out) {
        throw new UnsupportedOperationException("doWrite 尚未实现");
    }

    private void doWriteToTempFile(String bizParams, Consumer<Path> fileConsumer) {
        throw new UnsupportedOperationException("doWriteToTempFile 尚未实现");
    }
}
```

- [ ] **Step 4：运行测试，验证通过（未命中用例由 requireExecutor 在 doWrite 之前抛出）**

Run: `mvn -q -pl cookbook-fesod -am test -Dtest=WriteExecutorTest`
预期：2 个测试通过（`write_unknownBizType_throwsBizNotFound`、`duplicateRegistration_throwsIllegalState`）。

- [ ] **Step 5：提交**

```bash
git add cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteExecutor.java cookbook-fesod/src/test/java/io/ituknown/fesod/write/WriteExecutorTest.java
git commit -m "feat(fesod): add WriteExecutor registry, facade skeleton and lookup error

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 3：流式写入 + 分页循环

**Files:**
- Modify: `cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteExecutor.java`（实现 `doWrite`）
- Modify: `cookbook-fesod/src/test/java/io/ituknown/fesod/write/WriteExecutorTest.java`（追加流式测试）

可测行为：多页数据（总条数非每页整数倍）与整除情形，均写出正确行数与内容；分页循环按 `Pagination.getPages()` 正确终止。

- [ ] **Step 1：追加失败测试（多页 + 整除，用 fesod 读回校验）**

在 `WriteExecutorTest` 顶部 import 区追加：

```java
import org.apache.fesod.sheet.FastExcel;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
```

在类体内（`duplicateRegistration_throwsIllegalState` 之后、`// ===== 测试夹具 =====` 之前）追加：

```java
    @Test
    void write_stream_writesAllRows_multiPage() {
        // bizParams "7" → total 7, pageSize 3 → 3 + 3 + 1
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WriteExecutor.write(WriteBizTypeEnums.USER_EXPORT, "7", out);

        List<TestRow> rows = FastExcel.read(new ByteArrayInputStream(out.toByteArray()))
                .head(TestRow.class)
                .sheet()
                .doReadSync();

        assertEquals(7, rows.size());
        assertEquals("name-0", rows.get(0).getName());
        assertEquals(Integer.valueOf(0), rows.get(0).getAge());
        assertEquals("name-6", rows.get(6).getName());
    }

    @Test
    void write_stream_writesAllRows_exactPages() {
        // bizParams "6" → total 6, pageSize 3 → 3 + 3，整除，末页满页
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WriteExecutor.write(WriteBizTypeEnums.USER_EXPORT, "6", out);

        List<TestRow> rows = FastExcel.read(new ByteArrayInputStream(out.toByteArray()))
                .head(TestRow.class)
                .sheet()
                .doReadSync();

        assertEquals(6, rows.size());
        assertEquals("name-5", rows.get(5).getName());
    }
```

- [ ] **Step 2：运行测试，验证失败（doWrite 仍抛 UOE）**

Run: `mvn -q -pl cookbook-fesod -am test -Dtest=WriteExecutorTest`
预期：`write_stream_writesAllRows_multiPage`、`write_stream_writesAllRows_exactPages` 抛 `UnsupportedOperationException` 而失败；前两个用例仍通过。

- [ ] **Step 3：实现 `doWrite`（分页循环）**

在 `WriteExecutor.java` 的 import 区追加：

```java
import io.ituknown.payload.Pagination;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FastExcel;
import org.apache.fesod.sheet.write.metadata.WriteSheet;

import java.util.List;
```

将 `doWrite` 方法体替换为：

```java
    private void doWrite(String bizParams, OutputStream out) {
        P payload = buildPayload(bizParams);
        Context<P> ctx = Context.start(bizType(), bizParams, payload);

        try (ExcelWriter writer = FastExcel.write(out, headType())
                .excelType(excelType())
                .build()) {
            WriteSheet sheet = FastExcel.writerSheet()
                    .sheetName(sheetName())
                    .build();

            long rows = 0;
            int size = pageSize();
            for (int current = 1; ; current++) {
                PageRequest req = newPageRequest(current, size);
                Page<D> page = loadPage(payload, req);
                List<D> list = page.list();
                if (list.isEmpty()) {
                    break;                            // 无数据 → 结束
                }
                writer.write(list, sheet);
                rows += list.size();

                Pagination pg = page.pagination();
                if (pg != null && pg.getPages() > 0 && current >= pg.getPages()) {
                    break;                            // 已到末页（最可靠）
                }
                if (list.size() < size) {
                    break;                            // 不足一页 → 末页兜底
                }
            }
            writer.finish();
        }

        ctx.finish(rows);
    }

    private static PageRequest newPageRequest(int current, int pageSize) {
        PageRequest req = new PageRequest();
        req.setCurrent(current);
        req.setPageSize(pageSize);
        return req;
    }
```

- [ ] **Step 4：运行测试，验证全部通过**

Run: `mvn -q -pl cookbook-fesod -am test -Dtest=WriteExecutorTest`
预期：4 个测试全部通过。

- [ ] **Step 5：提交**

```bash
git add cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteExecutor.java cookbook-fesod/src/test/java/io/ituknown/fesod/write/WriteExecutorTest.java
git commit -m "feat(fesod): implement paged stream write loop in WriteExecutor

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 4：临时文件模式（用完即删）

**Files:**
- Modify: `cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteExecutor.java`（实现 `doWriteToTempFile`）
- Modify: `cookbook-fesod/src/test/java/io/ituknown/fesod/write/WriteExecutorTest.java`（追加临时文件测试）

可测行为：临时文件模式下，回调内文件存在且可读出正确行数；回调返回（方法执行完）后文件已被删除。

- [ ] **Step 1：追加失败测试（临时文件模式）**

在 `WriteExecutorTest` 顶部 import 区追加：

```java
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
```

在类体内（`write_stream_writesAllRows_exactPages` 之后、`// ===== 测试夹具 =====` 之前）追加：

```java
    @Test
    void write_tempFile_readableInCallbackAndDeletedAfter() {
        AtomicReference<Path> pathRef = new AtomicReference<>();

        WriteExecutor.write(WriteBizTypeEnums.USER_EXPORT, "5", path -> {
            pathRef.set(path);
            assertTrue(Files.exists(path), "回调内临时文件应存在");

            try (InputStream in = Files.newInputStream(path)) {
                List<TestRow> rows = FastExcel.read(in)
                        .head(TestRow.class)
                        .sheet()
                        .doReadSync();
                assertEquals(5, rows.size(), "回调内应能读出全部行");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertFalse(Files.exists(pathRef.get()), "回调返回后临时文件应已被删除");
    }
```

- [ ] **Step 2：运行测试，验证失败（doWriteToTempFile 仍抛 UOE）**

Run: `mvn -q -pl cookbook-fesod -am test -Dtest=WriteExecutorTest`
预期：`write_tempFile_readableInCallbackAndDeletedAfter` 抛 `UnsupportedOperationException` 而失败；其余 4 个通过。

- [ ] **Step 3：实现 `doWriteToTempFile`（复用 doWrite，finally 删除 + deleteOnExit 兜底）**

在 `WriteExecutor.java` 的 import 区追加：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
```

在类顶部字段区（`REGISTRY` 之前）追加日志器：

```java
    private static final Logger log = LoggerFactory.getLogger(WriteExecutor.class);

```

将 `doWriteToTempFile` 方法体替换为：

```java
    private void doWriteToTempFile(String bizParams, Consumer<Path> fileConsumer) {
        Path temp;
        try {
            temp = Files.createTempFile("fesod-write-", "." + excelType().getValue());
        } catch (IOException e) {
            throw new IllegalStateException("创建临时文件失败", e);
        }
        temp.toFile().deleteOnExit();                 // JVM 退出兜底（应对 Windows 文件锁）

        try (OutputStream out = Files.newOutputStream(temp)) {
            doWrite(bizParams, out);                  // 复用流式写入
            out.flush();
            fileConsumer.accept(temp);                // 调用方在 lambda 内使用文件
        } catch (IOException e) {
            throw new IllegalStateException("写入临时文件失败", e);
        } finally {
            try {
                Files.deleteIfExists(temp);           // 用完即删
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", temp, e);  // 不掩盖业务异常
            }
        }
    }
```

- [ ] **Step 4：运行全部测试，验证通过**

Run: `mvn -q -pl cookbook-fesod -am test -Dtest=WriteExecutorTest`
预期：5 个测试全部通过。

- [ ] **Step 5：运行全模块测试，确保无回归**

Run: `mvn -q -pl cookbook-fesod -am test`
预期：BUILD SUCCESS，所有测试通过。

- [ ] **Step 6：提交**

```bash
git add cookbook-fesod/src/main/java/io/ituknown/fesod/write/WriteExecutor.java cookbook-fesod/src/test/java/io/ituknown/fesod/write/WriteExecutorTest.java
git commit -m "feat(fesod): implement temp-file write mode with auto-delete

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 自审清单（编写者已执行）

**1. Spec 覆盖：**
- 注册/定位（纯 Java 自注册）→ Task 2 ✓
- 分页加载 `Page<D> loadPage(P, PageRequest)` + 按 `getPages()` 判停 → Task 3 ✓
- 三重终止条件 → Task 3 ✓
- 模式 A（OutputStream）→ Task 3 ✓
- 模式 B（Consumer<Path> + finally 删除 + deleteOnExit）→ Task 4 ✓
- `Context` 摘要日志 → Task 1 ✓
- 依赖（payload + slf4j）→ Task 1 ✓
- 异常（BizNotFoundException 未命中；删除失败仅 warn）→ Task 2 / Task 4 ✓
- 表头来源 `headType()` → Task 2 ✓
- 测试覆盖（流式/分页终止/临时文件/注册表）→ Task 2/3/4 ✓

**2. 占位符扫描：** 无 TBD/TODO；doWrite/doWriteToTempFile 在 Task 2 为 `UnsupportedOperationException` 是有意的 TDD 渐进，非占位符（后续 Task 实现）。

**3. 类型一致性：**
- `bizType()` / `buildPayload(String)` / `headType()` / `loadPage(P, PageRequest)` 在 Task 2 定义，Task 3 使用，签名一致 ✓
- `newPageRequest(current, size)` 在 Task 3 Step 3 定义并使用 ✓
- `PageRequest.setCurrent/setPageSize`（Lombok @Setter）✓；`Pagination.getPages()`（Lombok @Getter）✓
- `Context.start` / `finish` 在 Task 1 定义（public），Task 3 使用 ✓
- 门面 `write(...)` 两个重载在 Task 2 定义，Task 3/4 间接调用 ✓

## 不在本次范围

多 sheet、模板填充（`fill`）、异步加载、列宽/样式/合并单元格定制、CSV 专项测试（见 spec「YAGNI」）。
