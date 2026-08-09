# cookbook-aspect

通用 Spring AOP 切面基类模块，为「入参 `io.ituknown.payload.AbstractRequest`、返回值 `io.ituknown.payload.Result`」的方法提供可继承的模板基类 `AbstractResultAspect`。

业务方继承该基类后，只需声明自己的 `@Pointcut` 拦截规则，并按需重写 `preProceed`（前置校验，如鉴权）与 `postProceed`（后置处理），无需重复编写环绕通知样板代码。

## 契约

被拦截的方法须满足：

- 单个入参，类型为 `AbstractRequest`（或其子类）；
- 返回值为 `Result`（或其子类）。

## 子类编写示例

```java
@Aspect
@Order(10)
@Component
public class AuthAspect extends AbstractResultAspect {

    @Override
    @Pointcut("execution(* com.example.facade..*Facade.*(..)) && args(request)")
    public void pointcut(AbstractRequest request) {
    }

    @Override
    protected void preProceed(ProceedingJoinPoint joinPoint, AbstractRequest request) {
        // 前置校验：鉴权失败直接抛异常终止目标方法
        // 抛出的异常由全局异常处理器转换为 Result
    }

    @Override
    protected void postProceed(ProceedingJoinPoint joinPoint, AbstractRequest request, Result<?> result) {
        // 后置处理：填充 traceId、记录日志等
    }
}
```

## 执行流程

`AbstractResultAspect.around` 依次执行：

1. `preProceed`：前置校验；
2. 目标方法；
3. `postProceed`：仅当目标方法返回非空 `Result` 时调用。

`preProceed` 与目标方法抛出的 `BizException`（即 `io.ituknown.payload.exception` 包下的所有业务异常）会被捕获并封装为失败 `Result` 返回，不再执行 `postProceed`；可重写 `toResult(BizException)` 定制异常到结果的映射。其他异常原样上抛。

## 权限切面

模块在基类之上内置两个权限切面（包 `io.ituknown.aspect.permission`），参考 `aspect-permit` 的权限模型，但适配 Jakarta（Spring Boot 3），并复用 `around()` 的异常封装：权限不足直接抛 `BizForbiddenException`，由切面自动封装为 `000403` 失败 `Result`，无需独立的 `PermitDeniedException` 与异常处理器。

两者共享 `WebPermissionAspect` 中间基类提供的「取当前请求 → 解析上下文 → 校验权限」能力，当前用户由 SPI `PermissionContextResolver` 解析。

### 方法权限：`MethodPermissionAspect`

在方法上标注 `@RequirePermission("权限码")`，切面执行前校验当前用户是否持有该 authority。

```java
@RestController
public class UserController {

    @RequirePermission("user:read")
    public Result<User> detail(UserRequest request) {
        // ...
    }
}
```

切入点为 `@annotation(RequirePermission) && args(request)`，天然通用，不依赖具体包。order 为 `10`。

### URL 权限：`UrlPermissionAspect`

按当前请求 URI 匹配 `UrlPermissionRule`（Ant 模式 + 所需 authority），语义为白名单式：存在匹配规则且当前用户不满足其 authority 时拒绝，无任何规则匹配则放行。规则由 SPI `UrlPermissionRuleProvider` 提供（来自 DB、配置中心、注解扫描等）。order 为 `20`。

```java
public class DbRuleProvider implements UrlPermissionRuleProvider {
    @Override
    public List<UrlPermissionRule> rules() {
        return List.of(
            new UrlPermissionRule("/api/users/**", "user:read"),
            new UrlPermissionRule("/api/admin/**", "admin")
        );
    }
}
```

默认切入点为 `@within(RestController) && args(request)`：此类方法必在 HTTP 请求线程内，`RequestContextHolder` 能安全取到当前请求。若用 Facade 包或 `@Controller`，子类化并重写 `pointcut()`。

### 应用须提供的 Bean

两个切面均不自动注册（不加 `@Component`），由应用按需 `@Bean` 注册，并提供对应 SPI：

```java
@Bean
public PermissionContextResolver permissionContextResolver() {
    // 对接登录态 / Token，返回 PermissionContext(principal, authorities)
}

@Bean
public MethodPermissionAspect methodPermissionAspect(PermissionContextResolver resolver) {
    return new MethodPermissionAspect(resolver);
}

@Bean
public UrlPermissionRuleProvider urlPermissionRuleProvider() {
    // 提供 URL 权限规则
}

@Bean
public UrlPermissionAspect urlPermissionAspect(PermissionContextResolver resolver,
                                               UrlPermissionRuleProvider ruleProvider) {
    return new UrlPermissionAspect(resolver, ruleProvider);
}
```

应用须开启 AspectJ 自动代理（见下节）。两个切面共存时 order 已分别为 `10` / `20`，互不相同，可安全并存。

## 开启 AOP

消费方应用需开启 AspectJ 自动代理。Spring Boot 引入 `spring-boot-starter-aop` 即自动开启；普通 Spring 应用需添加 `@EnableAspectJAutoProxy`。

## 关于 @Order

`AbstractResultAspect` 实现 `org.springframework.core.Ordered`，默认返回 `0`。子类如需自定义顺序须重写 `getOrder()`（注意：基类实现 `Ordered` 后，子类上的 `@Order` 注解会被覆盖而失效，必须通过重写 `getOrder()` 设置顺序）。

注意：多个此类切面同时存在时，各自的 order 取值须互不相同，否则 Spring AOP 参数绑定会报 "JoinPointMatch was NOT bound in invocation"。
