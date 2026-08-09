package io.ituknown.aspect.permission;

import io.ituknown.aspect.AbstractResultAspect;
import io.ituknown.payload.exception.BizForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 基于 Web 请求的权限切面公共基类。
 * <p>
 * 为 {@link MethodPermissionAspect} / {@link UrlPermissionAspect} 提供共享的「取当前请求 →
 * 解析上下文 → 校验权限」能力。继承自 {@link AbstractResultAspect}，故拒绝时抛出的
 * {@link BizForbiddenException} 会被 {@code around()} 自动封装为失败 {@code Result}。
 * <p>
 * 自身不定义 {@code pointcut}，由具体子类提供。
 *
 * @author magicianlib@gmail.com
 */
public abstract class WebPermissionAspect extends AbstractResultAspect {

    private final PermissionContextResolver resolver;

    protected WebPermissionAspect(PermissionContextResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 当前 HTTP 请求。取自 {@link RequestContextHolder}，故本切面须在 Web 请求线程内生效。
     */
    protected HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attrs.getRequest();
    }

    /**
     * 当前调用方的权限上下文。
     */
    protected PermissionContext currentContext() {
        return resolver.resolve(currentRequest());
    }

    /**
     * 校验上下文是否持有指定 authority，不满足则抛 {@link BizForbiddenException}。
     *
     * @param context   当前权限上下文
     * @param authority 所需 authority
     */
    protected void require(PermissionContext context, String authority) {
        if (!context.hasAuthority(authority)) {
            throw new BizForbiddenException("权限不足，需要 authority: " + authority);
        }
    }
}
