package io.ituknown.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 该注解用于在方法执行前后自动获取与释放分布式锁，保护临界区、防止重复提交、做幂等控制。
 * <p>
 * 使用前提：已用 {@link EnableDistributedLock} 开启声明式分布式锁功能。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁键的 SpEL 表达式，把不同调用锁定到不同的资源上。
     * <p>留空时同一方法的所有调用争用一把锁；填表达式则按求值结果分别加锁，结果为 null 时退回方法级。
     * 键解析规则与 {@link RateLimit#key()} 完全一致，最终锁键为 {@code 简单类名#方法名[#求值结果]}。
     */
    String key() default "";

    /**
     * 最长等待获取时长，单位见 {@link #timeUnit()}；默认 0 表示不等待，抢不到立即失败并抛
     * {@link io.ituknown.redis.LockNotAcquiredException}。
     */
    long waitTime() default 0;

    /**
     * 持锁时长，单位见 {@link #timeUnit()}；默认 -1 表示启用 watchdog 自动续期，
     * 业务方法正常执行期间锁不会因超时丢失，进程崩溃则按 {@code lockWatchdogTimeout} 自动释放。
     * 设为正数时到点自动释放且不再续期。
     */
    long leaseTime() default -1;

    /**
     * {@link #waitTime()} 与 {@link #leaseTime()} 的单位，默认秒。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
