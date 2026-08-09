package io.ituknown.redis;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.List;

/**
 * 限流执行器：把判定参数透传给 Redis 限流命令，返回结构化结果。
 *
 * <p>底层依赖 CL.THROTTLE 命令（redis-cell 模块提供，Dragonfly 原生内置），
 * 采用 GCRA（通用信元速率算法）实现速率控制。命令依次接收限流键、最大突发量、
 * 周期配额、周期秒数、本次申请配额数五项入参，返回五元组：
 * 是否放行、桶总容量、剩余配额、重试等待秒数、恢复满桶等待秒数，
 * 其中桶总容量等于最大突发量加一。
 *
 * <p>该命令要求入参与限流键均为纯文本字节。Redisson 默认编解码器会把数值编码成
 * 非文本字节，导致限流命令无法解析，因此本实现改用字符串编解码器下发，
 * 所有参数统一转成文本。
 *
 * <p>热路径优化：首次下发时把脚本登记到服务端换取指纹，后续按指纹复用，
 * 避免每次重复传输脚本全文；指纹失效（如服务端重启清空缓存）时回退全量下发并重载。
 */
public class RateLimiter {

    /**
     * 限流命令的脚本包装：透传限流键与四项参数调用 CL.THROTTLE。
     * 借助脚本既能被服务端缓存换指纹，又能以单次网络往返完成判定。
     */
    private static final String LUA_THROTTLE =
            "return redis.call('CL.THROTTLE', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])";

    private final RedissonClient client;
    private volatile String scriptSha;

    public RateLimiter(RedissonClient client) {
        this.client = client;
    }

    public ThrottleStatus tryAcquire(String key, int period, int count) {
        return tryAcquire(key, count, period, count, 1);
    }

    /**
     * 申请一次配额并返回判定结果。
     *
     * @param key      限流键
     * @param maxBurst 最大突发量；小于 0（如注解默认 -1）表示取 count 作为突发上限
     * @param period   周期秒数
     * @param count    周期配额
     * @param quantity 本次申请配额数
     * @return 限流判定结果，含是否放行及桶容量、剩余配额、等待秒数等信息
     */
    public ThrottleStatus tryAcquire(String key, int maxBurst, int period, int count, int quantity) {
        // maxBurst 未显式指定时取周期配额作为突发上限
        if (maxBurst < 0) {
            maxBurst = count;
        }
        
        // 限流命令的入参与键须以纯文本下发，使用文本编解码避免默认编码产生非文本字节
        RScript script = client.getScript(new StringCodec());
        List<Object> keys = List.of(key);
        Object[] args = {
                String.valueOf(maxBurst),
                String.valueOf(count),
                String.valueOf(period),
                String.valueOf(quantity)
        };

        String sha = scriptSha;
        if (sha != null) {
            try {
                List<?> result = script.evalSha(RScript.Mode.READ_WRITE, sha, RScript.ReturnType.LIST, keys, args);
                return ThrottleStatus.from(result);
            } catch (Exception ignored) {
                // 脚本指纹失效（如服务端重启清空），回退全量下发并在下方重载指纹
                scriptSha = null;
            }
        }

        List<?> result = script.eval(RScript.Mode.READ_WRITE, LUA_THROTTLE, RScript.ReturnType.LIST, keys, args);
        scriptSha = script.scriptLoad(LUA_THROTTLE);
        return ThrottleStatus.from(result);
    }
}