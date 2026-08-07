# cookbook-redis

基于 Redis `CL.THROTTLE` 命令的声明式限流模块，提供 Spring 方法级注解 `@RateLimit`，可按方法入参维度做精细化配额控制。底层采用 GCRA（通用信元速率算法），兼容两种后端：

- Dragonfly：原生内置 `CL.THROTTLE`，开箱即用；
- Redis：需加载 redis-cell 模块。

判定状态全部存于服务端，多实例共享同一配额桶，天然适合分布式部署。

## 模块组成

| 组件 | 职责 |
|------|------|
| `@EnableRateLimit` | 开启声明式限流，导入装配并启用方法代理 |
| `@RateLimit` | 方法级限流声明，配置配额与维度 |
| `RateLimitAspect` | 切面：解析维度、申请配额、超限抛异常、故障开放 |
| `SpelKeyResolver` | 按 SpEL 表达式解析限流键 |
| `RateLimiter` | 限流执行器，下发 `CL.THROTTLE` 并解析结果 |
| `ThrottleResult` | 限流判定结果 |
| `RateLimitExceededException` | 超限异常，携带重试等待秒数等上下文 |

## 限流原理：CL.THROTTLE 与 GCRA

### 命令格式

```
CL.THROTTLE key max_burst count period [quantity]
```

五项入参依次为：限流键、最大突发量、周期配额、周期秒数、本次申请配额数。算法按 GCRA 维护一个虚拟漏桶：

- 持续速率 = `count / period`（每秒补充的配额）；
- 桶总容量 = `max_burst + 1`（可瞬时透支的额度上限）。

举例：`count=5 period=1 max_burst=3` 表示持续速率每秒 5 个、桶容量 4 个，瞬时最多放行 4 次请求，第 5 次被限流，随后按每秒 5 个的速率补充。

### 返回五元组

命令返回五个整数，本模块封装为 `ThrottleResult`：

| 序号 | 原始含义 | 对应字段 |
|------|----------|----------|
| 1 | 是否被限流（1=被限流，0=放行） | `allowed`（已取反为布尔） |
| 2 | 桶总容量（`max_burst + 1`） | `limit` |
| 3 | 桶内剩余配额 | `remaining` |
| 4 | 重试等待秒数（放行时为 -1） | `retryAfter` |
| 5 | 恢复满桶等待秒数 | `resetAfter` |

### 下发细节

`CL.THROTTLE` 的键与参数必须以纯文本字节下发；Redisson 默认编解码器会把数值编为非文本字节导致解析失败，因此 `RateLimiter` 改用字符串编解码，所有参数统一转成文本。此外脚本（`EVALSHA`）首次下发时登记换取指纹，后续按指纹复用以减少网络开销；服务端重启导致指纹失效时自动回退全量下发。

## 使用方式

### 1. 提供连接并开启限流

模块本身不创建连接，需要应用提供一个 `RedissonClient` Bean，再用 `@EnableRateLimit` 开启：

```java
@Configuration
@EnableRateLimit
public class RedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        return Redisson.create(config);
    }
}
```

### 2. 在方法上声明限流

```java
@RateLimit(count = 5, period = 1)
public void create(String userId) {
    // ...
}
```

`@RateLimit` 属性说明：

| 属性 | 默认 | 含义 |
|------|------|------|
| `count` | 必填 | 单个周期内允许的配额数 |
| `period` | 1 | 配额统计周期（秒） |
| `maxBurst` | -1 | 最大突发量；桶总容量 = maxBurst + 1；为 -1 时取 count |
| `quantity` | 1 | 单次请求消耗的配额数（用于加权限流） |
| `key` | "" | 限流维度的 SpEL 表达式，见下节 |

### 3. 按维度细分（key 的 SpEL 解析）

`key` 是一条 Spring 表达式语言（SpEL）表达式，决定按什么粒度独立计数。最终 Redis 键由三段拼接：`简单类名#方法名[#求值结果]`。解析步骤：

1. 固定前缀「简单类名#方法名」，作为命名空间避免不同接口维度撞键；
2. `key` 非空时，在调用上下文中求值，以 `#参数名` 引用入参；
3. 求值结果拼到前缀之后；结果为 `null` 时退化为方法级（仅前缀），所有求值为空的调用共享同一桶。

用法示例：

```java
// 方法级：该接口所有调用共享配额
@RateLimit(count = 100, period = 60)

// 调用方级：按用户标识分别计数
@RateLimit(count = 5, key = "#userId")

// 对象入参：导航到对象属性（需有对应 getter）
@RateLimit(count = 5, key = "#req.userId")

// 组合维度：多标识拼接为一个维度
@RateLimit(count = 5, key = "#req.userId + ':' + #req.tenantId")
```

> 提示：按入参名引用需编译期保留参数名（`-parameters`），本模块已在 pom 中开启。对象属性导航按 JavaBean 规则取值；若表达式引用了不存在的入参会抛求值异常，且不在「故障开放」范围内，建议为维度表达式补充求值单测。

### 4. 处理超限

配额耗尽时切面抛出 `RateLimitExceededException`，携带 `retryAfter`（重试等待秒数）、`remaining`、`resetAfter` 等，便于上层映射为 HTTP 429 与 `Retry-After` 响应头：

```java
@ExceptionHandler(RateLimitExceededException.class)
public ResponseEntity<Map<String, Object>> onLimited(RateLimitExceededException e) {
    Map<String, Object> body = Map.of(
            "error", "rate_limited",
            "retryAfter", e.getRetryAfter());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(e.getRetryAfter()))
            .body(body);
}
```

### 故障开放与异常边界

- 故障开放：服务端不可用或 `CL.THROTTLE` 下发抛错时，切面仅记录告警并放行请求，避免限流组件故障阻断业务主流程。
- 维度解析不在此保护范围内：`key` 表达式求值异常会直接冒泡到调用方。

## 快速验证（Docker）

用 Dragonfly 容器一键起一个原生支持 `CL.THROTTLE` 的实例：

```bash
docker run -d -p 6379:6379 --ulimit memlock=-1 --dbnum=1 --name dragonfly docker.dragonflydb.io/dragonflydb/dragonfly
```

启动后即可运行模块自带的真机测试（连接 `redis://127.0.0.1:6379`，不可达时自动跳过）：

```bash
mvn -pl cookbook-redis test -Dtest=RateLimitLiveTest
```

也可以连进实例手动验证命令：

```bash
docker exec -it dragonfly redis-cli CL.THROTTLE demo 3 5 1 1
```

## 依赖与编译要求

- Redisson：提供 `RedissonClient` 与脚本下发能力；
- Spring：spring-context 与 AspectJ weaver，启用方法代理；
- 编译需保留参数名：`-parameters`（模块 pom 已配置 `maven.compiler.parameters=true`）；
- 后端：Dragonfly 原生支持；Redis 需加载 redis-cell 模块。
