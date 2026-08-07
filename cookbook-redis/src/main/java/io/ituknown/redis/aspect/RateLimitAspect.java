package io.ituknown.redis.aspect;

import java.lang.reflect.Method;

import io.ituknown.redis.RateLimitExceededException;
import io.ituknown.redis.RateLimiter;
import io.ituknown.redis.ThrottleResult;
import io.ituknown.redis.annotation.RateLimit;
import io.ituknown.redis.support.SpelKeyResolver;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 方法限流切面：方法被限流注解标记时介入，先解析限流维度再向执行器申请配额。
 *
 * <p>判定流程：维度解析后申请一次配额，放行则继续原方法；超限则抛出限流异常，
 * 由调用方决定降级策略。限流后端不可用或判定抛错时按放行处理（故障开放），
 * 仅记录告警，避免限流组件故障阻断业务主流程。
 */
@Aspect
public class RateLimitAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimiter rateLimiter;
    private final SpelKeyResolver keyResolver;

    public RateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver) {
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
    }

    /**
     * 包裹被限流注解标记的方法：解析维度、申请配额，超限抛异常，其余放行原方法。
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = keyResolver.resolve(method, pjp.getArgs(), rateLimit.key());
        int maxBurst = rateLimit.maxBurst() < 0 ? rateLimit.count() : rateLimit.maxBurst();

        ThrottleResult result;
        try {
            result = rateLimiter.tryAcquire(
                    key, maxBurst, rateLimit.period(), rateLimit.count(), rateLimit.quantity());
        } catch (Exception e) {
            LOGGER.warn("限流判定异常，按放行处理; key={}", key, e);
            return pjp.proceed();
        }

        if (!result.allowed()) {
            throw new RateLimitExceededException(key, result);
        }
        return pjp.proceed();
    }
}
