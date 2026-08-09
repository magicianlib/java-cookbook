package io.ituknown.redis.aspect;

import io.ituknown.redis.DistributedLockManager;
import io.ituknown.redis.LockNotAcquiredException;
import io.ituknown.redis.annotation.DistributedLock;
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
 * 方法分布式锁切面
 * <p>
 * 拦截带 {@link DistributedLock} 注解的方法：解析锁键、抢锁，抢到就执行原方法并在结束后释放。</p>
 * <p>
 * 抢不到（锁被他人持有）抛 {@link LockNotAcquiredException}，由调用方决定降级策略。</p>
 * <p>
 * 故障策略为 fail-closed：获取阶段抛错（如 Redis 不可用）原样上抛、不执行原方法。这与限流切面的
 * fail-open 相反，因为放行会导致两个进程同时进入临界区，可能造成数据损坏，违背互斥语义。</p>
 * <p>
 * 释放阶段的异常只记告警、不掩盖业务方法的结果或异常。</p>
 * <p>
 * 实现 {@link Ordered}，顺序由 {@link io.ituknown.redis.annotation.EnableDistributedLock#order()} 注入，
 * 默认 {@code Ordered.HIGHEST_PRECEDENCE + 1}。</p>
 *
 * <p><b>关于默认顺序为何不取 {@link Ordered#HIGHEST_PRECEDENCE}</b>：带参数绑定（如
 * {@code @annotation(..)}）的切面若取 {@code Integer.MIN_VALUE}，会与 Spring 内部的
 * {@code ExposeInvocationInterceptor}（同样占据该顺序）冲突，导致运行期报
 * "Required to bind N arguments, but only bound M (JoinPointMatch was NOT bound in invocation)"，
 * 参数无法绑定。故默认退一格到 {@code HIGHEST_PRECEDENCE + 1}，仍近似最外层、包住事务等内层切面，
 * 又避开冲突；如需更内层可经 {@code @EnableDistributedLock(order = ...)} 调整。</p>
 */
@Aspect
public class DistributedLockAspect implements Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockAspect.class);

    /**
     * 锁执行器
     */
    private final DistributedLockManager lockManager;
    /**
     * 锁键解析器
     */
    private final SpelKeyResolver keyResolver;
    /**
     * 切面执行顺序
     */
    private final int order;

    public DistributedLockAspect(DistributedLockManager lockManager, SpelKeyResolver keyResolver) {
        this(lockManager, keyResolver, Ordered.HIGHEST_PRECEDENCE + 1);
    }

    /**
     * @param order 切面顺序，数值越小优先级越高（越靠外层执行）
     */
    public DistributedLockAspect(DistributedLockManager lockManager, SpelKeyResolver keyResolver, int order) {
        this.lockManager = lockManager;
        this.keyResolver = keyResolver;
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = keyResolver.resolve(method, pjp.getArgs(), distributedLock.key());

        boolean acquired = lockManager.tryAcquire(
                key, distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
        // tryAcquire 自身报错（如 Redis 不可用）原样上抛：fail-closed

        if (!acquired) { // 锁被他人持有
            throw new LockNotAcquiredException(key);
        }

        try {
            return pjp.proceed();
        } finally {
            try {
                lockManager.release(key);
            } catch (Exception e) {
                // 释放异常不应掩盖业务结果或业务异常，仅告警
                LOGGER.warn("释放分布式锁异常; key={}", key, e);
            }
        }
    }
}
