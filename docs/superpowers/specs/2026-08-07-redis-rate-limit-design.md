# cookbook-redis 限流模块设计

## 背景与目标

基于 Redis `CL.THROTTLE` 命令（Dragonfly 内置兼容，已实测可用）实现限流。模块 `cookbook-redis` 已引入 Redisson 4.6.1，并提供两层能力：

- 核心层：直接调用 `CL.THROTTLE` 的执行器，不依赖 Spring，可单独使用。
- 声明式层：Spring 注解 `@RateLimit` + AOP 切面，方法级声明式限流。

## 关键事实（已验证）

- Dragonfly（本地 127.0.0.1:6379，无认证）原生支持 `CL.THROTTLE`，返回格式与 redis-cell 一致。实测 `CL.THROTTLE test:rl 3 5 10 1` 返回 `[0, 4, 3, -1, 3]`：第 1 位 `0` 表示放行；`limit = maxBurst + 1`；`remaining` 为剩余令牌；`retryAfter = -1` 表示未被限流；`resetAfter` 为恢复满桶秒数。该命令不在 `MODULE LIST` 中，是 Dragonfly 内置实现。
- Redisson 4.6.1 在 Maven Central 有效（4.x 新分支，最新 4.7.0）。
- Lombok 由根 pom 以 `provided` 全局提供，子模块继承。

## 范围与决策

| 维度 | 决策 |
| --- | --- |
| 功能范围 | 含声明式限流：核心 API + 注解 + AOP |
| 限流 Key | SpEL 表达式，不填时退化为「类名#方法名」 |
| Key 拼接 | 始终带「类名#方法名」前缀；填写 key 时再追加 SpEL 求值结果，形如 `LoginService#login#42` |
| 超限行为 | 抛 `RateLimitExceededException`，携带 retryAfter 等信息 |
| Redis 故障 | fail-open：放行并告警，保障主流程可用 |
| 装配方式 | 手动 `@EnableRateLimit` 导入配置类，不使用 Spring Boot 自动装配 |
| RedissonClient | 由应用自行提供 Bean，模块不绑定客户端装配方式 |

## 架构

调用链：被标注的方法经由 `RateLimitAspect` 拦截，解析出最终 Key，调用 `RateLimiter.tryAcquire` 执行 `CL.THROTTLE`，返回 `ThrottleResult`；放行则继续原方法，超限则抛异常，Redis 异常则 fail-open 放行。

核心层与 Spring 无关；声明式层依赖核心层。现有 `RateLimitUtils` 的逻辑并入 `RateLimiter`，`RateLimitUtils` 删除。

## 核心层

### ThrottleResult

改为不可变 `record`（JDK 21），移除原有对 final 字段无效的 `@Setter`：

```java
public record ThrottleResult(boolean allowed, long limit, long remaining,
                             long retryAfter, long resetAfter) {
    public static ThrottleResult from(List<Long> raw) { ... }
}
```

`from` 解析规则：`raw.get(0) == 0` 表示放行，其余位按顺序映射。

### RateLimiter

Spring Bean，构造注入 `RedissonClient`：

```java
public class RateLimiter {
    public ThrottleResult tryAcquire(String key, int maxBurst, int count,
                                     int period, int quantity) { ... }
}
```

行为约定：

- 纯执行语义。放行或限流只体现在返回值的 `allowed` 字段。
- Lua 脚本启动时通过 `scriptLoad` 取 SHA，调用优先走 `evalSha`；遇到 `NOSCRIPT` 回退为 `eval`，避免热路径每次全量下发脚本。
- Redis 故障直接抛出异常。fail-open 策略由切面承担，执行器本身不做静默吞掉。

Lua 脚本透传参数给 `CL.THROTTLE`：`return redis.call('CL.THROTTLE', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])`。

## 声明式层

### 注解 @RateLimit

```java
@Target(METHOD)
@Retention(RUNTIME)
public @interface RateLimit {
    String key() default "";   // SpEL，可选
    int count();               // 必填：周期内配额
    int period() default 1;    // 周期（秒）
    int maxBurst() default -1; // -1 表示取 count 作为突发上限
    int quantity() default 1;  // 本次消耗配额
}
```

`count` 不设默认值，强制声明配额。`maxBurst` 默认取 `count`，对应 limit = maxBurst + 1。

### Key 解析 SpelKeyResolver

- 前缀取简单类名（不含包名）与方法名，形如 `LoginService#login`。
- `key` 为空，最终 Key = 「简单类名#方法名」。
- `key` 非空，最终 Key = 「简单类名#方法名#SpEL 求值结果」，例如 `LoginService#login#42`。
- 实现使用 Spring `SpelExpressionParser` 与 `MethodBasedEvaluationContext`，解析后的 `Expression` 按 key 文本缓存复用。

### 切面 RateLimitAspect

`@Around` 拦截带 `@RateLimit` 的方法：

1. 读取注解参数，计算 `maxBurst`（-1 取 count）。
2. 解析最终 Key。
3. 在 try 中调用 `rateLimiter.tryAcquire`；捕获 Redis 异常，记日志后 fail-open，直接放行原方法。fail-open 的 catch 只覆盖本次调用，不影响后续超限判定逻辑。
4. 正常拿到结果后，若 `allowed == false`，抛 `RateLimitExceededException`（携带 key、limit、remaining、retryAfter、resetAfter）。该抛出在 try/catch 之外，不会被 fail-open 的 catch 误吞。
5. 否则放行原方法。

### 异常 RateLimitExceededException

继承 `RuntimeException`，携带限流上下文（key、limit、remaining、retryAfter、resetAfter）。业务侧用全局异常处理器捕获，映射成 HTTP 429 与 `Retry-After` 响应头。

### 装配 @EnableRateLimit

```java
@Target(TYPE)
@Retention(RUNTIME)
@Import(RateLimitConfiguration.class)
public @interface EnableRateLimit {}
```

`RateLimitConfiguration`（`@Configuration`）注册三个 Bean：`RateLimiter`、`SpelKeyResolver`、`RateLimitAspect`。应用需自行提供一个 `RedissonClient` Bean。不使用 Spring Boot 自动装配，不生成 `AutoConfiguration.imports`。

## 依赖变化

`cookbook-redis/pom.xml` 新增两条编译依赖，版本放模块 properties（与 caffeine 模块惯例一致）：

- `org.springframework:spring-context`
- `org.aspectj:aspectjweaver`

Redisson 4.6.1 与 Lombok（根 pom provided）不变。

## 测试策略

仓库现有测试均为无外部依赖的纯单测，沿用该约定。

可离线运行（纳入 `mvn test`）：

- `ThrottleResultTest`：结果解析、边界值。
- `SpelKeyResolverTest`：空 key 退化、带 key 追加、SpEL 读入参。
- `RateLimitAspectTest`：以 mock 的 `RateLimiter` 验证放行、超限抛异常、Redis 异常 fail-open 三条路径。

真机验证（assumption 守卫）：

- `RateLimiterLiveTest`，开头 `Assumptions.assumeTrue(能连上 127.0.0.1:6379)`，连不上自动跳过而非失败。对接本地 Dragonfly 实跑：前 N 次放行、第 N+1 次被拒且 retryAfter 不小于 0、等待后令牌恢复。

## 文件清单

新增：

- `src/main/java/io/ituknown/redis/RateLimiter.java`
- `src/main/java/io/ituknown/redis/RateLimitExceededException.java`
- `src/main/java/io/ituknown/redis/annotation/RateLimit.java`
- `src/main/java/io/ituknown/redis/annotation/EnableRateLimit.java`
- `src/main/java/io/ituknown/redis/config/RateLimitConfiguration.java`
- `src/main/java/io/ituknown/redis/support/SpelKeyResolver.java`
- `src/main/java/io/ituknown/redis/aspect/RateLimitAspect.java`
- 对应测试 4 个

修改：

- `ThrottleResult.java` 改为 record。
- `pom.xml` 新增依赖。

删除：

- `RateLimitUtils.java`（逻辑并入 `RateLimiter`）。

## 注释与风格约定

遵循全局规范：注释只描述业务处理逻辑，不出现 Java 标识符、SQL/数据库信息或 `{@code}`；精简、自文档化。不照搬 `CaffeineUtils` 的冗长 Javadoc 风格。Markdown 正文不使用长破折号与横线分隔符。
