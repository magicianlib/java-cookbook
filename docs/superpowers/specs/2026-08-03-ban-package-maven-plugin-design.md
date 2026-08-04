# ban-package-maven-plugin 设计

## 1. 背景与目标

需要一个 Maven 插件，在构建期拦截**项目自身代码**对指定包 / 类的引用，命中即让构建失败。

典型场景：禁止项目代码使用 `com.alibaba.fastjson`（整包）或 `com.alibaba.fastjson.JSON`（单个类）。

关键约束：只关心**项目自己写的代码**，不去管引入的第三方依赖 jar 内部是否也调用了这些包——依赖内部调用与项目自身代码无关，必须避免误报。同时保留一个开关，可在需要时做包含依赖在内的全局审计。

## 2. 检测机制选型

采用 **ASM 字节码扫描**：编译后扫描本模块产出的 `.class` 文件常量池与指令，收集其引用到的所有类型，再与禁用规则匹配。

原理：项目代码一旦调用了被禁类型，自身编译出的 `.class` 就会留下该类型的引用；而依赖 jar 内部如何调用，不会出现在项目自身的 `.class` 里，因此天然不会误报。

相比扫 `import` 语句，字节码扫描能覆盖内联全限定名、方法调用、字段类型、强转、`instanceof` 等所有引用位点，与社区 `forbidden-apis` 插件同一思路。

## 3. 模块定位与坐标

| 项 | 值 |
|---|---|
| groupId | `io.ituknown` |
| artifactId | `ban-package-maven-plugin` |
| version | `${revision}`（随根 pom，当前 1.3.0） |
| packaging | `maven-plugin` |
| 代码包名 | `io.ituknown.ban` |
| goal | `check` |

挂在根 `java-cookbook` 下作为新模块，继承现有 parent（复用 groupId、版本、Java 21、flatten、compiler 配置）。调用方式：`mvn io.ituknown:ban-package-maven-plugin:check`。

## 4. 配置模型

规则分两层：**配置级 baseline**（任何 scope 都生效）与**作用域级叠加**（仅对应 scope 运行时额外生效）。生效规则 = baseline ∪ 当前 scope 的专属规则。

采用叠加语义：baseline 永远在，scope 专属块只能加不能减，避免因配置了某个 scope 块而意外丢掉 baseline 的禁用项。

块名与触发模式为**反向映射**（有意设计）：`globalBans` 在 `scope=PROJECT` 时叠加，`projectBans` 在 `scope=GLOBAL` 时叠加。

```xml
<configuration>
  <scope>PROJECT</scope>            <!-- 本次运行扫哪个范围 -->

  <!-- 配置级 baseline：PROJECT / GLOBAL 都生效 -->
  <bannedPackages>
    <bannedPackage>com.alibaba.fastjson</bannedPackage>
  </bannedPackages>
  <bannedClasses>
    <bannedClass>com.alibaba.fastjson.JSON</bannedClass>
  </bannedClasses>

  <!-- 可选：仅 scope=GLOBAL 时额外叠加（块名与模式反向，有意设计） -->
  <projectBans>
    <bannedPackages>
      <bannedPackage>org.bouncycastle</bannedPackage>
    </bannedPackages>
  </projectBans>

  <!-- 可选：仅 scope=PROJECT 时额外叠加 -->
  <globalBans>
    <bannedPackages>
      <bannedPackage>org.joda.time</bannedPackage>
    </bannedPackages>
  </globalBans>

  <scanTests>true</scanTests>
  <failOnViolation>true</failOnViolation>
  <skip>false</skip>
</configuration>
```

### 参数清单

| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `scope` | enum | `PROJECT` | 扫描范围：`PROJECT` 仅本模块产物 / `GLOBAL` 含依赖 jar |
| `bannedPackages` | `List<String>` | 空 | 配置级禁包前缀（含子包），任何 scope 生效 |
| `bannedClasses` | `List<String>` | 空 | 配置级禁精确类，任何 scope 生效 |
| `projectBans` | `BanSet`（可选） | 空 | 仅 `scope=GLOBAL` 时叠加的禁包 / 禁类 |
| `globalBans` | `BanSet`（可选） | 空 | 仅 `scope=PROJECT` 时叠加的禁包 / 禁类 |
| `scanTests` | boolean | `true` | 是否扫测试类 |
| `failOnViolation` | boolean | `true` | 命中是否失败构建；`false` 仅打印告警 |
| `skip` | boolean | `false` | 跳过整个检查（`-Dban.skip=true`） |

`BanSet` 为简单 POJO：`{ List<String> bannedPackages; List<String> bannedClasses; }`，由 Maven 直接绑定嵌套结构。

`scope` 与 `bannedPackages` / `projectBans` / `globalBans` 支持命令行覆盖（`-Dban.scope=GLOBAL`、`-Dban.skip=true` 等）。

### 生效示例

| 本次 `scope` | 实际生效的禁用项 |
|---|---|
| `PROJECT` | baseline（`com.alibaba.fastjson` 包、`com.alibaba.fastjson.JSON` 类）+ globalBans（`org.joda.time`）|
| `GLOBAL` | baseline + projectBans（`org.bouncycastle`）|
| 不写 `globalBans` / `projectBans` | 两个 scope 都只用 baseline |

## 5. 检测与匹配逻辑

### 类型收集（`ClassFileScanner`）

用 ASM `ClassReader` 解析每个 `.class`，收集其引用到的所有类型内部名（`/` 分隔），覆盖以下引用位点：

- 本类自身、父类、实现的接口
- 字段类型（解析描述符）
- 方法签名（参数类型、返回类型）
- 方法体指令：`new` / `checkcast` / `instanceof` / `anewarray` 等类型指令、字段访问与方法调用的 owner、`ldc` 类常量、局部变量类型

### 规则匹配（`BanRule`）

被禁项以点号形式给出，统一转成 ASM 内部名后再比对。

- 禁包 `com.alibaba.fastjson` → 内部前缀 `com/alibaba/fastjson/`，命中所有 `startsWith` 该前缀的类型（含内部类 `JSON$Node`、子包）。
- 禁类 `com.alibaba.fastjson.JSON` → 内部名 `com/alibaba/fastjson/JSON`，**精确相等**才算（其内部类 `JSON$Node` 不命中，符合预期）。
- 原始类型忽略；数组类型经 `Type.getElementType()` 归一再判。

## 6. 扫描范围：PROJECT vs GLOBAL

- **PROJECT（默认）**：类根为 `${project.build.outputDirectory}`，`scanTests` 时再加 `${project.build.testOutputDirectory}`。只覆盖项目自身编译产物。
- **GLOBAL**：在 PROJECT 类根之外，再枚举 `project.getArtifacts()` 中每个依赖的 jar，逐个解压扫描其 `.class`。会把依赖内部对被禁包的调用一并报出，噪音大，适合一次性彻底审计；默认不开启。

GLOBAL 依赖 Mojo 声明 `requiresDependencyResolution = ResolutionScope.RUNTIME`，让 Maven 把依赖产物解析好备用。

范围澄清：GLOBAL 扫描的是**运行时类路径**上的依赖 jar（compile + runtime 作用域），不包含 test 作用域的依赖——test 依赖不参与部署，审计它无意义。`scanTests` 在 GLOBAL 下仅控制是否额外扫项目自身的 `target/test-classes`，不影响依赖扫描范围。

## 7. 构建阶段绑定

`defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES`：主类与测试类均已编译完成的最早阶段，一次绑定即可覆盖 `target/classes` + `target/test-classes`，并在 `package` 之前失败，符合「编译失败」体感。

注意点：`-Dmaven.test.skip=true` 会跳过 test-compile 整段，导致该阶段不执行、检查不跑。文档需写明：若要在跳过测试时仍强制检查，把 execution 改绑到 `process-classes`（仅查主代码）。

## 8. 失败与报告

扫描结束后汇总所有违规，逐条打印：

```
[ERROR] 禁用包违规：io/ituknown/xxx/FooService 引用了被禁类型 com/alibaba/fastjson/JSON
        命中规则：bannedPackage=com.alibaba.fastjson   来源文件：FooService.java
```

- `failOnViolation=true`（默认）且有违规 → 抛 `MojoExecutionException`，构建失败，附违规总数。
- `failOnViolation=false` → 以 `WARN` 级别打印，构建继续（灰度上线用）。

## 9. 模块结构与职责划分

```
ban-package-maven-plugin/
  pom.xml                              packaging=maven-plugin
  src/main/java/io/ituknown/ban/
    BanPackageMojo.java                @Mojo(name="check")：参数注入 + 编排，尽量薄
    BanCheck.java                      纯 POJO 核心：吃「类根列表 + 规则」→ 输出违规列表
    BanRule.java                       规则匹配：包前缀 / 精确类
    ClassFileScanner.java              ASM 扫描单个 .class，收集其引用到的所有类型
    Violation.java                     违规记录（来源类、命中规则、来源文件）
  src/test/java/io/ituknown/ban/       见第 11 节测试
```

设计原则：Mojo 尽量薄，把「扫描 + 匹配 + 汇总」逻辑放进不依赖 Maven API 的 POJO（`BanCheck`），核心逻辑用纯 JUnit 单测覆盖，避免依赖 maven-plugin-testing-harness。

## 10. 模块 pom 关键依赖

| 依赖 | scope | 用途 |
|---|---|---|
| `org.apache.maven:maven-plugin-api` | provided | `AbstractMojo`、`@Mojo` |
| `org.apache.maven.plugin-tools:maven-plugin-annotations` | provided | `@Mojo`、`@Parameter` |
| `org.apache.maven:maven-core` | provided | `MavenProject`、`Artifact` |
| `org.ow2.asm:asm`（9.x） | compile | 字节码扫描 |
| `org.junit.jupiter:junit-jupiter` | test | 已在根 BOM 管理 |

构建插件：`maven-plugin-plugin`（生成 `plugin.xml` 描述符），在模块 build 中声明。

## 11. 测试方案（JUnit 5）

- `BanRuleTest`：包前缀匹配、精确类匹配、内部类 `JSON$Node`、数组 / 原始类型、空配置等边界。
- `ClassFileScannerTest`：在 `src/test/java` 放一个**故意引用被禁类型**的 fixture 类（被禁类型用测试内 stub 类模拟，如 `io.ituknown.ban.testfix.FastjsonStub`），对 `target/test-classes` 下编译好的 fixture `.class` 跑扫描，断言命中。`test-compile` 先于 `test` 执行，故 fixture 已编译。
- `BanCheckTest`：给一组规则 + 类根，断言返回违规集合正确（多规则、多来源类、GLOBAL 含 jar 等场景）。
- Mojo 层不写重测，逻辑全在 POJO 覆盖。

## 12. 不做项（YAGNI）

- 正则匹配 / 复杂 include-exclude（前缀 + 精确类已够）。
- 反射调用（`Class.forName("com.alibaba...")`）的静态分析——字节码层面无法可靠识别。
- 自动生成 `<help>` goal、i18n 报错信息等锦上添花项。
