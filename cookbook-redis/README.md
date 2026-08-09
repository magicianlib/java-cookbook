# cookbook-redis

基于 Redis 的两个声明式功能：声明式限流（`@RateLimit`）与分布式锁（`@DistributedLock`），均提供 Spring
方法级注解，可按方法入参维度做精细化控制。所有状态存于服务端，多实例共享，天然适合分布式部署。

- 限流：基于 `CL.THROTTLE` 命令，采用 GCRA（通用信元速率算法）。Dragonfly 原生内置、开箱即用，Redis 需加载
  redis-cell 模块；
- 分布式锁：基于 Redisson 可重入锁，watchdog 自动续期，兼容任意 Redis/Dragonfly 实例。

下文先介绍限流（`@RateLimit`），再介绍分布式锁（`@DistributedLock`）。

## 模块组成

| 组件                            | 职责                            |
|-------------------------------|-------------------------------|
| `@EnableRateLimit`            | 开启声明式限流，导入装配并启用方法代理；`order` 配置切面顺序 |
| `@RateLimit`                  | 方法级限流声明，配置配额与维度               |
| `RateLimitAspect`             | 切面：解析维度、申请配额、超限抛异常、故障开放       |
| `RateLimiter`                 | 限流执行器，下发 `CL.THROTTLE` 并解析结果 |
| `ThrottleStatus`              | 限流状态                          |
| `RateLimitExceededException`  | 超限异常，携带重试等待秒数等上下文             |
| `@EnableDistributedLock`      | 开启声明式分布式锁，导入装配并启用方法代理；`order` 配置切面顺序 |
| `@DistributedLock`            | 方法级锁声明，配置锁键与等待、持锁时长           |
| `DistributedLockAspect`       | 切面：解析锁键、抢锁、释锁、抢不到抛异常、故障关闭     |
| `DistributedLockManager`      | 锁执行器，封装 Redisson 可重入锁的获取与释放   |
| `LockNotAcquiredException`    | 抢锁失败异常，携带锁键                   |
| `SpelKeyResolver`             | 按 SpEL 表达式解析限流键与锁键（两个功能共用）    |

## 限流原理：CL.THROTTLE 与 GCRA

### 命令格式

```
CL.THROTTLE key max_burst count period [quantity]
```

五项入参依次为：限流键、最大突发量、周期配额、周期秒数、本次申请配额数。算法按 GCRA 维护一个虚拟漏桶：

- 持续速率 = `count / period`（每秒补充的配额）；
- 桶总容量 = `max_burst + 1`（可瞬时透支的额度上限）。

举例：`count=5 period=1 max_burst=3` 表示持续速率每秒 5 个、桶容量 4 个，瞬时最多放行 4 次请求，第 5 次被限流，随后按每秒 5
个的速率补充。

### 返回五元组

命令返回五个整数，本模块封装为 `ThrottleStatus`：

| 序号 | 原始含义                  | 对应字段              |
|----|-----------------------|-------------------|
| 1  | 是否被限流（1=被限流，0=放行）     | `allowed`（已取反为布尔） |
| 2  | 桶总容量（`max_burst + 1`） | `limit`           |
| 3  | 桶内剩余配额                | `remaining`       |
| 4  | 重试等待秒数（放行时为 -1）       | `retryAfter`      |
| 5  | 恢复满桶等待秒数              | `resetAfter`      |

### 下发细节

`CL.THROTTLE` 的键与参数必须以纯文本字节下发；Redisson 默认编解码器会把数值编为非文本字节导致解析失败，因此 `RateLimiter`
改用字符串编解码，所有参数统一转成文本。此外脚本（`EVALSHA`）首次下发时登记换取指纹，后续按指纹复用以减少网络开销；服务端重启导致指纹失效时自动回退全量下发。

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

`@EnableRateLimit` 还提供 `order` 属性配置限流切面在调用链中的执行顺序，数值越小优先级越高（越靠外层执行）。默认最高优先级（最外层，`HIGHEST_PRECEDENCE + 1`）：限流尽量靠前，在打开事务、访问数据等耗资源操作之前就拒绝越界请求。默认不取字面 `HIGHEST_PRECEDENCE`（`Integer.MIN_VALUE`），原因见下文分布式锁「锁语义与故障边界」。若希望限流靠内层生效，可调大该值：

```java
@EnableRateLimit(order = 0)
```

### 2. 在方法上声明限流

```java
@RateLimit(count = 5, period = 1)
public void create(String userId) {
    // ...
}
```

`@RateLimit` 属性说明：

| 属性         | 默认 | 含义                                      |
|------------|----|-----------------------------------------|
| `count`    | 必填 | 单个周期内允许的配额数                             |
| `period`   | 1  | 配额统计周期（秒）                               |
| `maxBurst` | -1 | 最大突发量；桶总容量 = maxBurst + 1；为 -1 时取 count |
| `quantity` | 1  | 单次请求消耗的配额数（用于加权限流）                      |
| `key`      | "" | 限流维度的 SpEL 表达式，见下节                      |

### 3. 按维度细分（key 的 SpEL 解析）

`key` 是一条 Spring 表达式语言（SpEL）表达式，决定按什么粒度独立计数。最终 Redis 键由三段拼接：`简单类名#方法名[#求值结果]`
。解析步骤：

1. 固定前缀为 简单类名#方法名，作为命名空间避免不同接口维度撞键；
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

> 提示：按入参名引用需编译期保留参数名（`-parameters`），本模块已在 pom 中开启。对象属性导航按 JavaBean
> 规则取值；若表达式引用了不存在的入参会抛求值异常，且不在故障开放范围内，建议为维度表达式补充求值单测。

### 4. 处理超限

配额耗尽时切面抛出 `RateLimitExceededException`，携带 `retryAfter`（重试等待秒数）、`remaining`、`resetAfter` 等，便于上层映射为
HTTP 429 与 `Retry-After` 响应头：

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

## 分布式锁

基于 Redisson 可重入锁（`RLock`）的声明式分布式锁，提供方法级注解 `@DistributedLock`，可按方法入参维度
对临界区加锁，用于防重复提交、幂等控制、串行化共享资源等。底层锁状态存于 Redis，多实例共享，天然适合分布式部署。

分布式锁与限流可独立启用，互不依赖；两者共用同一 `RedissonClient` 与 `SpelKeyResolver` 键解析逻辑。

### 1. 提供连接并开启

与限流相同，模块本身不创建连接，由应用提供 `RedissonClient` Bean，再用 `@EnableDistributedLock` 开启：

```java
@Configuration
@EnableDistributedLock
public class RedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        return Redisson.create(config);
    }
}
```

`@EnableDistributedLock` 的 `order` 属性配置锁切面在调用链中的执行顺序，数值越小优先级越高（越靠外层执行）。
默认 `Ordered.HIGHEST_PRECEDENCE + 1`：分布式锁通常应包住事务等内层切面（抢锁在开事务之前、释放在提交之后），
故默认偏外层。默认不取 `HIGHEST_PRECEDENCE`（即 `Integer.MIN_VALUE`），原因见下文「锁语义与故障边界」。

```java
@EnableDistributedLock(order = 0)
```

### 2. 在方法上声明

```java
@DistributedLock(key = "#orderId")
public void pay(String orderId) {
    // 同一 orderId 任意时刻只有一个调用能进入
}
```

`@DistributedLock` 属性说明：

| 属性          | 默认   | 含义                                                      |
|-------------|------|---------------------------------------------------------|
| `key`       | `""` | 锁键的 SpEL 表达式，见下节                                       |
| `waitTime`  | `0`  | 最长等待获取时长；`0` 表示不等待，抢不到立即失败并抛 `LockNotAcquiredException` |
| `leaseTime` | `-1` | 持锁时长；`-1` 表示启用 watchdog 自动续期；为正数时到点自动释放且不再续期            |
| `timeUnit`  | 秒    | `waitTime` 与 `leaseTime` 的单位                           |

### 3. 按维度分锁（key 的 SpEL 解析）

`key` 决定按什么粒度加锁，解析规则与限流完全一致，最终锁键为 `简单类名#方法名[#求值结果]`。留空时同一方法的
所有调用争用一把锁；填表达式则按求值结果分别加锁，求值为 `null` 时退回方法级。

```java
// 方法级：该接口所有调用争用一把锁
@DistributedLock

// 调用方级：按订单标识分别加锁，同一订单串行
@DistributedLock(key = "#orderId")

// 对象入参：导航到对象属性（需有对应 getter）
@DistributedLock(key = "#req.orderId")
```

### 4. 处理抢锁失败

锁被他人持有时，切面抛出 `LockNotAcquiredException`（携带 `key`），由调用方决定降级策略，例如返回「请勿重复提交」：

```java
@ExceptionHandler(LockNotAcquiredException.class)
public ResponseEntity<Map<String, Object>> onLockFailed(LockNotAcquiredException e) {
    Map<String, Object> body = Map.of(
            "error", "lock_not_acquired",
            "key", e.getKey());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
}
```

### 锁语义与故障边界

- **可重入**：Redisson `RLock` 按线程可重入，同一线程在持锁期间可再次获取同一把锁，计数释放后才真正解锁。
- **watchdog 与 lease**：`leaseTime = -1`（默认）启用 watchdog，业务方法正常执行期间锁自动续期、不会因超时丢失；
  进程崩溃或显式关闭后按 `lockWatchdogTimeout`（默认 30s）自动释放，避免死锁。`leaseTime` 为正数时到点自动释放、
  不再续期，适合对执行时长有上限的场景。
- **释放健壮**：切面在 `finally` 中释放，且仅当锁由当前线程持有时才解锁；若持锁期间 lease 到期已自动释放，则跳过，
  避免误抛 `IllegalMonitorStateException`。
- **故障关闭（fail-closed）**：与限流的故障开放相反，分布式锁在抢锁阶段抛错（如 Redis 不可用）时**原样上抛、不执行
  原方法**。原因是放行会让两个进程同时进入临界区，可能造成数据损坏，违背互斥语义；仅释锁阶段的异常被记录告警、
  不掩盖业务结果。
- **关于默认顺序**：带参数绑定（`@annotation(..)`）的切面若取 `Ordered.HIGHEST_PRECEDENCE`（`Integer.MIN_VALUE`），
  会与 Spring 内部的 `ExposeInvocationInterceptor` 冲突，运行期抛
  "Required to bind N arguments, but only bound M (JoinPointMatch was NOT bound in invocation)"。
  故默认退一格到 `HIGHEST_PRECEDENCE + 1`，近似最外层又避开冲突。

## 快速验证（Docker）

用 Dragonfly 容器一键起一个原生支持 `CL.THROTTLE` 的实例：

```bash
docker run -d \
-p 6379:6379 \
--ulimit memlock=-1 \
--name dragonfly \
docker.dragonflydb.io/dragonflydb/dragonfly \
--dbnum=1
```

启动后即可运行模块自带的真机测试（连接 `redis://127.0.0.1:6379`，不可达时自动跳过）：

```bash
# 限流真机测试
mvn -pl cookbook-redis test -Dtest=RateLimitLiveTest

# 分布式锁真机测试（执行器层 + 切面层）
mvn -pl cookbook-redis test -Dtest='DistributedLockManagerLiveTest,DistributedLockLiveTest'
```

也可以连进实例手动验证命令：

```bash
docker exec -it dragonfly redis-cli CL.THROTTLE demo 3 5 1 1
```

## 依赖与编译要求

- Redisson：提供 `RedissonClient`、脚本下发能力（限流）与可重入锁（分布式锁）；
- Spring：spring-context 与 AspectJ weaver，启用方法代理；
- 编译需保留参数名：`-parameters`（模块 pom 已配置 `maven.compiler.parameters=true`）；
- 后端：Dragonfly 原生支持 `CL.THROTTLE`，Redis 需加载 redis-cell 模块；分布式锁兼容任意 Redis/Dragonfly。