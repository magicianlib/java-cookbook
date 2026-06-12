package io.ituknown.caffeine;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CaffeineUtils 单元测试。
 */
class CaffeineUtilsTest {

    // ========== newCache ==========

    @Test
    void newCache_put_and_get() {
        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(100, 10);

        cache.put("key", Optional.of("value"));
        assertEquals(Optional.of("value"), cache.getIfPresent("key"));
    }

    @Test
    void newCache_put_empty_value() {
        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(100, 10);

        // Optional.empty() 可以正常缓存，用于防缓存穿透
        cache.put("key", Optional.empty());
        assertNotNull(cache.getIfPresent("key"));    // 命中
        assertFalse(cache.getIfPresent("key").isPresent()); // 值为空
    }

    @Test
    void newCache_get_with_loader() {
        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(100, 10);

        Optional<String> result = cache.get("missing", k -> Optional.of("loaded-" + k));
        assertEquals(Optional.of("loaded-missing"), result);
        assertEquals(Optional.of("loaded-missing"), cache.getIfPresent("missing"));
    }

    @Test
    void newCache_invalidate() {
        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(100, 10);

        cache.put("key", Optional.of("value"));
        cache.invalidate("key");
        assertNull(cache.getIfPresent("key"));
    }

    // ========== newLoadingCache ==========

    @Test
    void loadingCache_auto_load() {
        AtomicInteger loadCount = new AtomicInteger();
        LoadingCache<String, Optional<String>> cache = CaffeineUtils.newLoadingCache(100, 10, key -> {
            loadCount.incrementAndGet();
            return "value-of-" + key;
        });

        // 首次访问触发加载
        assertEquals(Optional.of("value-of-a"), cache.get("a"));
        assertEquals(1, loadCount.get());

        // 再次访问命中缓存，不触发加载
        assertEquals(Optional.of("value-of-a"), cache.get("a"));
        assertEquals(1, loadCount.get());
    }

    @Test
    void loadingCache_loads_null_as_empty() {
        LoadingCache<String, Optional<String>> cache = CaffeineUtils.newLoadingCache(100, 10,
                key -> null);

        // loader 返回 null → 包装为 Optional.empty()
        assertEquals(Optional.empty(), cache.get("missing"));
    }

    @Test
    void loadingCache_getAll() {
        LoadingCache<String, Optional<String>> cache = CaffeineUtils.newLoadingCache(100, 10,
                key -> "value-of-" + key);

        var result = cache.getAll(Set.of("x", "y"));
        assertEquals(Optional.of("value-of-x"), result.get("x"));
        assertEquals(Optional.of("value-of-y"), result.get("y"));
    }

    // ========== newExpireAfterAccessCache ==========

    @Test
    void expireAfterAccess_entries_expire() {
        var ticker = new com.github.benmanes.caffeine.cache.Ticker() {
            private long nanos = 0;

            @Override
            public long read() {
                return nanos;
            }

            public void advance(long duration, TimeUnit unit) {
                nanos += unit.toNanos(duration);
            }
        };

        Cache<String, Optional<String>> cache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .executor(Runnable::run)
                .ticker(ticker::read)
                .build();

        cache.put("key", Optional.of("value"));
        assertEquals(Optional.of("value"), cache.getIfPresent("key"));

        // 推进时间超过过期阈值
        ticker.advance(61, TimeUnit.SECONDS);
        cache.cleanUp();

        assertNull(cache.getIfPresent("key"));
    }

    // ========== 容量驱逐 ==========

    @Test
    void maximumSize_evicts_excess_entries() {
        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(2, 60);

        cache.put("a", Optional.of("1"));
        cache.put("b", Optional.of("2"));
        cache.put("c", Optional.of("3")); // 超过 maxSize=2，触发驱逐
        cache.cleanUp();

        assertTrue(cache.estimatedSize() <= 2);
        assertTrue(cache.stats().evictionCount() >= 1);
    }

    // ========== newRefreshLoadingCache ==========

    @Test
    void refreshLoadingCache_returns_value() {
        LoadingCache<String, Optional<String>> cache = CaffeineUtils.newRefreshLoadingCache(
                100, 10, 1,
                key -> "fresh-" + key);

        assertEquals(Optional.of("fresh-a"), cache.get("a"));
    }

    // ========== newAsyncLoadingCache ==========

    @Test
    void asyncLoadingCache_get_returns_future() throws Exception {
        AsyncLoadingCache<String, Optional<String>> cache = CaffeineUtils.newAsyncLoadingCache(
                100, 10,
                key -> "async-" + key);

        CompletableFuture<Optional<String>> future = cache.get("key");
        assertNotNull(future);
        assertEquals(Optional.of("async-key"), future.get(1, TimeUnit.SECONDS));

        // 缓存命中
        CompletableFuture<Optional<String>> cached = cache.get("key");
        assertEquals(Optional.of("async-key"), cached.getNow(null));
    }

    // ========== 统计 ==========

    @Test
    void stats_records_hits_and_misses() {
        Cache<String, Optional<String>> cache = CaffeineUtils.newCache(100, 10);

        cache.put("a", Optional.of("1"));

        // 仅操作一次 hit，避免 Caffeine 内部维护周期产生额外 miss 计数
        cache.getIfPresent("a");  // hit
        cache.getIfPresent("b");  // miss

        CacheStats stats = cache.stats();
        assertTrue(stats.hitCount() >= 1, "应有至少 1 次命中");
        assertTrue(stats.missCount() >= 1, "应有至少 1 次未命中");
        assertTrue(stats.hitRate() > 0, "命中率应大于 0");
    }

    // ========== 弱引用 / 软引用 ==========

    @Test
    void weakKeyCache_basic_operations() {
        Cache<Object, Optional<String>> cache = CaffeineUtils.newWeakKeyCache(10);

        Object key = new Object();
        cache.put(key, Optional.of("value"));
        assertEquals(Optional.of("value"), cache.getIfPresent(key));
    }

    @Test
    void softValueCache_basic_operations() {
        Cache<String, Optional<String>> cache = CaffeineUtils.newSoftValueCache(100, 10);

        cache.put("key", Optional.of("value"));
        assertEquals(Optional.of("value"), cache.getIfPresent("key"));
    }
}
