package io.ituknown.redis.aspect;

import io.ituknown.redis.RateLimitExceededException;
import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.ThrottleStatus;
import io.ituknown.redis.annotation.RateLimit;
import io.ituknown.redis.support.SpelKeyResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 方法限流切面
 * <p>
 * 该切面会拦截带 {@link RateLimit} 注解的方法，根据注解配置解析限流规则并向 {@link RateLimiter} 申请配额。</p>
 * <p>
 * 如果 {@link RateLimiter} 不可用，仅记录告警，避免因为限流组件故障原因阻断业务主流程。</p>
 * <p>
 * 如果被限流将抛出 {@link RateLimitExceededException}，由调用方决定降级策略。</p>
 */
@Aspect
public class RateLimitAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitAspect.class);

    /**
     * 限流组件
     */
    private final RateLimiter rateLimiter;
    /**
     * {@link RateLimit} 规则解析器
     */
    private final SpelKeyResolver keyResolver;

    public RateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver) {
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = keyResolver.resolve(method, pjp.getArgs(), rateLimit.key());
        int maxBurst = rateLimit.maxBurst() < 0 ? rateLimit.count() : rateLimit.maxBurst();

        ThrottleStatus result;
        try {
            result = rateLimiter.tryAcquire(key, maxBurst, rateLimit.period(), rateLimit.count(), rateLimit.quantity());
        } catch (Exception e) {
            LOGGER.warn("限流判定异常，按放行处理; key={}", key, e);
            return pjp.proceed();
        }

        if (!result.allowed()) { // 被限流
            throw new RateLimitExceededException(key, result);
        }
        return pjp.proceed();
    }
}
