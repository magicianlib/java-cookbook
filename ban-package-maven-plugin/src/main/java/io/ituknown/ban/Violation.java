package io.ituknown.ban;

public record Violation(String sourceClass, String referencedType, String matchedRule, String sourceFile) {
}
