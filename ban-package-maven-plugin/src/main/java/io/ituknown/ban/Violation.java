package io.ituknown.ban;

/**
 * 一次禁用引用命中的记录:引用方、被禁类型、命中规则说明、来源源文件。
 */
public record Violation(String sourceClass, String referencedType, String matchedRule, String sourceFile) {
}
