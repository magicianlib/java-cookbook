package io.ituknown.aspect.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明被标注方法所需的权限码，由 {@link MethodPermissionAspect} 在方法执行前校验。
 * <p>
 * 当前用户（由 {@link PermissionContextResolver} 解析）须持有该 authority，否则抛
 * {@link io.ituknown.payload.exception.BizForbiddenException}，经切面封装为 {@code 000403} 失败 {@code Result}。
 *
 * @author magicianlib@gmail.com
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 所需权限码。
     */
    String value();
}
