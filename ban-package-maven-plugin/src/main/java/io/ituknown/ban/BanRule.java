package io.ituknown.ban;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 禁用匹配规则:对引用类型先按包前缀匹配,再按精确类匹配;命中后返回可读的命中说明。
 */
public final class BanRule {

    private final Set<String> packagePrefixes;
    private final Set<String> exactClasses;

    public BanRule(Collection<String> bannedPackages, Collection<String> bannedClasses) {
        this.packagePrefixes = new HashSet<>();
        for (String pkg : bannedPackages) {
            this.packagePrefixes.add(toInternal(pkg) + "/");
        }
        this.exactClasses = new HashSet<>();
        for (String cls : bannedClasses) {
            this.exactClasses.add(toInternal(cls));
        }
    }

    /**
     * 命中则返回可读说明(包前缀命中或精确类命中),未命中返回空。
     */
    public String match(String internalName) {
        if (internalName == null) {
            return null;
        }
        for (String prefix : packagePrefixes) {
            if (internalName.startsWith(prefix)) {
                return "package=" + toDotted(stripTrailingSlash(prefix));
            }
        }
        if (exactClasses.contains(internalName)) {
            return "class=" + toDotted(internalName);
        }
        return null;
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    static String toInternal(String dotted) {
        return dotted.replace('.', '/');
    }

    static String toDotted(String internal) {
        return internal.replace('/', '.');
    }
}
