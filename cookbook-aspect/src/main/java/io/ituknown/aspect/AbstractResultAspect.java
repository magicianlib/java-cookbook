package io.ituknown.aspect;

import io.ituknown.payload.AbstractRequest;
import io.ituknown.payload.Result;
import io.ituknown.payload.ResultUtils;
import io.ituknown.payload.exception.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.core.Ordered;

/**
 * 通用方法切面基类（模板方法）。
 * <p>
 * 约束被拦截方法的契约：入参为 {@link AbstractRequest}，返回值为 {@link Result}。
 * <p>
 * 子类需要：
 * <ol>
 *   <li>重写 {@link #pointcut(AbstractRequest)} 并加上 {@code @Pointcut}，自定义拦截规则，参数必须为 request；</li>
 *   <li>标注 {@code @Aspect} 注册为切面，并按需重写 {@link #getOrder()} 指定顺序；</li>
 *   <li>按需重写 {@link #preProceed} / {@link #postProceed} 实现切面逻辑。</li>
 * </ol>
 * <p>
 * {@link #around} 会捕获 {@link #preProceed} 与目标方法抛出的 {@link BizException}（即
 * {@code io.ituknown.payload.exception} 包下的业务异常），经 {@link #toResult(BizException)}
 * 封装为失败 {@link Result} 返回；其他异常原样上抛。
 * <p>
 * 关于执行顺序：本基类实现 {@link Ordered}，默认返回 {@code 0}。子类如需自定义顺序须重写
 * {@link #getOrder()}（注意：基类实现 {@code Ordered} 后，子类上的 {@code @Order} 注解会被覆盖而失效，
 * 必须通过重写 {@code getOrder()} 设置顺序）。
 * <p>
 * 多个此类切面同时存在时，各自的 order 取值须互不相同，否则 Spring AOP 参数绑定会报
 * "Required to bind N arguments, but only bound M (JoinPointMatch was NOT bound in invocation)"。
 *
 * @author magicianlib@gmail.com
 */
public abstract class AbstractResultAspect implements Ordered {

    /**
     * 子类重写此方法并加上 {@code @Pointcut} 注解，自定义拦截规则；参数必须为 request。
     * <p>
     * 示例：
     * <pre>{@code
     *     @Override
     *     @Pointcut("execution(* com.example.facade..*Facade.*(..)) && args(request)")
     *     public void pointcut(AbstractRequest request) {
     *     }
     * }</pre>
     *
     * @param request 被拦截方法的入参，约定为单参 {@link AbstractRequest}
     */
    public abstract void pointcut(AbstractRequest request);

    /**
     * 环绕通知：依次执行 {@link #preProceed}、目标方法、{@link #postProceed}。
     * <p>
     * {@link #preProceed} 与目标方法抛出的 {@link BizException}（即 {@code io.ituknown.payload.exception}
     * 包下的所有业务异常）会被捕获并经 {@link #toResult(BizException)} 封装为失败 {@link Result} 返回，
     * 不再执行 {@link #postProceed}；其他异常原样上抛。
     *
     * @param joinPoint 连接点
     * @param request   被拦截方法的入参，约定为单参 {@link AbstractRequest}
     * @return 目标方法返回的 {@link Result}，或业务异常封装的失败结果
     */
    @Around(value = "pointcut(request)", argNames = "joinPoint,request")
    public Object around(ProceedingJoinPoint joinPoint, AbstractRequest request) throws Throwable {
        Result<?> result;
        try {
            preProceed(joinPoint, request);
            result = (Result<?>) joinPoint.proceed(new Object[]{request});
        } catch (BizException e) {
            // 前置校验或目标方法抛出业务异常时，统一封装为失败 Result 返回，不再执行后置处理
            return toResult(e);
        }

        if (result != null) {
            postProceed(joinPoint, request, result);
        }

        return result;
    }

    /**
     * 将业务异常封装为 {@link Result}，默认取异常携带的 {@link io.ituknown.payload.ResultCode}
     * 与信息（信息为空时回退到响应码默认信息）。
     * <p>
     * 子类可重写以定制异常到结果的映射。
     *
     * @param e 业务异常
     * @return 失败结果
     */
    protected Result<?> toResult(BizException e) {
        return ResultUtils.create(e.getCode(), e.getMessage());
    }

    /**
     * 目标方法执行前调用。子类可在此做前置校验（如鉴权），抛出的异常会原样上抛并终止后续执行。
     *
     * @param joinPoint 连接点
     * @param request   被拦截方法的入参
     */
    protected void preProceed(ProceedingJoinPoint joinPoint, AbstractRequest request) {
    }

    /**
     * 目标方法返回非空结果后调用。子类可在此做后置处理，如填充 traceId、记录日志等。
     *
     * @param joinPoint 连接点
     * @param request   被拦截方法的入参
     * @param result    目标方法返回的结果
     */
    protected void postProceed(ProceedingJoinPoint joinPoint, AbstractRequest request, Result<?> result) {
    }

    @Override
    public int getOrder() {
        return 0;
    }
}