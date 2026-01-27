package io.ituknown.cookbook.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public class Cache {
    public static void main(String[] args) {
        LoadingCache<String, Object> cache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Object>() {
                    @Override
                    public Object load(String key) {
                        // 加载 value
                        return null;
                    }
                });
    }
}