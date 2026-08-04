package io.ituknown.ban;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BanCheck {

    private final List<String> baselinePackages;
    private final List<String> baselineClasses;
    private final BanSet projectBans;
    private final BanSet globalBans;

    public BanCheck(List<String> baselinePackages, List<String> baselineClasses,
                    BanSet projectBans, BanSet globalBans) {
        this.baselinePackages = nullSafe(baselinePackages);
        this.baselineClasses = nullSafe(baselineClasses);
        this.projectBans = projectBans == null ? new BanSet() : projectBans;
        this.globalBans = globalBans == null ? new BanSet() : globalBans;
    }

    public BanRule effectiveRule(Scope scope) {
        List<String> packages = new ArrayList<>(baselinePackages);
        List<String> classes = new ArrayList<>(baselineClasses);
        BanSet scoped = scope == Scope.PROJECT ? globalBans : projectBans;
        packages.addAll(scoped.getBannedPackages());
        classes.addAll(scoped.getBannedClasses());
        return new BanRule(packages, classes);
    }

    public List<Violation> check(Scope scope, List<Path> roots) throws IOException {
        BanRule rule = effectiveRule(scope);
        List<Violation> violations = new ArrayList<>();
        ClassFileScanner scanner = new ClassFileScanner();
        for (Path root : roots) {
            for (byte[] bytes : enumerateClasses(root)) {
                ScanResult result = scanner.scan(bytes);
                String sourceClass = BanRule.toDotted(result.className());
                for (String ref : result.referencedTypes()) {
                    String matched = rule.match(ref);
                    if (matched != null) {
                        violations.add(new Violation(sourceClass, BanRule.toDotted(ref), matched, result.sourceFile()));
                    }
                }
            }
        }
        return violations;
    }

    private static List<byte[]> enumerateClasses(Path root) throws IOException {
        List<byte[]> classes = new ArrayList<>();
        if (Files.isDirectory(root)) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
                    try {
                        classes.add(Files.readAllBytes(p));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } else if (root.toString().endsWith(".jar") && Files.isRegularFile(root)) {
            try (ZipFile zip = new ZipFile(root.toFile())) {
                java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        try (InputStream in = zip.getInputStream(entry)) {
                            classes.add(in.readAllBytes());
                        }
                    }
                }
            }
        }
        return classes;
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
