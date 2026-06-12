package io.ituknown.caffeine;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Caffeine 缓存工具类，提供生产环境常用的缓存构建方法。
 * <p>
 * 所有方法的缓存 Value 类型统一为 {@link Optional}，原因如下：
 * <ul>
 *   <li>Caffeine 底层基于 {@link java.util.concurrent.ConcurrentHashMap}，不支持缓存 {@code null} 值；
 *       {@code put(key, null)} 会被静默忽略，loader 返回 {@code null} 等同于未命中。</li>
 *   <li>使用 {@code Optional<V>} 可以明确区分"缓存未命中"和"值确实为空"两种语义，
 *       避免调用方因 {@code getIfPresent()} 返回 {@code null} 而产生歧义。</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>{@code
 * // 缓存实际值
 * Cache<String, Optional<User>> cache = CaffeineUtils.newCache(100, 10);
 * cache.put("user:1", Optional.of(user));
 *
 * // 缓存空值（表示"查过了，确实没有"），防止缓存穿透
 * cache.put("user:2", Optional.empty());
 *
 * // 读取
 * Optional<User> value = cache.getIfPresent("user:1");
 * if (value == null) {
 *     // 缓存未命中 → 需要加载
 * } else if (value.isPresent()) {
 *     // 缓存命中，有值
 * } else {
 *     // 缓存命中，值为空（已被标记为不存在）
 * }
 * }</pre>
 * <p>
 * 所有方法均开启统计（{@code recordStats()}），并内置驱逐日志监听器。
 *
 * @author magicianlib@gmail.com
 * @see Caffeine
 * @see Cache
 * @see LoadingCache
 * @see AsyncLoadingCache
 * @see Optional
 */
public final class CaffeineUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaffeineUtils.class);

    /**
     * 异步加载线程池，用于 {@code refreshAfterWrite} 和 {@code AsyncLoadingCache}。
     */
    private static final Executor ASYNC_EXECUTOR = Executors.newCachedThreadPool();

    private CaffeineUtils() {
    }

    // ======================== Manual Cache ========================

    /**
     * 创建手动缓存，指定最大容量和写入过期时间。
     * <p>
     * 适用于需要显式 {@code put}/{@code get}/{@code invalidate} 操作的场景。
     * 缓存 Value 类型为 {@link Optional}，使用 {@code Optional.of(value)} 存入，
     * 使用 {@code Optional.empty()} 表示空值（防缓存穿透）。
     *
     * @param maxSize                 最大条目数
     * @param expireAfterWriteMinutes 写入后过期时间（分钟）
     * @param <K>                     键类型
     * @param <V>                     值类型（缓存中存储为 {@code Optional<V>}）
     * @return 手动缓存实例，Value 为 {@code Optional<V>}
     */
    public static <K, V> Cache<K, Optional<V>> newCache(long maxSize, long expireAfterWriteMinutes) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .recordStats()
                .removalListener(CaffeineUtils::onRemoval)
                .build();
    }

    // ======================== Loading Cache ========================

    /**
     * 创建自动加载缓存，指定最大容量、写入过期时间和加载函数。
     * <p>
     * 当 {@code get(key)} 发现缓存未命中时，自动调用 {@code loader} 加载值。
     * 加载函数的返回值会通过 {@link Optional#ofNullable(Object)} 包装，
     * 因此 loader 可以安全地返回 {@code null}——将被存储为 {@link Optional#empty()}。
     *
     * @param maxSize                 最大条目数
     * @param expireAfterWriteMinutes 写入后过期时间（分钟）
     * @param loader                  缓存加载函数，返回值允许为 {@code null}
     * @param <K>                     键类型
     * @param <V>                     值类型（缓存中存储为 {@code Optional<V>}）
     * @return 自动加载缓存实例，Value 为 {@code Optional<V>}
     */
    public static <K, V> LoadingCache<K, Optional<V>> newLoadingCache(long maxSize,
                                                                      long expireAfterWriteMinutes,
                                                                      Function<K, V> loader) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .recordStats()
                .removalListener(CaffeineUtils::onRemoval)
                .build(key -> Optional.ofNullable(loader.apply(key)));
    }

    /**
     * 创建带自动刷新的加载缓存。
     * <p>
     * {@code refreshAfterWriteMinutes} 应小于 {@code expireAfterWriteMinutes}，
     * 刷新在后台异步执行，不会阻塞读取（返回旧值）。
     * <br/>
     * <b>注意：</b>刷新仅在条目被查询时触发，不会主动定时刷新。
     * 加载函数的返回值通过 {@link Optional#ofNullable(Object)} 包装。
     *
     * @param maxSize                      最大条目数
     * @param expireAfterWriteMinutes      写入后过期时间（分钟）
     * @param refreshAfterWriteMinutes     写入后刷新时间（分钟），须小于过期时间
     * @param loader                       缓存加载函数，返回值允许为 {@code null}
     * @param <K>                          键类型
     * @param <V>                          值类型（缓存中存储为 {@code Optional<V>}）
     * @return 带自动刷新的加载缓存实例，Value 为 {@code Optional<V>}
     */
    public static <K, V> LoadingCache<K, Optional<V>> newRefreshLoadingCache(long maxSize,
                                                                             long expireAfterWriteMinutes,
                                                                             long refreshAfterWriteMinutes,
                                                                             Function<K, V> loader) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .refreshAfterWrite(refreshAfterWriteMinutes, TimeUnit.MINUTES)
                .executor(ASYNC_EXECUTOR)
                .recordStats()
                .removalListener(CaffeineUtils::onRemoval)
                .build(key -> Optional.ofNullable(loader.apply(key)));
    }

    // ======================== Access-Expiry Cache ========================

    /**
     * 创建访问过期缓存，条目在最后一次访问后指定时间过期。
     * <p>
     * 适用于热点数据场景——频繁访问的条目不会过期，冷数据自动淘汰。
     * 缓存 Value 类型为 {@link Optional}。
     *
     * @param maxSize                  最大条目数
     * @param expireAfterAccessMinutes 访问后过期时间（分钟）
     * @param <K>                      键类型
     * @param <V>                      值类型（缓存中存储为 {@code Optional<V>}）
     * @return 访问过期缓存实例，Value 为 {@code Optional<V>}
     */
    public static <K, V> Cache<K, Optional<V>> newExpireAfterAccessCache(long maxSize,
                                                                         long expireAfterAccessMinutes) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES)
                .recordStats()
                .removalListener(CaffeineUtils::onRemoval)
                .build();
    }

    // ======================== Weak / Soft Reference Cache ========================

    /**
     * 创建弱引用 Key 缓存。
     * <p>
     * 当 Key 没有强引用时，GC 可回收该条目。
     * <br/>
     * <b>注意：</b>使用弱引用 Key 时，Caffeine 使用 {@code identity}（==）而非 {@code equals} 比较 Key，
     * 因此仅适用于 Key 对象本身唯一的场景。
     * 缓存 Value 类型为 {@link Optional}。
     *
     * @param expireAfterWriteMinutes 写入后过期时间（分钟）
     * @param <K>                     键类型
     * @param <V>                     值类型（缓存中存储为 {@code Optional<V>}）
     * @return 弱引用 Key 缓存实例，Value 为 {@code Optional<V>}
     */
    public static <K, V> Cache<K, Optional<V>> newWeakKeyCache(long expireAfterWriteMinutes) {
        return Caffeine.newBuilder()
                .weakKeys()
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .recordStats()
                .removalListener(CaffeineUtils::onRemoval)
                .build();
    }

    /**
     * 创建软引用 Value 缓存，指定容量和过期时间。
     * <p>
     * 当 JVM 内存不足时，GC 可回收软引用的 Value。
     * <br/>
     * <b>注意：</b>软引用 Value 的实际驱逐取决于 GC 行为，不适合精确控制缓存大小。
     * 不能同时使用 {@code weakValues} 和 {@code softValues}。
     * 缓存 Value 类型为 {@link Optional}。
     *
     * @param maxSize                 最大条目数
     * @param expireAfterWriteMinutes 写入后过期时间（分钟）
     * @param <K>                     键类型
     * @param <V>                     值类型（缓存中存储为 {@code Optional<V>}）
     * @return 软引用 Value 缓存实例，Value 为 {@code Optional<V>}
     */
    public static <K, V> Cache<K, Optional<V>> newSoftValueCache(long maxSize,
                                                                  long expireAfterWriteMinutes) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .softValues()
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .recordStats()
                .removalListener(CaffeineUtils::onRemoval)
                .build();
    }

    // ======================== Async Loading Cache ========================

    /**
     * 创建异步加载缓存。
     * <p>
     * {@code get()} 返回 {@code CompletableFuture<Optional<V>>}，不阻塞调用线程。
     * 加载函数在 {@link #ASYNC_EXECUTOR} 线程池中异步执行，返回值通过
     * {@link Optional#ofNullable(Object)} 包装。
     *
     * @param maxSize                 最大条目数
     * @param expireAfterWriteMinutes 写入后过期时间（分钟）
     * @param loader                  同步加载函数（将被包装为异步执行），返回值允许为 {@code null}
     * @param <K>                     键类型
     * @param <V>                     值类型（缓存中存储为 {@code Optional<V>}）
     * @return 异步加载缓存实例，Value 为 {@code Optional<V>}
     */
    public static <K, V> AsyncLoadingCache<K, Optional<V>> newAsyncLoadingCache(long maxSize,
                                                                                long expireAfterWriteMinutes,
                                                                                Function<K, V> loader) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .executor(ASYNC_EXECUTOR)
                .recordStats()
                .removalListener(CaffeineUtils::onRemoval)
                .buildAsync((key, executor) ->
                        CompletableFuture.supplyAsync(() -> Optional.ofNullable(loader.apply(key)), executor));
    }

    // ======================== Internal ========================

    /**
     * 通用驱逐/移除监听器，记录日志。
     */
    private static <K, V> void onRemoval(K key, V value, RemovalCause cause) {
        if (cause.wasEvicted()) {
            LOGGER.debug("Cache entry evicted: key={}, cause={}", key, cause);
        }
    }
}
