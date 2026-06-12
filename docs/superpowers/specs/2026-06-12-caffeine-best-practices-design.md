# cookbook-caffeine 生产最佳实践设计

## 目标

将 cookbook-caffeine 从骨架 Main 升级为可直接复用的工具库，覆盖基础缓存操作和加载/刷新场景。

## 约束

- 方案 A：单一工具类 + 独立示例，与 JacksonUtils/AesUtils 等模块风格一致
- 包名统一：`io.ituknown.cookbook.caffeine` → `io.ituknown.caffeine`
- Java 21, Caffeine 3.2.3, JUnit 5

## 文件结构

```
src/main/java/io/ituknown/caffeine/
├── CaffeineUtils.java        // 核心工具类
└── Examples.java             // 可运行的示例集合

src/test/java/io/ituknown/caffeine/
└── CaffeineUtilsTest.java    // 单元测试
```

## CaffeineUtils API

枚举单例风格，静态方法：

| 方法 | 说明 |
|---|---|
| `newCache(maxSize, expireAfterWriteMinutes)` | Manual Cache |
| `newLoadingCache(maxSize, expireAfterWriteMinutes, loader)` | LoadingCache |
| `newAsyncLoadingCache(maxSize, expireAfterWriteMinutes, loader)` | AsyncCache |
| `newExpireAfterAccessCache(maxSize, expireAfterAccessMinutes)` | 访问过期缓存 |
| `newWeakKeyCache(expireAfterWriteMinutes)` | 弱引用 Key |
| `newSoftValueCache(maxSize, expireAfterWriteMinutes)` | 软引用 Value |

## Examples 覆盖场景

1. 基础 CRUD（put/get/invalidate）
2. LoadingCache 自动加载
3. 过期策略（write vs access）
4. 容量驱逐 + RemovalListener
5. 弱引用/软引用
6. AsyncCache
7. refreshAfterWrite
8. 统计监控
9. 生产注意事项

## 测试覆盖

- 基本 put/get
- LoadingCache 自动加载
- 过期驱逐（Ticker 模拟）
- 容量驱逐
- 统计信息

## 清理

- 删除 `io/ituknown/cookbook-caffeine/` 空目录
- 删除旧 `Cache.java`
