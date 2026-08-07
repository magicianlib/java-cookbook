# cookbook-redis 限流模块 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 Redis `CL.THROTTLE`（Dragonfly 内置兼容）实现两层限流能力：Spring 无关的核心执行器 + Spring 声明式注解限流。

**Architecture:** 核心层 `RateLimiter` 通过 Redisson 执行 Lua 包装的 `CL.THROTTLE`，返回 `ThrottleResult`；声明式层 `@RateLimit` 注解 + AOP 切面，按 SpEL 解析限流 Key，超限抛 `RateLimitExceededException`，Redis 故障 fail-open 放行，由 `@EnableRateLimit` 手动装配。

**Tech Stack:** Java 21、Redisson 4.6.1、Dragonfly（CL.THROTTLE）、Spring Framework 6.x、AspectJ、JUnit 5.12.2。

## Global Constraints

- JDK 21：`maven.compiler.release=21`，构建用系统 `mvn`（无 mvnw 包装）。
- 限流 Key 规则：前缀恒为「简单类名#方法名」；注解 `key` 非空时追加「#SpEL 求值结果」，如 `LoginService#login#42`；为空时仅为前缀。
- 超限行为：抛 `RateLimitExceededException`，携带 key、limit、remaining、retryAfter、resetAfter。
- Redis 故障：fail-open 放行并告警，保障主流程可用。
- 装配：`@EnableRateLimit` 手动导入配置类，不使用 Spring Boot 自动装配（不生成 AutoConfiguration.imports）。
- `RedissonClient` Bean 由应用自行提供，模块不绑定客户端装配方式。
- 注释只描述业务处理逻辑，不得包含 Java 标识符、SQL/数据库信息或 `{@code}`；Markdown 正文不使用长破折号与 `---` 横线分隔符。
- 依赖版本由根 pom 的 `spring-boot-dependencies` BOM（3.5.9）与 `junit-bom`（5.12.2）托管：`spring-context`、`aspectjweaver`、`spring-test`、`slf4j-api` 声明时一律不写版本。
- 执行期约束：每完成一个独立任务（单测 + 实现）必须暂停并向用户确认，再进入下一任务；单任务内不连续修改 2 个以上无关文件。

## File Structure

主代码（包根 `io.ituknown.redis`）：

- `ThrottleResult.java`（改为 record）：限流判定结果值对象，含 `from(List<?>)` 工厂。
- `RateLimitExceededException.java`（新建）：超限异常，携带判定结果。
- `RateLimiter.java`（新建）：核心执行器，封装 Redisson 调 CL.THROTTLE，替代并删除 `RateLimitUtils.java`。
- `annotation/RateLimit.java`（新建）：方法级注解，声明配额与可选 SpEL key。
- `annotation/EnableRateLimit.java`（新建）：装配入口，`@Import` 配置类。
- `config/RateLimitConfiguration.java`（新建）：注册三个 Bean 并开启 AspectJ 代理。
- `support/SpelKeyResolver.java`（新建）：按规则解析最终限流 Key。
- `aspect/RateLimitAspect.java`（新建）：`@Around` 切面，串联解析、判定、超限抛异常、fail-open。

删除：`RateLimitUtils.java`（逻辑并入 `RateLimiter`，Task 3 一并处理）。

测试（`src/test/java/io/ituknown/redis`）：

- `ThrottleResultTest`：结果解析（离线）。
- `RateLimitExceededExceptionTest`：异常携带字段（离线）。
- `RateLimiterLiveTest`：核心执行器对接本地 Dragonfly（assumption 守卫）。
- `support/SpelKeyResolverTest`：Key 拼接与 SpEL（离线）。
- `aspect/RateLimitAspectTest`：切面三条判定路径，用伪执行器（离线）。
- `config/EnableRateLimitTest`：`@EnableRateLimit` 装配校验（离线）。
- `RateLimitLiveTest`：注解 + AOP + 真实 Redis 全链路（assumption 守卫）。

`cookbook-redis/pom.xml`：新增 `spring-context`、`aspectjweaver`、`slf4j-api`（compile）与 `spring-test`（test），均不写版本。

## Task 1: ThrottleResult 改为不可变 record

**Files:**
- Modify: `cookbook-redis/src/main/java/io/ituknown/redis/ThrottleResult.java`
- Test: `cookbook-redis/src/test/java/io/ituknown/redis/ThrottleResultTest.java`

**Interfaces:**
- Consumes: 无。
- Produces: `record ThrottleResult(boolean allowed, long limit, long remaining, long retryAfter, long resetAfter)`；静态工厂 `static ThrottleResult from(List<?> raw)`（`raw.get(0)==0` 表示放行；元素按 Number 强转 long，兼容 Long/Integer 解码）。

- [ ] **Step 1: 写失败测试**

```java
package io.ituknown.redis;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrottleResultTest {

    @Test
    void from_allowed_decodes_all_fields() {
        ThrottleResult r = ThrottleResult.from(List.of(0L, 4L, 3L, -1L, 3L));

        assertTrue(r.allowed());
        assertEquals(4L, r.limit());
        assertEquals(3L, r.remaining());
        assertEquals(-1L, r.retryAfter());
        assertEquals(3L, r.resetAfter());
    }

    @Test
    void from_denied_flags_not_allowed_and_keeps_retryAfter() {
        ThrottleResult r = ThrottleResult.from(List.of(1L, 4L, 0L, 2L, 8L));

        assertFalse(r.allowed());
        assertEquals(2L, r.retryAfter());
    }

    @Test
    void from_accepts_integer_elements() {
        // 服务端可能把整数解码为 Integer，此处兼容
        ThrottleResult r = ThrottleResult.from(Arrays.asList(0, 4, 3, -1, 3));

        assertTrue(r.allowed());
        assertEquals(4L, r.limit());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-redis test -Dtest=ThrottleResultTest`
Expected: 编译失败或断言失败（旧 `ThrottleResult` 是带 `@Setter` 的普通类，无 `from`、无访问器 `allowed()`）。

- [ ] **Step 3: 改为 record 实现**

整体替换 `ThrottleResult.java`：

```java
package io.ituknown.redis;

import java.util.List;

/**
 * 限流判定结果。第 1 位为 0 表示放行，1 表示被限流；后续依次为桶容量、剩余令牌、
 * 重试等待秒数（-1 表示未被限流）、恢复满桶等待秒数。
 */
public record ThrottleResult(boolean allowed, long limit, long remaining,
                             long retryAfter, long resetAfter) {

    public static ThrottleResult from(List<?> raw) {
        return new ThrottleResult(
                asLong(raw.get(0)) == 0,
                asLong(raw.get(1)),
                asLong(raw.get(2)),
                asLong(raw.get(3)),
                asLong(raw.get(4)));
    }

    private static long asLong(Object element) {
        return ((Number) element).longValue();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-redis test -Dtest=ThrottleResultTest`
Expected: PASS（3 个测试）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-redis/src/main/java/io/ituknown/redis/ThrottleResult.java \
        cookbook-redis/src/test/java/io/ituknown/redis/ThrottleResultTest.java
git commit -m "refactor(redis): turn ThrottleResult into an immutable record" \
           -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 2: RateLimitExceededException 超限异常

**Files:**
- Create: `cookbook-redis/src/main/java/io/ituknown/redis/RateLimitExceededException.java`
- Test: `cookbook-redis/src/test/java/io/ituknown/redis/RateLimitExceededExceptionTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ThrottleResult`（访问器 `limit()`、`remaining()`、`retryAfter()`、`resetAfter()`）。
- Produces: `RateLimitExceededException extends RuntimeException`，构造 `RateLimitExceededException(String key, ThrottleResult result)`；访问器 `getKey()`、`getLimit()`、`getRemaining()`、`getRetryAfter()`、`getResetAfter()`。

- [ ] **Step 1: 写失败测试**

```java
package io.ituknown.redis;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitExceededExceptionTest {

    @Test
    void carries_result_fields_and_is_runtime() {
        ThrottleResult result = ThrottleResult.from(List.of(1L, 4L, 0L, 2L, 8L));

        RateLimitExceededException ex = new RateLimitExceededException("Foo#bar", result);

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("Foo#bar", ex.getKey());
        assertEquals(4L, ex.getLimit());
        assertEquals(0L, ex.getRemaining());
        assertEquals(2L, ex.getRetryAfter());
        assertEquals(8L, ex.getResetAfter());
        assertTrue(ex.getMessage().contains("Foo#bar"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-redis test -Dtest=RateLimitExceededExceptionTest`
Expected: 编译失败（类不存在）。

- [ ] **Step 3: 实现异常类**

```java
package io.ituknown.redis;

/**
 * 请求超出限流配额时抛出，携带限流上下文，便于上层映射为 429 与重试等待秒数。
 */
public class RateLimitExceededException extends RuntimeException {

    private final String key;
    private final long limit;
    private final long remaining;
    private final long retryAfter;
    private final long resetAfter;

    public RateLimitExceededException(String key, ThrottleResult result) {
        super("请求被限流; key=" + key + ", retryAfter=" + result.retryAfter() + "s");
        this.key = key;
        this.limit = result.limit();
        this.remaining = result.remaining();
        this.retryAfter = result.retryAfter();
        this.resetAfter = result.resetAfter();
    }

    public String getKey() {
        return key;
    }

    public long getLimit() {
        return limit;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getRetryAfter() {
        return retryAfter;
    }

    public long getResetAfter() {
        return resetAfter;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-redis test -Dtest=RateLimitExceededExceptionTest`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add cookbook-redis/src/main/java/io/ituknown/redis/RateLimitExceededException.java \
        cookbook-redis/src/test/java/io/ituknown/redis/RateLimitExceededExceptionTest.java
git commit -m "feat(redis): add RateLimitExceededException carrying throttle result" \
           -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 3: RateLimiter 核心执行器 + 真机测试，删除 RateLimitUtils

**Files:**
- Create: `cookbook-redis/src/main/java/io/ituknown/redis/RateLimiter.java`
- Delete: `cookbook-redis/src/main/java/io/ituknown/redis/RateLimitUtils.java`
- Test: `cookbook-redis/src/test/java/io/ituknown/redis/RateLimiterLiveTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ThrottleResult.from(List<?>)`；Redisson `RScript`（`eval`/`evalSha`/`scriptLoad`，`Mode.READ_WRITE`、`ReturnType.LIST`）。
- Produces: `RateLimiter(RedissonClient client)`；`ThrottleResult tryAcquire(String key, int maxBurst, int count, int period, int quantity)`。

说明：Redisson 仅能借 Lua eval 执行非标准命令，已用 RESP 实测「Lua 的 `redis.call` 调 CL.THROTTLE」在本地 Dragonfly 返回 `[0,4,3,-1,3]`，语义与 redis-cell 一致。真机测试用 `Assumptions.assumeTrue` 守卫：连不上 127.0.0.1:6379 自动跳过而非失败。

- [ ] **Step 1: 写真机测试（连不上则跳过）**

```java
package io.ituknown.redis;

import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterLiveTest {

    private static final String ADDRESS = "redis://127.0.0.1:6379";

    private static RateLimiter rateLimiter;
    private static RedissonClient client;

    @BeforeAll
    static void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress(ADDRESS);
        try {
            client = Redisson.create(config);
            client.getBucket("ratelimit:probe").delete(); // 探活，连不上会抛异常
        } catch (Exception e) {
            client = null;
        }
        Assumptions.assumeTrue(client != null, "本地 Dragonfly 不可用，跳过真机测试");
        rateLimiter = new RateLimiter(client);
    }

    @Test
    void allows_burst_then_denies_with_retryAfter() {
        String key = "live:" + UUID.randomUUID();

        // maxBurst=3 对应 limit=4，前 4 次放行
        for (int i = 0; i < 4; i++) {
            ThrottleResult allowed = rateLimiter.tryAcquire(key, 3, 5, 10, 1);
            assertTrue(allowed.allowed(), "第 " + (i + 1) + " 次应放行");
        }

        // 第 5 次被限流
        ThrottleResult denied = rateLimiter.tryAcquire(key, 3, 5, 10, 1);
        assertFalse(denied.allowed());
        assertTrue(denied.retryAfter() >= 0, "被限流时重试等待秒数应非负");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-redis test -Dtest=RateLimiterLiveTest`
Expected: 编译失败（`RateLimiter` 不存在）。

- [ ] **Step 3: 实现 RateLimiter**

```java
package io.ituknown.redis;

import java.util.List;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

/**
 * 限流执行器：把判定参数透传给限流命令，返回结构化结果。
 * 脚本指纹优先复用，失效时回退全量下发并重载指纹，减少热路径重复传输。
 */
public class RateLimiter {

    private static final String LUA_THROTTLE =
            "return redis.call('CL.THROTTLE', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])";

    private final RedissonClient client;
    private volatile String scriptSha;

    public RateLimiter(RedissonClient client) {
        this.client = client;
    }

    public ThrottleResult tryAcquire(String key, int maxBurst, int count, int period, int quantity) {
        // 限流命令按对象集合接收键，显式按对象构造，避免被推断成字符串集合而类型不符
        List<Object> keys = List.<Object>of(key);
        Object[] args = {maxBurst, count, period, quantity};

        String sha = scriptSha;
        if (sha != null) {
            try {
                List<?> result = client.getScript().evalSha(
                        RScript.Mode.READ_WRITE, sha, RScript.ReturnType.LIST, keys, args);
                return ThrottleResult.from(result);
            } catch (Exception ignored) {
                // 脚本指纹失效（如服务端重启清空），回退全量下发并在下方重载指纹
                scriptSha = null;
            }
        }

        List<?> result = client.getScript().eval(
                RScript.Mode.READ_WRITE, LUA_THROTTLE, RScript.ReturnType.LIST, keys, args);
        scriptSha = client.getScript().scriptLoad(LUA_THROTTLE);
        return ThrottleResult.from(result);
    }
}
```

- [ ] **Step 4: 删除被取代的 RateLimitUtils**

```bash
git rm cookbook-redis/src/main/java/io/ituknown/redis/RateLimitUtils.java
```

- [ ] **Step 5: 运行真机测试确认通过**

Run: `mvn -pl cookbook-redis test -Dtest=RateLimiterLiveTest`
Expected: 本地 Dragonfly 在线时 PASS；离线时输出 `assumption skipped`。

- [ ] **Step 6: 提交**

```bash
git add cookbook-redis/src/main/java/io/ituknown/redis/RateLimiter.java \
        cookbook-redis/src/test/java/io/ituknown/redis/RateLimiterLiveTest.java
git commit -m "feat(redis): add RateLimiter over CL.THROTTLE, drop RateLimitUtils" \
           -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 4: 引入 Spring 依赖 + @RateLimit 注解 + SpelKeyResolver

**Files:**
- Modify: `cookbook-redis/pom.xml`
- Create: `cookbook-redis/src/main/java/io/ituknown/redis/annotation/RateLimit.java`
- Create: `cookbook-redis/src/main/java/io/ituknown/redis/support/SpelKeyResolver.java`
- Test: `cookbook-redis/src/test/java/io/ituknown/redis/support/SpelKeyResolverTest.java`

**Interfaces:**
- Consumes: Spring SpEL（`SpelExpressionParser`、`MethodBasedEvaluationContext`、`DefaultParameterNameDiscoverer`）。
- Produces: 注解 `@RateLimit`（`key`、`count`、`period`、`maxBurst`、`quantity`）；`SpelKeyResolver.resolve(Method method, Object[] args, String keyExpression)` 返回最终 Key 字符串。

- [ ] **Step 1: 加 Spring / AspectJ / slf4j / spring-test 依赖**

在 `cookbook-redis/pom.xml` 的 `<dependencies>` 内、现有 redisson 与 junit 之间，加入（版本由根 BOM 托管，不写 version）：

```xml
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>

        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
```

并在 test 依赖区（junit 之后）加入：

```xml
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-test</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: 写 Key 解析失败测试**

```java
package io.ituknown.redis.support;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpelKeyResolverTest {

    static class Fixture {
        public void login(String userId) {
        }

        public void query(String userId, int page) {
        }
    }

    @Test
    void empty_key_falls_back_to_method_signature() throws Exception {
        Method method = Fixture.class.getMethod("login", String.class);

        assertEquals("Fixture#login", new SpelKeyResolver().resolve(method, new Object[]{"42"}, ""));
    }

    @Test
    void spel_key_appends_evaluated_value() throws Exception {
        Method method = Fixture.class.getMethod("login", String.class);

        assertEquals("Fixture#login#42", new SpelKeyResolver().resolve(method, new Object[]{"42"}, "#userId"));
    }

    @Test
    void spel_key_can_read_later_param() throws Exception {
        Method method = Fixture.class.getMethod("query", String.class, int.class);

        assertEquals("Fixture#query#3", new SpelKeyResolver().resolve(method, new Object[]{"u1", 3}, "#page"));
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `mvn -pl cookbook-redis test -Dtest=SpelKeyResolverTest`
Expected: 编译失败（`SpelKeyResolver`、`@RateLimit` 不存在）。

- [ ] **Step 4: 实现 @RateLimit 注解**

```java
package io.ituknown.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级限流。配额按周期计算，突发上限默认取配额数。
 * 表达式非空时按方法入参区分限流维度；为空时仅按方法签名限流。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key() default "";

    int count();

    int period() default 1;

    int maxBurst() default -1;

    int quantity() default 1;
}
```

- [ ] **Step 5: 实现 SpelKeyResolver**

```java
package io.ituknown.redis.support;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 限流维度解析：前缀为简单类名与方法名；表达式非空时追加求值结果。
 */
public class SpelKeyResolver {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
    private final ConcurrentHashMap<String, Expression> cache = new ConcurrentHashMap<>();

    public String resolve(Method method, Object[] args, String keyExpression) {
        String prefix = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        if (keyExpression == null || keyExpression.isEmpty()) {
            return prefix;
        }
        Expression expression = cache.computeIfAbsent(keyExpression, parser::parseExpression);
        MethodBasedEvaluationContext context =
                new MethodBasedEvaluationContext(null, method, args, discoverer);
        Object value = expression.getValue(context);
        return prefix + "#" + value;
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

Run: `mvn -pl cookbook-redis test -Dtest=SpelKeyResolverTest`
Expected: PASS（3 个测试）。

- [ ] **Step 7: 提交**

```bash
git add cookbook-redis/pom.xml \
        cookbook-redis/src/main/java/io/ituknown/redis/annotation/RateLimit.java \
        cookbook-redis/src/main/java/io/ituknown/redis/support/SpelKeyResolver.java \
        cookbook-redis/src/test/java/io/ituknown/redis/support/SpelKeyResolverTest.java
git commit -m "feat(redis): add Spring deps, @RateLimit annotation and SpEL key resolver" \
           -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 5: RateLimitAspect 切面 + 三路径测试

**Files:**
- Create: `cookbook-redis/src/main/java/io/ituknown/redis/aspect/RateLimitAspect.java`
- Test: `cookbook-redis/src/test/java/io/ituknown/redis/aspect/RateLimitAspectTest.java`

**Interfaces:**
- Consumes: Task 3 `RateLimiter.tryAcquire`；Task 4 `SpelKeyResolver.resolve`、`@RateLimit`；Task 2 `RateLimitExceededException`。
- Produces: `RateLimitAspect(RateLimiter, SpelKeyResolver)`；`@Around("@annotation(rateLimit)") Object around(ProceedingJoinPoint pjp, RateLimit rateLimit)`。

说明：放行/超限抛异常在 try/catch 之外，fail-open 的 catch 只覆盖限流判定调用，避免误吞超限异常。测试用继承 `RateLimiter` 的伪执行器，避开 Redis。

- [ ] **Step 1: 写切面三路径测试（伪执行器）**

```java
package io.ituknown.redis.aspect;

import io.ituknown.redis.RateLimitExceededException;
import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.ThrottleResult;
import io.ituknown.redis.annotation.RateLimit;
import io.ituknown.redis.support.SpelKeyResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RateLimitAspectTest.TestConfig.class)
class RateLimitAspectTest {

    @Autowired
    SampleService sampleService;

    @Test
    void allowed_request_proceeds() {
        assertEquals("ok-1", sampleService.allowed("1"));
    }

    @Test
    void denied_request_throws() {
        assertThrows(RateLimitExceededException.class, () -> sampleService.denied("x"));
    }

    @Test
    void redis_failure_fails_open() {
        assertEquals("ok-y", sampleService.broken("y"));
    }

    static class SampleService {
        @RateLimit(count = 5)
        public String allowed(String id) {
            return "ok-" + id;
        }

        @RateLimit(count = 5)
        public String denied(String id) {
            return "ok-" + id;
        }

        @RateLimit(count = 5)
        public String broken(String id) {
            return "ok-" + id;
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        SpelKeyResolver spelKeyResolver() {
            return new SpelKeyResolver();
        }

        @Bean
        RateLimiter rateLimiter() {
            return new RateLimiter(null) {
                @Override
                public ThrottleResult tryAcquire(String key, int maxBurst, int count, int period, int quantity) {
                    if (key.endsWith("#broken")) {
                        throw new RuntimeException("redis down");
                    }
                    if (key.endsWith("#denied")) {
                        return new ThrottleResult(false, 5, 0, 2, 8);
                    }
                    return new ThrottleResult(true, 5, 4, -1, 0);
                }
            };
        }

        @Bean
        RateLimitAspect rateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver) {
            return new RateLimitAspect(rateLimiter, keyResolver);
        }

        @Bean
        SampleService sampleService() {
            return new SampleService();
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-redis test -Dtest=RateLimitAspectTest`
Expected: 编译失败（`RateLimitAspect` 不存在）。

- [ ] **Step 3: 实现切面**

```java
package io.ituknown.redis.aspect;

import java.lang.reflect.Method;

import io.ituknown.redis.RateLimitExceededException;
import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.ThrottleResult;
import io.ituknown.redis.annotation.RateLimit;
import io.ituknown.redis.support.SpelKeyResolver;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 方法限流切面：解析限流维度后判定，放行则继续原方法，超限抛异常，判定异常按放行处理。
 */
@Aspect
public class RateLimitAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimiter rateLimiter;
    private final SpelKeyResolver keyResolver;

    public RateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver) {
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = keyResolver.resolve(method, pjp.getArgs(), rateLimit.key());
        int maxBurst = rateLimit.maxBurst() < 0 ? rateLimit.count() : rateLimit.maxBurst();

        ThrottleResult result;
        try {
            result = rateLimiter.tryAcquire(
                    key, maxBurst, rateLimit.count(), rateLimit.period(), rateLimit.quantity());
        } catch (Exception e) {
            LOGGER.warn("限流判定异常，按放行处理; key={}", key, e);
            return pjp.proceed();
        }

        if (!result.allowed()) {
            throw new RateLimitExceededException(key, result);
        }
        return pjp.proceed();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl cookbook-redis test -Dtest=RateLimitAspectTest`
Expected: PASS（3 个测试）。

- [ ] **Step 5: 提交**

```bash
git add cookbook-redis/src/main/java/io/ituknown/redis/aspect/RateLimitAspect.java \
        cookbook-redis/src/test/java/io/ituknown/redis/aspect/RateLimitAspectTest.java
git commit -m "feat(redis): add RateLimitAspect with fail-open guard" \
           -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 6: @EnableRateLimit 装配入口 + 配置类

**Files:**
- Create: `cookbook-redis/src/main/java/io/ituknown/redis/annotation/EnableRateLimit.java`
- Create: `cookbook-redis/src/main/java/io/ituknown/redis/config/RateLimitConfiguration.java`
- Test: `cookbook-redis/src/test/java/io/ituknown/redis/config/EnableRateLimitTest.java`

**Interfaces:**
- Consumes: Task 3 `RateLimiter`、Task 4 `SpelKeyResolver`、Task 5 `RateLimitAspect`；应用提供的 `RedissonClient`。
- Produces: `@EnableRateLimit`（`@Import(RateLimitConfiguration.class)`）；`RateLimitConfiguration`（`@Configuration` + `@EnableAspectJAutoProxy`）注册 `RateLimiter`、`SpelKeyResolver`、`RateLimitAspect`。

说明：配置类自带 `@EnableAspectJAutoProxy`，非 Spring Boot 场景下也能让 `@Aspect` 生效。装配测试提供一个 `Redisson.create()` 的惰性客户端（构造期不连 Redis），仅校验 Bean 注册，不调用限流方法，因此离线可跑。

- [ ] **Step 1: 写装配测试（仅校验 Bean 注册）**

```java
package io.ituknown.redis.config;

import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.annotation.EnableRateLimit;
import io.ituknown.redis.aspect.RateLimitAspect;
import io.ituknown.redis.support.SpelKeyResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EnableRateLimitTest.AppConfig.class)
class EnableRateLimitTest {

    @Autowired
    RateLimiter rateLimiter;
    @Autowired
    SpelKeyResolver spelKeyResolver;
    @Autowired
    RateLimitAspect rateLimitAspect;

    @Test
    void enable_registers_all_beans() {
        assertNotNull(rateLimiter);
        assertNotNull(spelKeyResolver);
        assertNotNull(rateLimitAspect);
    }

    @Configuration
    @EnableRateLimit
    static class AppConfig {

        @Bean(destroyMethod = "shutdown")
        RedissonClient redissonClient() {
            // 惰性客户端，构造期不连接；测试不触发限流调用，故离线可跑
            return Redisson.create();
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl cookbook-redis test -Dtest=EnableRateLimitTest`
Expected: 编译失败（`@EnableRateLimit`、`RateLimitConfiguration` 不存在）。

- [ ] **Step 3: 实现配置类**

```java
package io.ituknown.redis.config;

import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.aspect.RateLimitAspect;
import io.ituknown.redis.support.SpelKeyResolver;

import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 限流装配：注册执行器、维度解析器、切面，并开启方法代理。
 * 由 @EnableRateLimit 导入；RedissonClient 由应用提供。
 */
@Configuration
@EnableAspectJAutoProxy
public class RateLimitConfiguration {

    @Bean
    public RateLimiter rateLimiter(RedissonClient redissonClient) {
        return new RateLimiter(redissonClient);
    }

    @Bean
    public SpelKeyResolver spelKeyResolver() {
        return new SpelKeyResolver();
    }

    @Bean
    public RateLimitAspect rateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver) {
        return new RateLimitAspect(rateLimiter, keyResolver);
    }
}
```

- [ ] **Step 4: 实现 @EnableRateLimit**

```java
package io.ituknown.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.ituknown.redis.config.RateLimitConfiguration;

import org.springframework.context.annotation.Import;

/**
 * 开启声明式限流：导入限流装配，注册切面并启用 AspectJ 代理。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RateLimitConfiguration.class)
public @interface EnableRateLimit {
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -pl cookbook-redis test -Dtest=EnableRateLimitTest`
Expected: PASS。

- [ ] **Step 6: 提交**

```bash
git add cookbook-redis/src/main/java/io/ituknown/redis/annotation/EnableRateLimit.java \
        cookbook-redis/src/main/java/io/ituknown/redis/config/RateLimitConfiguration.java \
        cookbook-redis/src/test/java/io/ituknown/redis/config/EnableRateLimitTest.java
git commit -m "feat(redis): add @EnableRateLimit and RateLimitConfiguration wiring" \
           -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 7: 全链路真机测试 + 全量回归

**Files:**
- Test: `cookbook-redis/src/test/java/io/ituknown/redis/RateLimitLiveTest.java`

**Interfaces:**
- Consumes: Task 6 `@EnableRateLimit`；Task 4 `@RateLimit`（含 SpEL key）；Task 2 `RateLimitExceededException`；应用提供的真实 `RedissonClient`。
- Produces: 全链路验证：注解 + AOP + 真实 Redis 的端到端限流。

说明：每次运行用 UUID 作 SpEL 入参，获得独立桶，避免跨运行共享状态导致抖动。`@BeforeEach` 探活，连不上则跳过。

- [ ] **Step 1: 写全链路真机测试**

```java
package io.ituknown.redis;

import io.ituknown.redis.annotation.EnableRateLimit;
import io.ituknown.redis.annotation.RateLimit;

import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RateLimitLiveTest.AppConfig.class)
class RateLimitLiveTest {

    @Autowired
    LimitedService limitedService;
    @Autowired
    RedissonClient client;

    @BeforeEach
    void requireRedis() {
        Assumptions.assumeTrue(canReachRedis(), "本地 Dragonfly 不可用，跳过真机测试");
    }

    @Test
    void annotation_throttles_via_real_redis() {
        String id = UUID.randomUUID().toString();

        // 配额 5、突发上限 3 对应 limit=4，前 4 次放行
        for (int i = 0; i < 4; i++) {
            assertEquals("ok-" + id, limitedService.call(id));
        }
        // 第 5 次被限流
        assertThrows(RateLimitExceededException.class, () -> limitedService.call(id));
    }

    private boolean canReachRedis() {
        try {
            client.getBucket("ratelimit:probe").delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static class LimitedService {
        @RateLimit(key = "#id", count = 5, maxBurst = 3)
        public String call(String id) {
            return "ok-" + id;
        }
    }

    @Configuration
    @EnableRateLimit
    static class AppConfig {

        @Bean(destroyMethod = "shutdown")
        RedissonClient redissonClient() {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://127.0.0.1:6379");
            return Redisson.create(config);
        }

        @Bean
        LimitedService limitedService() {
            return new LimitedService();
        }
    }
}
```

- [ ] **Step 2: 运行全链路真机测试确认通过**

Run: `mvn -pl cookbook-redis test -Dtest=RateLimitLiveTest`
Expected: 本地 Dragonfly 在线时 PASS；离线时输出 `assumption skipped`。

- [ ] **Step 3: 全量回归**

Run: `mvn -pl cookbook-redis test`
Expected: 所有测试通过；离线环境下两个 Live 测试被跳过，其余 12 个断言全绿。

- [ ] **Step 4: 提交**

```bash
git add cookbook-redis/src/test/java/io/ituknown/redis/RateLimitLiveTest.java
git commit -m "test(redis): add end-to-end @RateLimit live test against Dragonfly" \
           -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## 收尾

- 运行 `mvn -pl cookbook-redis test` 全绿（离线时 Live 测试跳过）。
- 可选：在样例中演示 `@EnableRateLimit` + `@RateLimit(key="#userId", count=10)` 的用法，并在全局异常处理器里把 `RateLimitExceededException` 映射为 HTTP 429 与 `Retry-After` 头（不在本计划范围内，仅在 README 提及）。
