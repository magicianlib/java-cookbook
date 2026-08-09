package io.ituknown.redis.annotation;

import io.ituknown.redis.config.DistributedLockConfiguration;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启声明式分布式锁功能
 * <p>
 * 开启后就能在方法上用 {@link DistributedLock} 注解配置分布式锁。</p>
 * <p>
 * 使用前要先在容器里注册 {@link RedissonClient} Bean；没有的话，启动会直接报错。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(DistributedLockConfiguration.class)
public @interface EnableDistributedLock {

    /**
     * 配置切面执行顺序，数值越小优先级越高，默认 {@code Ordered.HIGHEST_PRECEDENCE + 1}。
     * <p>
     * 分布式锁通常应包住事务等内层切面（抢锁在开事务之前、释放在提交之后），故默认偏外层。</p>
     * <p>
     * 特别说明：默认不取 {@link Ordered#HIGHEST_PRECEDENCE}（即 {@code Integer.MIN_VALUE}）。
     * 带参数绑定（{@code @annotation(..)}）的切面若取该极值，会与 Spring 内部的
     * {@code ExposeInvocationInterceptor}（同样占据该顺序）冲突，运行期抛
     * "Required to bind N arguments, but only bound M (JoinPointMatch was NOT bound in invocation)"。
     * 故默认退一格到 {@code HIGHEST_PRECEDENCE + 1}，近似最外层又避开冲突；如需更内层可调整本值。</p>
     * <p>
     * 若环境中配置了多个切面，最好保证不同切面的执行顺序不同，避免参数绑定类的歧义。</p>
     */
    int order() default Ordered.HIGHEST_PRECEDENCE + 1;
}
