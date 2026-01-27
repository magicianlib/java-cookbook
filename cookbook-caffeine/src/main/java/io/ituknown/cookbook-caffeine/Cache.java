public class Cache {
LoadingCache<String, Object> cache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Object>() {
                    @Override
                    public @Nullable Object load(@NonNull String key) {
                        // 加载 value
                        return null;
                    }
                });
}
