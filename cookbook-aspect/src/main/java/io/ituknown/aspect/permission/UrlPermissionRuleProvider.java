package io.ituknown.aspect.permission;

import java.util.List;

/**
 * URL 权限规则来源 SPI。
 * <p>
 * 业务方实现此接口提供规则（来自 DB、配置中心、注解扫描等），由 {@link UrlPermissionAspect} 校验时遍历。
 *
 * @author magicianlib@gmail.com
 */
public interface UrlPermissionRuleProvider {

    /**
     * 当前生效的 URL 权限规则；返回 {@code null} 视为无规则（全部放行）。
     */
    List<UrlPermissionRule> rules();
}
