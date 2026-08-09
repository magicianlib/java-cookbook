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
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * 方法限流切面
 * <p>
 * 拦截带 {@link RateLimit} 注解的方法：解析限流键、向 {@link RateLimiter} 申请配额，放行就执行原方法。</p>
 * <p>
 * 被限流时抛出 {@link RateLimitExceededException}，由调用方决定降级策略。</p>
 * <p>
 * {@link RateLimiter} 自身报错（如 Redis 不可用）只记告警、照常放行，不让限流组件的故障拖垮主流程。</p>
 * <p>
 * 实现 {@link Ordered}，顺序由 {@link io.ituknown.redis.annotation.EnableRateLimit#order()} 注入，
 * 默认 {@link Ordered#LOWEST_PRECEDENCE}（最内层）；需要让限流先于事务等切面生效时调小该值。</p>
 */
@Aspect
public class RateLimitAspect implements Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitAspect.class);

    /**
     * 限流执行器
     */
    private final RateLimiter rateLimiter;
    /**
     * 限流键解析器
     */
    private final SpelKeyResolver keyResolver;
    /**
     * 切面执行顺序
     */
    private final int order;

    public RateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver) {
        this(rateLimiter, keyResolver, Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * @param order 切面顺序，数值越小优先级越高（越靠外层执行）
     */
    public RateLimitAspect(RateLimiter rateLimiter, SpelKeyResolver keyResolver, int order) {
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = keyResolver.resolve(method, pjp.getArgs(), rateLimit.key());

        ThrottleStatus result;
        try {
            result = rateLimiter.tryAcquire(key, rateLimit.maxBurst(), rateLimit.period(), rateLimit.count(), rateLimit.quantity());
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