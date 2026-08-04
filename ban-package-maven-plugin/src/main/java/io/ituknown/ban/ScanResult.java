package io.ituknown.ban;

import java.util.Set;

public record ScanResult(String className, String sourceFile, Set<String> referencedTypes) {
}
