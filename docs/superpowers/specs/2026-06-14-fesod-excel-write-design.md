# cookbook-fesod Excel 写功能设计

> **修订记录**：本设计在初版（注册表 + 静态门面 + 框架分页）基础上，按反馈重构为
> "纯抽象类 + 实例 write + 子类自管分页"。下方为最终设计，与代码一致。

## 目标

在 `cookbook-fesod` 模块实现一个可扩展的 Excel 同步写功能：
- 子类继承抽象类 `WriteExecutor<P, D>`，声明业务类型、定义请求对象与行 Bean、实现数据加载。
- 外部负责"按业务类型定位具体子类"（与本抽象类无关），拿到子类实例后调用其 `write(...)`。
- 框架负责把外部传入的字符串参数转换为请求对象，循环加载数据并逐批写入 Excel，
  提供"写入 OutputStream"与"临时文件 + 用完即删"两种输出方式。

## 约束

- 纯 Java、零 Spring 依赖。
- `WriteExecutor` 只定义子类契约 + 自己的 `write` 实现，**不负责定位/注册/查表**。
- 数据加载（含分页）完全由子类实现；框架只按"返回空 List = 无更多数据"驱动循环。
- Excel 写入基于 `fesod-sheet`（EasyExcel/FastExcel 更名版），逐批 `writer.write(list, sheet)` + `finish()`。
- Java 21、slf4j、JUnit 5。**不依赖 `cookbook-payload`**（loadData 返回裸 `List<D>`）。

## 核心决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 定位子类 | 外部负责，抽象类不参与 | 抽象类只管自身业务逻辑 |
| `bizType()` | 保留为抽象方法 | 供外部读取以构建定位映射；本身不做分发 |
| 数据加载 | `List<D> loadData(P)`，空 = 结束 | 分页/游标完全由子类内部处理 |
| 表头来源 | 子类 `headType()` 返回 `Class<D>` | fesod 据此推断列结构 |
| `write` | 实例方法（OutputStream + 临时文件两种） | 外部拿到实例后直接调用 |
| `Context` | 保留，统计对象（耗时、数据量、可扩展） | 后续可扩展更多统计维度 |

## 架构

```
外部                    拿到具体子类实例 executor（如何定位与抽象类无关）
  │                              │
  │  executor.write(bizParams, out)
  │─────────────────────────────►│ ① buildPayload(bizParams) → P   [子类转换字符串]
  │                              │ ② Context.start(bizType())       [开始统计]
  │                              │ ③ while: loadData(P) → List<D>   [子类自管分页]
  │                              │          空 → 结束；否则 writer.write(batch) + ctx.addRows(n)
  │                              │ ④ writer.finish()
  │                              │ ⑤ ctx.finish()                   [统计耗时 + 摘要日志]
```

- `P` = 请求对象（子类把外部字符串转换而来）
- `D` = Excel 行 Bean（字段标注 `@ExcelProperty`）

## 文件结构

```
src/main/java/io/ituknown/fesod/
├── Context.java                       // 写入统计对象（耗时、数据量、可扩展）
└── write/
    ├── WriteBizTypeEnums.java         // 业务类型枚举（应用侧扩展；外部定位用）
    ├── WriteExecutor.java             // 抽象基类：契约 + 实例 write
    └── ExampleWriteExecutor.java      // Demo：演示子类写法，loadData 返回空

src/test/java/io/ituknown/fesod/write/
└── WriteExecutorTest.java             // 流式多页/整除、临时文件用完即删、Demo 冒烟
```

## 子类契约

```java
public abstract class WriteExecutor<P, D> {

    /** 业务类型，供外部定位子类 */
    public abstract WriteBizTypeEnums bizType();

    /** 将外部传入的 bizParams 字符串转换为请求对象 P */
    public abstract P buildPayload(String bizParams);

    /** Excel 行 Bean 的 Class（fesod 据此推断表头列结构） */
    public abstract Class<D> headType();

    /** 加载一批数据；返回空 List 表示无更多数据。分页/游标由子类内部处理。 */
    public abstract List<D> loadData(P payload);

    /* 可选覆盖（带默认值） */
    public String sheetName()        { return "Sheet1"; }
    public ExcelTypeEnum excelType() { return ExcelTypeEnum.XLSX; }

    /* write（实例方法） */
    public void write(String bizParams, OutputStream out);
    public void write(String bizParams, Consumer<Path> fileConsumer);
}
```

## write 内部循环

```java
public void write(String bizParams, OutputStream out) {
    P payload = buildPayload(bizParams);
    Context ctx = Context.start(bizType());

    try (ExcelWriter writer = FastExcel.write(out, headType())
            .excelType(excelType())
            .build()) {
        WriteSheet sheet = FastExcel.writerSheet()
                .sheetName(sheetName())
                .build();
        while (true) {
            List<D> batch = loadData(payload);   // 子类自管分页/游标
            if (batch.isEmpty()) {
                break;                            // 无更多数据 → 结束
            }
            writer.write(batch, sheet);
            ctx.addRows(batch.size());
        }
        writer.finish();
    }

    ctx.finish();
}
```

**终止条件**：`loadData` 返回空 List。**注意**：若首次即返回空（无任何数据写入），
fesod 不会创建 sheet，产出的是无 sheet 的工作簿——属于 fesod 惰性行为，框架不额外处理（YAGNI）。

## 两种输出模式

```java
// 模式 A：写入调用方传入的 OutputStream
executor.write(bizParams, outputStream);

// 模式 B：框架生成临时文件，写完后回调给调用方，回调返回后立即删除
executor.write(bizParams, path -> { /* 读/上传 path */ });
```

模式 B 实现：创建临时文件 → `deleteOnExit` 兜底 → 复用模式 A 写入 → `consumer.accept(temp)` → finally `deleteIfExists`（失败仅 warn，不掩盖业务异常）。

## Context（统计对象）

非 final、字段 `protected`，便于后续扩展：

```java
public class Context {
    protected final WriteBizTypeEnums bizType;
    protected final long startTime;
    protected long rows;        // 已写入数据量
    protected long elapsedMs;   // finish 时计算

    public static Context start(WriteBizTypeEnums bizType);
    public void addRows(int n);
    public void finish();       // 计算 elapsedMs 并打印摘要日志
    // getters: getBizType / getRows / getElapsedMs
}
```

扩展方式：直接添加 `protected` 字段并在 `finish()` 中输出，或继承本类重写 `finish()`。

## 异常处理

- `buildPayload` / `loadData` 抛出的运行时异常原样上抛；fesod 的 `ExcelRuntimeException` 直接透传。
- 临时文件删除失败：仅 `warn` 日志，不掩盖业务异常（finally 中单独捕获）。

## 测试覆盖（JUnit 5）

1. **流式多页**（total 7，batch 3 → 3+3+1）：fesod 读回校验 7 行 + 内容。
2. **流式整除**（total 6 → 3+3）：校验末页满页正确终止。
3. **临时文件模式**：回调内文件存在且可读 5 行；回调返回后文件已删除。
4. **Demo 冒烟**：`ExampleWriteExecutor`（loadData 返回空）写入不报错、产出有效文件。

## 不在本次范围（YAGNI）

- 多 sheet、模板填充（`fill`）、异步加载、列宽/样式/合并单元格定制。
- 空数据产出 header-only sheet（fesod 默认不创建 sheet；如需可后续在空数据时写一个空批）。
- "按业务类型定位子类"的具体机制（注册表/枚举映射等）——交由外部。
