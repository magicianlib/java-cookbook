package io.ituknown.aspect.permission;

import java.util.Set;

/**
 * 一次权限校验的上下文：当前调用方标识与其持有的 authority 集合。
 * <p>
 * 不可变。由 {@link PermissionContextResolver} 根据当前请求 / 登录态构建。
 *
 * @param principal   调用方标识，可为 {@code null}
 * @param authorities 调用方持有的 authority 集合，{@code null} 视为空集
 * @author magicianlib@gmail.com
 */
public record PermissionContext(String principal, Set<String> authorities) {

    public PermissionContext {
        authorities = (authorities == null) ? Set.of() : Set.copyOf(authorities);
    }

    /**
     * 当前上下文是否持有指定 authority。
     * <p>
     * {@code authority} 为 {@code null} 视为无要求，直接放行。
     */
    public boolean hasAuthority(String authority) {
        return authority == null || authorities.contains(authority);
    }
}
