# ban-package-maven-plugin

一个用于治理依赖使用的 Maven 插件：在构建期扫描项目已编译的字节码，一旦发现代码引用了被禁用的包或类，就让构建失败（或仅告警）。

判定基于**字节码中的实际引用**，而不是源码里的导入语句，因此无论通过继承、字段、方法参数、方法体调用、注解、方法引用还是泛型签名触发的引用，都能被发现；未出现在字节码里的导入（例如仅 import 但完全未使用）不会被误报。

## 功能特性

- 字节码级检测，覆盖父类/接口、字段与方法签名、方法体指令、声明与类型注解、方法引用、泛型签名等全部引用点。
- 双重匹配：按**包前缀**匹配整族类型，也可按**精确类**匹配单个类型。
- 两种检查范围：`PROJECT` 只查项目自身编译产物，`GLOBAL` 额外把依赖 jar 纳入扫描。
- 基线清单 + 范围叠加清单的配置模型，同一份配置可按范围切换生效规则。
- 命中时可中断构建，也可设为仅告警；支持命令行临时切换范围或跳过。

## 快速开始

插件坐标：

```xml
<groupId>io.ituknown</groupId>
<artifactId>ban-package-maven-plugin</artifactId>
<version>1.3.0</version>
```

> 版本号跟随本仓库的 `${revision}`，在仓内其他模块引用时可写成 `${revision}`；仓外引用请使用上面发布出的具体版本。

最小配置：声明执行，并配置一个被禁包前缀。

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.ituknown</groupId>
            <artifactId>ban-package-maven-plugin</artifactId>
            <version>1.3.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <bannedPackages>
                    <bannedPackage>com.alibaba.fastjson</bannedPackage>
                </bannedPackages>
            </configuration>
        </plugin>
    </plugins>
</build>
```

`check` 目标默认绑定到测试类编译阶段（`process-test-classes`），所以执行 `mvn test`、`mvn verify`、`mvn package` 等常规命令时会自动运行。也可手动触发：

```bash
mvn io.ituknown:ban-package-maven-plugin:check
```

## 检查范围与工作原理

| 范围 | 扫描对象 | 典型用途 |
| --- | --- | --- |
| `PROJECT`（默认） | 项目自身主代码编译产物，按需含测试代码 | 阻止项目自身代码引入禁用库 |
| `GLOBAL` | 上述产物 + 全部依赖 jar | 排查整条依赖链是否触及禁用库 |

工作流程：收集待扫描根（主代码输出目录，按需含测试输出，全局范围下再纳入依赖产物） → 用 ASM 逐个读取类字节码并抽取其引用到的全部类型 → 用当前范围的有效禁用规则逐一匹配 → 命中即记录为一条违规。

> `PROJECT` 范围只检项目自身代码，依赖 jar 内部类型之间的相互调用不会被标记，避免把第三方库的内部实现误报为违规。

## 配置参数

| 参数 | 命令行属性 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `scope` | `ban.scope` | 枚举 | `PROJECT` | 检查范围，取 `PROJECT` 或 `GLOBAL` |
| `bannedPackages` | 无 | `List<String>` | 空 | 基线禁用包前缀清单，两种范围下都生效 |
| `bannedClasses` | 无 | `List<String>` | 空 | 基线禁用精确类清单，两种范围下都生效 |
| `projectBans` | 无 | `BanSet` | 空 | 项目级补充清单，**仅在全局范围下叠加** |
| `globalBans` | 无 | `BanSet` | 空 | 全局补充清单，**仅在项目级范围下叠加** |
| `scanTests` | `ban.scanTests` | `boolean` | `true` | 是否一并扫描测试编译产物 |
| `failOnViolation` | `ban.failOnViolation` | `boolean` | `true` | 命中时是否中断构建，关闭则仅告警 |
| `skip` | `ban.skip` | `boolean` | `false` | 是否跳过本次检查 |

## 匹配规则

- **包前缀**：配置 `com.alibaba` 会命中 `com.alibaba.fastjson.JSON`、`com.alibaba.fastjson.TypeReference` 等所有以该前缀开头的类型。匹配以包边界为准，不会把 `com.alibabax` 之类的前缀碰撞误判为命中。
- **精确类**：配置 `com.alibaba.fastjson.JSON` 只命中这一个类，不影响同包下的其他类。
- **何为一次引用**：继承、实现、字段类型、方法入参与返回值、抛出异常、方法体内的调用与实例化、声明注解与类型注解、方法引用/lambda 的目标、泛型签名中的类型参数、类字面量加载等，均算作引用。

## 配置模型：基线 + 范围叠加

清单分两层：基线清单（`bannedPackages` / `bannedClasses`）在两种范围下都生效；范围补充清单（`projectBans` / `globalBans`）按范围叠加。

叠加对应关系是**有意设计的反向映射**，并非笔误：

- 范围为 `PROJECT` 时，叠加的是 `globalBans`（全局补充清单）。
- 范围为 `GLOBAL` 时，叠加的是 `projectBans`（项目级补充清单）。

这样一份配置就能按范围表达「平时只查自身、且把更宽的全局禁项一起套上；做全局体检时再额外把更严的项目级禁项套上」的治理意图。

`BanSet` 的结构与顶层清单一致，内含包前缀与精确类两组：

```xml
<configuration>
    <!-- 基线：两种范围都生效 -->
    <bannedPackages>
        <bannedPackage>com.alibaba</bannedPackage>
    </bannedPackages>

    <!-- 仅在 PROJECT 范围叠加 -->
    <globalBans>
        <bannedPackages>
            <bannedPackage>org.apache.commons.lang3</bannedPackage>
        </bannedPackages>
        <bannedClasses>
            <bannedClass>org.apache.http.client.HttpClient</bannedClass>
        </bannedClasses>
    </globalBans>

    <!-- 仅在 GLOBAL 范围叠加 -->
    <projectBans>
        <bannedPackages>
            <bannedPackage>org.checkerframework</bannedPackage>
        </bannedPackages>
    </projectBans>
</configuration>
```

按上例，默认范围 `PROJECT` 下的有效规则为：基线 `com.alibaba` + 全局补充 `org.apache.commons.lang3` 与 `org.apache.http.client.HttpClient`；切到 `GLOBAL` 后有效规则变为：基线 `com.alibaba` + 项目级补充 `org.checkerframework`。

## 更多示例

禁用单个精确类：

```xml
<configuration>
    <bannedClasses>
        <bannedClass>com.alibaba.fastjson.JSON</bannedClass>
    </bannedClasses>
</configuration>
```

只告警、不中断构建（适合灰度推行）：

```xml
<configuration>
    <bannedPackages>
        <bannedPackage>com.alibaba.fastjson</bannedPackage>
    </bannedPackages>
    <failOnViolation>false</failOnViolation>
</configuration>
```

命令行临时切换范围或跳过，无需改配置：

```bash
# 本次按全局范围体检整条依赖链
mvn verify -Dban.scope=GLOBAL

# 本次只告警不中断
mvn verify -Dban.failOnViolation=false

# 本次跳过检查
mvn verify -Dban.skip=true
```

不扫描测试代码（只管主代码）：

```xml
<configuration>
    <scanTests>false</scanTests>
    <bannedPackages>
        <bannedPackage>com.alibaba.fastjson</bannedPackage>
    </bannedPackages>
</configuration>
```

## 命中输出样例

命中时每条违规打印一行，最后给出汇总；默认配置下随后抛出异常令构建失败。

```
[ERROR] 禁用包违规：io.ituknown.it.App 引用了被禁类型 io.ituknown.it.forbidden.Forbidden（命中规则 package=io.ituknown.it.forbidden，来源文件 App.java）
[ERROR] 发现 1 处禁用包引用
```

设为仅告警时，同样打印违规明细，但以 `WARN` 级别给出汇总且构建继续。

## 注意事项

- 检测依赖已编译的类文件，目标默认绑定到测试类编译阶段，确保扫描前类已生成；若手动调用目标，请在编译之后执行。
- 匹配在字节码层面进行，源码中删除导入语句但未真正去除使用并不会绕过检测；反之，仅导入而未使用的类型不会出现在字节码中，也不会被误报。
- `GLOBAL` 范围会遍历全部依赖 jar，大型项目耗时更长，建议按需启用。
