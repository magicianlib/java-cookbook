package io.ituknown.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁执行器：把锁的获取与释放封装在 Redisson 之上，供切面与调用方统一使用。
 *
 * <p>底层使用 Redisson 的可重入锁（RLock）。{@code leaseTime} 为 -1 时启用 watchdog 自动续期，
 * 业务正常执行期间锁不会因超时丢失；进程崩溃或显式关闭则按 {@code lockWatchdogTimeout} 自动释放，
 * 避免死锁。
 *
 * <p><b>获取阶段被中断（InterruptedException）的触发场景</b>：{@code tryLock(waitTime, leaseTime, unit)}
 * 本身是可中断的，当调用线程被置了中断标志时即抛 {@link InterruptedException}，主要有两类：
 * <ul>
 *   <li>阻塞等待期间被中断：仅当 {@code waitTime > 0} 且锁正被他人持有时，调用线程才会阻塞等锁
 *       （Redisson 走订阅加可中断等待）。这段等待窗口内若被打断（如 {@code Future.cancel(true)}、
 *       {@code ExecutorService.shutdownNow()}、异步框架取消传播、请求超时或客户端断连时容器中断请求线程）
 *       即抛出。</li>
 *   <li>调用前线程已处于中断状态：上游被中断但未清理标志，进入获取时同样会抛。</li>
 * </ul>
 * 默认 {@code waitTime = 0}（非阻塞）时，Redisson 抢不到即返回 false、不进入可中断等待，故该分支实际走不到，
 * 仅在显式设 {@code waitTime > 0} 并发生取消或中断时才会出现。
 *
 * <p><b>本类的处理</b>：捕获后先 {@code Thread.currentThread().interrupt()} 恢复中断标志（不吞中断，让上层仍能感知），
 * 再转为 {@link IllegalStateException} 抛出，交由切面按 fail-closed 处理（被保护方法不执行）；线程被中断通常就是
 * 取消信号，中止临界区是正确反应。
 *
 * <p><b>释放阶段</b>以 {@link RLock#isHeldByCurrentThread()} 守卫，仅当前线程持有时才解锁，兼容 lease 到期已自动释放
 * 的情形，避免 {@code unlock()} 误抛 {@link IllegalMonitorStateException}。
 */
public class DistributedLockManager {

    private final RedissonClient client;

    public DistributedLockManager(RedissonClient client) {
        this.client = client;
    }

    /**
     * 尝试获取分布式锁。
     *
     * @param key       锁键
     * @param waitTime  最长等待获取时长；0 表示不等待，抢不到立即返回 false
     * @param leaseTime 持锁时长；-1 表示启用 watchdog 自动续期，持有到显式释放为止
     * @param unit      waitTime 与 leaseTime 的单位
     * @return true 表示获取成功（含重入），false 表示在等待时间内未抢到
     */
    public boolean tryAcquire(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = client.getLock(key);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            // 恢复中断标志后转为运行期异常，避免吞中断；交由切面按 fail-closed 上抛
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取分布式锁被中断; key=" + key, e);
        }
    }

    public boolean tryAcquire(String key, long waitTime) {
        return tryAcquire(key, waitTime, -1, TimeUnit.MILLISECONDS);
    }

    public boolean tryAcquire(String key) {
        return tryAcquire(key, 0, -1, TimeUnit.MILLISECONDS);
    }

    /**
     * 释放分布式锁。仅当锁由当前线程持有时才解锁，否则视为已自动释放而跳过。
     *
     * @param key 锁键
     */
    public void release(String key) {
        RLock lock = client.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}