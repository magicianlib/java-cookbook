package io.ituknown.aspect.permission;

import org.springframework.util.AntPathMatcher;

/**
 * 一条 URL 权限规则：匹配某 URL 模式的请求，要求调用方持有指定 authority。
 * <p>
 * 不可变。{@link AntPathMatcher} 以静态常量复用，避免每次校验新建（{@code AntPathMatcher} 线程安全）。
 *
 * @param urlPattern        Ant 风格 URL 模式，如 {@code /api/users/**}
 * @param requiredAuthority 命中后所需的 authority
 * @author magicianlib@gmail.com
 */
public record UrlPermissionRule(String urlPattern, String requiredAuthority) {

    /** 类级共享匹配器，线程安全。 */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 当前请求 URI 是否命中本规则的 URL 模式。
     *
     * @param requestUri 请求 URI，{@code null} 返回 {@code false}
     */
    public boolean matches(String requestUri) {
        return requestUri != null && PATH_MATCHER.match(urlPattern, requestUri);
    }
}
