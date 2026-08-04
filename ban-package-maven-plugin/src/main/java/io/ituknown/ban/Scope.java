package io.ituknown.ban;

/**
 * 检查范围,决定扫描哪些已编译产物。
 */
public enum Scope {
    /**
     * 仅扫描项目自身编译产物(主代码,按需含测试代码)。
     */
    PROJECT,
    /**
     * 在自身产物之外,额外扫描全部依赖 jar。
     */
    GLOBAL
}