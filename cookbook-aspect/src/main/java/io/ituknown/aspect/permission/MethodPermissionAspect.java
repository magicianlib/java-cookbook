package io.ituknown.aspect.permission;

import io.ituknown.payload.AbstractRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 方法级权限切面：拦截标注 {@link RequirePermission} 的方法，在执行前按注解声明的权限码校验当前用户。
 * <p>
 * 仅拦截「标注 {@code @RequirePermission} 且入参为 {@link AbstractRequest}」的方法。当前用户由
 * {@link PermissionContextResolver} 解析，权限不足抛 {@link io.ituknown.payload.exception.BizForbiddenException}。
 * <p>
 * 由应用 {@code @Bean} 注册，并提供 {@link PermissionContextResolver}。
 *
 * @author magicianlib@gmail.com
 */
@Aspect
public class MethodPermissionAspect extends WebPermissionAspect {

    public MethodPermissionAspect(PermissionContextResolver resolver) {
        super(resolver);
    }

    @Override
    @Pointcut("@annotation(io.ituknown.aspect.permission.RequirePermission) && args(request)")
    public void pointcut(AbstractRequest request) {
    }

    @Override
    protected void preProceed(ProceedingJoinPoint joinPoint, AbstractRequest request) {
        RequirePermission required = ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(RequirePermission.class);
        require(currentContext(), required.value());
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
