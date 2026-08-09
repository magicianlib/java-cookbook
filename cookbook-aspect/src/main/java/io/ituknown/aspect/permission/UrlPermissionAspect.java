package io.ituknown.aspect.permission;

import io.ituknown.payload.AbstractRequest;
import io.ituknown.payload.exception.BizForbiddenException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.List;

/**
 * URL 权限切面：拦截 {@code @RestController} 类下「入参为 {@link AbstractRequest}」的方法，
 * 按当前请求 URI 匹配 {@link UrlPermissionRule}，命中则要求当前用户持有对应 authority。
 * <p>
 * 语义为白名单式（镜像参考实现 {@code PermitManager.verify}）：遍历
 * {@link UrlPermissionRuleProvider#rules()}，存在匹配当前 URI 的规则且当前上下文不满足其 authority 时
 * 抛 {@link BizForbiddenException}（由 {@code around()} 封装为 {@code 000403} 失败 {@code Result}）；
 * 无任何规则匹配则放行。
 * <p>
 * 默认切入 {@code @within(RestController)}：此类方法必在 HTTP 请求线程内，{@code RequestContextHolder}
 * 能安全取到当前请求。若用 Facade 包或 {@code @Controller}，子类化并重写 {@link #pointcut}。
 * <p>
 * 由应用 {@code @Bean} 注册，并提供 {@link PermissionContextResolver} 与 {@link UrlPermissionRuleProvider}。
 *
 * @author magicianlib@gmail.com
 */
@Aspect
public class UrlPermissionAspect extends WebPermissionAspect {

    private final UrlPermissionRuleProvider ruleProvider;

    public UrlPermissionAspect(PermissionContextResolver resolver, UrlPermissionRuleProvider ruleProvider) {
        super(resolver);
        this.ruleProvider = ruleProvider;
    }

    @Override
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) && args(request)")
    public void pointcut(AbstractRequest request) {
    }

    @Override
    protected void preProceed(ProceedingJoinPoint joinPoint, AbstractRequest request) {
        String requestUri = currentRequest().getRequestURI();

        List<UrlPermissionRule> rules = ruleProvider.rules();
        if (rules == null) {
            return;
        }

        PermissionContext context = currentContext();
        for (UrlPermissionRule rule : rules) {
            if (rule.matches(requestUri) && !context.hasAuthority(rule.requiredAuthority())) {
                throw new BizForbiddenException("权限不足，URI: " + requestUri + "，需要 authority: " + rule.requiredAuthority());
            }
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
