package io.ituknown.caffeine;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 缓存生产环境最佳实践示例集。
 * <p>
 * 直接运行 {@code main} 方法即可查看各场景演示输出。
 * <p>
 * 所有缓存 Value 均使用 {@link Optional} 包装，参见 {@link CaffeineUtils} 类注释。
 *
 * @author magicianlib@gmail.com
 * @see CaffeineUtils
 */
public class Examples {

    public static void main(String[] args) throws Exception {
        // ========== 1. 基础 CRUD（手动缓存） ==========
        basicCrud();

        // ========== 2. 自动加载（LoadingCache） ==========
        loadingCache();

        // ========== 3. 过期策略对比 ==========
        expireStrategy();

        // ========== 4. 容量驱逐 ==========
        sizeBasedEviction();

        // ========== 5. 弱引用 / 软引用 ==========
        referenceCache();

        // ========== 6. 异步缓存 ==========
        asyncLoadingCache();

        // ========== 7. 自动刷新（refreshAfterWrite） ==========
        refreshAfterWrite();

        // ========== 8. 统计监控 ==========
        statistics();

        // ========== 9. 生产注意事项 ==========
        productionNotes();
    }

    // ======================== 1. 基础 CRUD ========================

    /**
     * 手动缓存基本操作：put、get、invalidate。
     */
    static void basicCrud() {
        System.out.println("===== 1. 基础 CRUD =====");

        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(100, 10);

        // 写入：用 Optional.of() 包装实际值
        cache.put("user:1", Optional.of("Alice"));
        cache.put("user:2", Optional.of("Bob"));

        // 读取：getIfPresent 返回 Optional<String> 或 null（未命中）
        System.out.println("user:1 -> " + cache.getIfPresent("user:1").orElse(null)); // Alice
        System.out.println("user:3 -> " + cache.getIfPresent("user:3"));               // null（未命中）

        // 写入空值：Optional.empty() 表示"查过了，确实没有"，可用于防止缓存穿透
        cache.put("user:4", Optional.empty());
        System.out.println("user:4 (empty) -> " + cache.getIfPresent("user:4")); // Optional.empty（命中，值为空）

        // 原子计算：缓存未命中时通过 Function 加载
        Optional<String> value = cache.get("user:3", k -> Optional.of("Charlie"));
        System.out.println("user:3 (computed) -> " + value.orElse(null)); // Charlie

        // 单个失效
        cache.invalidate("user:1");
        System.out.println("user:1 (after invalidate) -> " + cache.getIfPresent("user:1")); // null

        // 全量失效
        cache.invalidateAll();
        System.out.println("cache size (after invalidateAll) -> " + cache.estimatedSize()); // 0
        System.out.println();
    }

    // ======================== 2. 自动加载 ========================

    /**
     * LoadingCache 自动加载：get 时缓存未命中自动调用 loader。
     * loader 返回值会自动通过 Optional.ofNullable() 包装。
     */
    static void loadingCache() {
        System.out.println("===== 2. 自动加载（LoadingCache） =====");

        LoadingCache<String, Optional<String>> cache = CaffeineUtils.newLoadingCache(100, 10,
                key -> {
                    System.out.println("  [loader] 加载 key: " + key);
                    return "value-of-" + key;
                });

        // 首次访问触发加载，返回 Optional<String>
        System.out.println("get(a) -> " + cache.get("a").orElse(null));     // 触发 loader
        System.out.println("get(a) -> " + cache.get("a").orElse(null));     // 缓存命中，不再触发

        // getAll 批量加载
        System.out.println("getAll([b, c]) -> " + cache.getAll(java.util.Set.of("b", "c")));
        System.out.println();
    }

    // ======================== 3. 过期策略 ========================

    /**
     * expireAfterWrite vs expireAfterAccess 的区别。
     * <p>
     * 写入过期：条目在写入后固定时间过期，无论是否被访问。
     * 访问过期：条目在最后一次访问（读/写）后过期，频繁访问可续命。
     */
    static void expireStrategy() {
        System.out.println("===== 3. 过期策略对比 =====");
        System.out.println("expireAfterWrite: 写入后固定时间过期，适合数据强一致性场景（如配置）。");
        System.out.println("expireAfterAccess: 最后访问后过期，适合热点数据续命场景。");
        System.out.println("注意：不要同时设置两种过期策略，行为容易混淆。");
        System.out.println();
    }

    // ======================== 4. 容量驱逐 ========================

    /**
     * maximumSize 驱逐策略：基于 Window TinyLfu 算法。
     */
    static void sizeBasedEviction() {
        System.out.println("===== 4. 容量驱逐 =====");

        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(3, 60);

        cache.put("a", Optional.of("1"));
        cache.put("b", Optional.of("2"));
        cache.put("c", Optional.of("3"));

        System.out.println("写入 3 条 (maxSize=3): " + cache.asMap().keySet());

        // 超过容量时，Window TinyLfu 策略驱逐最不常用的条目
        cache.put("d", Optional.of("4"));
        cache.cleanUp(); // 手动触发清理以观察驱逐

        System.out.println("写入第 4 条后: " + cache.asMap().keySet());
        System.out.println("驱逐数: " + cache.stats().evictionCount());
        System.out.println("提示: maximumSize 是近似值，Caffeine 内部有分段策略，实际驱逐可能有延迟。");
        System.out.println();
    }

    // ======================== 5. 弱引用 / 软引用 ========================

    /**
     * weakKeys / softValues 适用场景。
     */
    static void referenceCache() {
        System.out.println("===== 5. 弱引用 / 软引用 =====");

        // 弱引用 Key：Key 被 GC 回收后条目自动移除
        Cache<Object, Optional<String>> weakKeyCache = CaffeineUtils.newWeakKeyCache(10);
        Object key = new Object();
        weakKeyCache.put(key, Optional.of("value"));
        System.out.println("weakKey get (强引用存在) -> " + weakKeyCache.getIfPresent(key).orElse(null));
        key = null; // 释放强引用
        System.out.println("提示: key 置 null 后，下次 GC 可能回收该条目。");

        // 软引用 Value：内存不足时 GC 回收 Value
        Cache<String, Optional<byte[]>> softValueCache = CaffeineUtils.newSoftValueCache(100, 10);
        softValueCache.put("big-data", Optional.of(new byte[1024]));
        System.out.println("softValue get -> " + softValueCache.getIfPresent("big-data").isPresent());
        System.out.println("提示: 软引用 Value 在 JVM 内存不足时才会被 GC 回收，平时不会自动清理。");
        System.out.println();
    }

    // ======================== 6. 异步缓存 ========================

    /**
     * AsyncLoadingCache：get 返回 CompletableFuture，不阻塞调用线程。
     */
    static void asyncLoadingCache() throws Exception {
        System.out.println("===== 6. 异步缓存 =====");

        AsyncLoadingCache<String, Optional<String>> cache = CaffeineUtils.newAsyncLoadingCache(100, 10,
                key -> {
                    // 模拟耗时加载
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "async-value-of-" + key;
                });

        // 非阻塞获取
        CompletableFuture<Optional<String>> future = cache.get("x");
        System.out.println("提交异步加载请求，做其他事情...");
        Optional<String> result = future.get(1, TimeUnit.SECONDS);
        System.out.println("异步结果: " + result.orElse(null));

        // 再次获取（缓存命中）
        CompletableFuture<Optional<String>> cached = cache.get("x");
        System.out.println("缓存命中: " + cached.getNow(null).orElse(null));
        System.out.println();
    }

    // ======================== 7. 自动刷新 ========================

    /**
     * refreshAfterWrite：写入后指定时间标记为"需要刷新"，
     * 下次查询时在后台异步刷新，期间返回旧值。
     */
    static void refreshAfterWrite() {
        System.out.println("===== 7. 自动刷新（refreshAfterWrite） =====");

        LoadingCache<String, Optional<String>> cache = CaffeineUtils.newRefreshLoadingCache(
                100, 10, 1, // 10min 过期, 1min 刷新
                key -> "fresh-value-" + System.currentTimeMillis());

        Optional<String> first = cache.get("config");
        System.out.println("首次加载: " + first.orElse(null));

        // 1 分钟内再次获取 → 返回旧值（未到刷新时间）
        System.out.println("刷新间隔内获取: " + cache.get("config").orElse(null));

        System.out.println("提示: refreshAfterWrite 仅在条目被访问时才触发刷新，不是定时任务。");
        System.out.println("生产建议: refreshAfterWrite < expireAfterWrite，保证刷新失败后最终过期。");
        System.out.println();
    }

    // ======================== 8. 统计监控 ========================

    /**
     * 通过 recordStats() 收集缓存命中率、驱逐数等指标，用于生产监控。
     */
    static void statistics() {
        System.out.println("===== 8. 统计监控 =====");

        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(100, 10);

        // 模拟访问
        cache.put("a", Optional.of("1"));
        cache.getIfPresent("a");  // hit
        cache.getIfPresent("a");  // hit
        cache.getIfPresent("b");  // miss
        cache.get("c", k -> Optional.of("3")); // miss + load

        CacheStats stats = cache.stats();
        System.out.printf("命中率: %.2f%%%n", stats.hitRate() * 100);
        System.out.println("命中次数: " + stats.hitCount());
        System.out.println("未命中次数: " + stats.missCount());
        System.out.println("加载次数: " + stats.loadCount());
        System.out.println("驱逐次数: " + stats.evictionCount());
        System.out.printf("平均加载耗时: %.3f ms%n", stats.averageLoadPenalty() / 1_000_000.0);
        System.out.println();
    }

    // ======================== 9. 生产注意事项 ========================

    /**
     * 生产环境使用 Caffeine 的关键注意事项汇总。
     */
    static void productionNotes() {
        System.out.println("===== 9. 生产注意事项 =====");
        System.out.println("""
                1. [容量] 必须设置 maximumSize 或 maximumWeight，防止内存溢出。
                   - maximumSize: 按条目数限制（最常用）
                   - maximumWeight: 按权重限制（适合 Value 大小不均匀的场景）

                2. [过期] 必须设置过期策略，推荐 expireAfterWrite。
                   - 不要同时设置 expireAfterWrite 和 expireAfterAccess，行为易混淆。
                   - refreshAfterWrite 是"后台续命"，不等同于过期。

                3. [驱逐] 驱逐是惰性的，不会主动扫描。
                   - 条目过期后不会立即移除，而是下次访问或 cleanUp() 时清理。
                   - estimatedSize() 包含已过期但未清理的条目，仅供参考。

                4. [弱/软引用]
                   - weakKeys 使用 == 而非 equals 比较，String 等共享对象慎用。
                   - softValues 依赖 GC 行为，不适合精确控制缓存大小。
                   - 不要同时使用 weakValues 和 softValues。

                5. [LoadingCache loader]
                   - loader 返回 null 是安全的，会被自动包装为 Optional.empty()。
                   - loader 中如果抛异常，get() 会传播该异常。
                   - 批量场景覆写 CacheLoader.loadAll() 提升效率。

                6. [refreshAfterWrite]
                   - 刷新是被动触发（仅在查询时），不是定时任务。
                   - 必须配合 expireAfterWrite 使用，refresh < expire，保证刷新失败后最终过期。
                   - 需要设置 executor()，否则刷新在调用线程同步执行。

                7. [监控] 生产环境务必开启 recordStats()。
                   - 对接 Micrometer 等监控系统：recordStats(() -> new MicrometerStatsCounter(...))
                   - 关注命中率低于 80% 的缓存，可能需要调整容量或过期策略。

                8. [并发]
                   - Caffeine 使用 ConcurrentHashMap，线程安全。
                   - get(key, loader) 对同一个 key 的并发加载只执行一次。
                   - AsyncLoadingCache.get() 返回同一个 CompletableFuture 实例。

                9. [序列化] Cache 对象不支持序列化，不要尝试将 Cache 存入 Session 或分布式缓存。

                10. [内存]
                   - 缓存 Value 建议使用不可变对象，避免外部修改导致数据不一致。
                   - 大对象缓存考虑 maximumWeight + Weigher 按内存占用控制。

                11. [Optional 包装]
                   - 本工具类统一使用 Optional<V> 作为 Value 类型。
                   - Optional.empty() 可用于缓存空值，防止缓存穿透。
                   - 读取时注意区分：getIfPresent() 返回 null = 未命中；返回 Optional.empty() = 命中但值为空。
                   - loader 返回 null 是安全的，自动包装为 Optional.empty()。
                """);
    }
}
