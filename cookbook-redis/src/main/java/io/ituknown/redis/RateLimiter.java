package io.ituknown.redis;

import java.util.List;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

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
        // 限流命令的入参与键须以纯文本下发，使用文本编解码避免默认编码产生非文本字节
        RScript script = client.getScript(new StringCodec());
        List<Object> keys = List.<Object>of(key);
        Object[] args = {
                String.valueOf(maxBurst),
                String.valueOf(count),
                String.valueOf(period),
                String.valueOf(quantity)
        };

        String sha = scriptSha;
        if (sha != null) {
            try {
                List<?> result = script.evalSha(
                        RScript.Mode.READ_WRITE, sha, RScript.ReturnType.LIST, keys, args);
                return ThrottleResult.from(result);
            } catch (Exception ignored) {
                // 脚本指纹失效（如服务端重启清空），回退全量下发并在下方重载指纹
                scriptSha = null;
            }
        }

        List<?> result = script.eval(
                RScript.Mode.READ_WRITE, LUA_THROTTLE, RScript.ReturnType.LIST, keys, args);
        scriptSha = script.scriptLoad(LUA_THROTTLE);
        return ThrottleResult.from(result);
    }
}
