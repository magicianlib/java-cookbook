package io.ituknown.aspect.permission;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 权限上下文解析 SPI。
 * <p>
 * 根据当前 HTTP 请求解析出调用方的 {@link PermissionContext}（标识 + authority 集合），
 * 对接登录态 / Token 等真实身份来源。由应用提供实现。
 *
 * @author magicianlib@gmail.com
 */
public interface PermissionContextResolver {

    PermissionContext resolve(HttpServletRequest request);
}
