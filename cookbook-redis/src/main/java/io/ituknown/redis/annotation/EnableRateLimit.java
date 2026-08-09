package io.ituknown.redis.annotation;

import io.ituknown.redis.config.RateLimitConfiguration;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启声明式限流功能
 * <p>
 * 开启后就能在方法上用 {@link RateLimit} 注解配置请求频率。</p>
 * <p>
 * 使用前要先在容器里注册 {@link RedissonClient} Bean；没有的话，启动会直接报错。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RateLimitConfiguration.class)
public @interface EnableRateLimit {

    /**
     * 配置切面执行顺序，数值越小优先级越高（越靠外层执行），默认 {@code Ordered.HIGHEST_PRECEDENCE + 1}（最外层）。
     * <p>
     * 默认取最高优先级：限流通常希望尽量靠前，在打开事务、访问数据等耗资源操作之前就拒绝越界请求，故默认放最外层。</p>
     * <p>
     * 默认值退一格为 {@code HIGHEST_PRECEDENCE + 1} 而非字面的 {@link Ordered#HIGHEST_PRECEDENCE}（即
     * {@code Integer.MIN_VALUE}）：带参数绑定（{@code @annotation(..)}）的切面若取 MIN_VALUE，会与 Spring 内部的
     * {@code ExposeInvocationInterceptor}（同样占据该顺序）冲突，运行期抛
     * "Required to bind N arguments, but only bound M (JoinPointMatch was NOT bound in invocation)"。退一格既近似最外层又避开冲突。</p>
     * <p>
     * 若希望限流晚于某些切面生效（靠内层），把本值调大即可，例如 {@code order = 0}；
     * 与 {@code @EnableDistributedLock} 同时启用且需明确二者先后时，请为它们显式指定不同的 order。</p>
     */
    int order() default Ordered.HIGHEST_PRECEDENCE + 1;
}