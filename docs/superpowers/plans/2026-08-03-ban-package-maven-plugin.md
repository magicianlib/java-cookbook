# ban-package-maven-plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Maven plugin (`io.ituknown:ban-package-maven-plugin`) with a `check` goal that fails the build when project code references banned packages/classes, detected via ASM bytecode scanning.

**Architecture:** Thin `@Mojo` delegates to POJOs. `BanCheck` resolves effective rules per scope (baseline + scope overlay), `ClassFileScanner` uses ASM to collect every type a `.class` references, `BanRule` matches collected types against banned packages (prefix) and classes (exact). Only the module's own compiled classes are scanned by default (`PROJECT`); `GLOBAL` additionally scans dependency jars.

**Tech Stack:** Java 21, Maven Plugin API 3.9.9, maven-plugin-tools 3.15.1, ASM 9.8, JUnit 5.

## Global Constraints

- Java 21 via inherited `maven.compiler.release=21` (do NOT define `java.version` — it is intentionally unresolved in the root pom; `release` takes precedence and source/target are ignored).
- New module `ban-package-maven-plugin` inherits the root `java-cookbook` parent (groupId `io.ituknown`, version `${revision}` = 1.3.0) and its compiler/flatten config.
- Code in package `io.ituknown.ban`. Comments are business-language only, minimal, self-documenting — no code identifiers, SQL, or expressions in comments.
- TDD: every logic task writes the failing test first, runs it, implements minimally, runs it green, then commits.
- Commits are optional per user preference — skip the commit step if the user has not asked to commit.

## File Structure

- Create: `ban-package-maven-plugin/pom.xml` — module build (packaging `maven-plugin`, deps, descriptor plugin).
- Modify: `pom.xml` (root) — register the new module under `<modules>`.
- Create: `src/main/java/io/ituknown/ban/Scope.java` — enum `PROJECT` / `GLOBAL`.
- Create: `src/main/java/io/ituknown/ban/BanSet.java` — POJO holding a package list + class list (for `projectBans`/`globalBans`).
- Create: `src/main/java/io/ituknown/ban/BanRule.java` — compiled rules; `match(internalName)` returns the matched rule string or null.
- Create: `src/main/java/io/ituknown/ban/BanCheck.java` — orchestrator: effective rule per scope + scan roots into violations.
- Create: `src/main/java/io/ituknown/ban/ScanResult.java` — record: source class, source file, referenced types.
- Create: `src/main/java/io/ituknown/ban/ClassFileScanner.java` — ASM scan of one `.class` into a `ScanResult`.
- Create: `src/main/java/io/ituknown/ban/Violation.java` — record: source class, referenced type, matched rule, source file.
- Create: `src/main/java/io/ituknown/ban/BanPackageMojo.java` — the `@Mojo(name="check")`.
- Tests under `src/test/java/io/ituknown/ban/`: `BanRuleTest`, `BanCheckTest`, `ClassFileScannerTest`, `BanPackageMojoTest`, plus `fixtures/ForbiddenStub.java` and `fixtures/FixtureUsingBanned.java`.

## Task 1: Module scaffolding

**Files:**
- Create: `ban-package-maven-plugin/pom.xml`
- Modify: `pom.xml` (root) — add `<module>ban-package-maven-plugin</module>` as the last entry inside `<modules>` (after `cookbook-log`).

**Interfaces:**
- Produces: a buildable `maven-plugin` module in the reactor.

- [ ] **Step 1: Create the module pom**

`ban-package-maven-plugin/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.ituknown</groupId>
        <artifactId>java-cookbook</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>ban-package-maven-plugin</artifactId>
    <packaging>maven-plugin</packaging>

    <properties>
        <maven.version>3.9.9</maven.version>
        <maven.plugin.tools.version>3.15.1</maven.plugin.tools.version>
        <asm.version>9.8</asm.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-plugin-api</artifactId>
            <version>${maven.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.maven.plugin-tools</groupId>
            <artifactId>maven-plugin-annotations</artifactId>
            <version>${maven.plugin.tools.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-core</artifactId>
            <version>${maven.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.ow2.asm</groupId>
            <artifactId>asm</artifactId>
            <version>${asm.version}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-plugin-plugin</artifactId>
                <version>${maven.plugin.tools.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Register the module in the root pom**

In root `pom.xml`, add inside `<modules>` (after the `cookbook-log` line):

```xml
        <module>ban-package-maven-plugin</module>
```

- [ ] **Step 3: Create the source package directories**

Run from repo root:

```bash
mkdir -p ban-package-maven-plugin/src/main/java/io/ituknown/ban
mkdir -p ban-package-maven-plugin/src/test/java/io/ituknown/ban/fixtures/forbidden
```

- [ ] **Step 4: Verify the module builds empty**

Run: `mvn -q -pl ban-package-maven-plugin -am package`
Expected: BUILD SUCCESS. The `maven-plugin` packaging runs `maven-plugin-plugin:descriptor`; with no `@Mojo` yet it produces a minimal descriptor. This proves the module is wired correctly into the reactor.

- [ ] **Step 5: Commit (optional)**

```bash
git add ban-package-maven-plugin/pom.xml pom.xml
git commit -m "chore: scaffold ban-package-maven-plugin module"
```

## Task 2: BanRule (matching logic)

**Files:**
- Create: `src/main/java/io/ituknown/ban/BanRule.java`
- Test: `src/test/java/io/ituknown/ban/BanRuleTest.java`

**Interfaces:**
- Produces: `BanRule(Collection<String> bannedPackages, Collection<String> bannedClasses)`; `String match(String internalName)` returns `"package=<dotted>"` / `"class=<dotted>"` or `null`; static helpers `toInternal(String)`, `toDotted(String)`.

- [ ] **Step 1: Write the failing test**

`src/test/java/io/ituknown/ban/BanRuleTest.java`:

```java
package io.ituknown.ban;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BanRuleTest {

    @Test
    void packagePrefixMatchesClassesAndSubpackages() {
        BanRule rule = new BanRule(List.of("com.alibaba.fastjson"), List.of());
        assertEquals("package=com.alibaba.fastjson", rule.match("com/alibaba/fastjson/JSON"));
        assertEquals("package=com.alibaba.fastjson", rule.match("com/alibaba/fastjson/JSONObject"));
    }

    @Test
    void packagePrefixDoesNotMatchSiblingSegment() {
        BanRule rule = new BanRule(List.of("com.alibaba.fastjson"), List.of());
        assertNull(rule.match("com/alibaba/fastjsonx/Foo"));
    }

    @Test
    void exactClassDoesNotMatchNestedClass() {
        BanRule rule = new BanRule(List.of(), List.of("com.alibaba.fastjson.JSON"));
        assertEquals("class=com.alibaba.fastjson.JSON", rule.match("com/alibaba/fastjson/JSON"));
        assertNull(rule.match("com/alibaba/fastjson/JSON$Node"));
    }

    @Test
    void packagePrefixMatchesNestedClass() {
        BanRule rule = new BanRule(List.of("com.alibaba.fastjson"), List.of());
        assertEquals("package=com.alibaba.fastjson", rule.match("com/alibaba/fastjson/JSON$Node"));
    }

    @Test
    void returnsNullWhenNothingBanned() {
        BanRule rule = new BanRule(List.of(), List.of());
        assertNull(rule.match("com/alibaba/fastjson/JSON"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=BanRuleTest`
Expected: compilation failure — `BanRule` does not exist.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/io/ituknown/ban/BanRule.java`:

```java
package io.ituknown.ban;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=BanRuleTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit (optional)**

```bash
git add ban-package-maven-plugin/src
git commit -m "feat(ban): add BanRule package/class matching"
```

## Task 3: Scope, BanSet, and effective-rule resolution

**Files:**
- Create: `src/main/java/io/ituknown/ban/Scope.java`
- Create: `src/main/java/io/ituknown/ban/BanSet.java`
- Create: `src/main/java/io/ituknown/ban/BanCheck.java`
- Test: `src/test/java/io/ituknown/ban/BanCheckTest.java`

**Interfaces:**
- Consumes: `BanRule` (Task 2).
- Produces: `Scope` enum; `BanSet` POJO (`getBannedPackages()`, `getBannedClasses()`); `BanCheck(baselinePackages, baselineClasses, projectBans, globalBans)` with `BanRule effectiveRule(Scope)`.

- [ ] **Step 1: Write the failing test**

`src/test/java/io/ituknown/ban/BanCheckTest.java`:

```java
package io.ituknown.ban;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BanCheckTest {

    @Test
    void projectScopeAppliesBaselineAndGlobalBans() {
        BanSet empty = new BanSet();
        BanSet globalBans = banSet("org.joda.time");
        BanCheck check = new BanCheck(
                List.of("com.alibaba.fastjson"), List.of(), empty, globalBans);

        BanRule rule = check.effectiveRule(Scope.PROJECT);

        assertNotNull(rule.match("com/alibaba/fastjson/JSON"));
        assertNotNull(rule.match("org/joda/time/DateTime"));
        assertNull(rule.match("org/bouncycastle/BC"));
    }

    @Test
    void globalScopeAppliesBaselineAndProjectBans() {
        BanSet projectBans = banSet("org.bouncycastle");
        BanSet empty = new BanSet();
        BanCheck check = new BanCheck(
                List.of("com.alibaba.fastjson"), List.of(), projectBans, empty);

        BanRule rule = check.effectiveRule(Scope.GLOBAL);

        assertNotNull(rule.match("com/alibaba/fastjson/JSON"));
        assertNotNull(rule.match("org/bouncycastle/BC"));
        assertNull(rule.match("org/joda/time/DateTime"));
    }

    private static BanSet banSet(String... packages) {
        BanSet set = new BanSet();
        for (String p : packages) {
            set.getBannedPackages().add(p);
        }
        return set;
    }
}
```

Note: the inverted mapping is intentional (per design) — `scope=PROJECT` pulls in `globalBans`, `scope=GLOBAL` pulls in `projectBans`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=BanCheckTest`
Expected: compilation failure — `Scope`, `BanSet`, `BanCheck` do not exist.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/io/ituknown/ban/Scope.java`:

```java
package io.ituknown.ban;

public enum Scope {
    PROJECT,
    GLOBAL
}
```

`src/main/java/io/ituknown/ban/BanSet.java`:

```java
package io.ituknown.ban;

import java.util.ArrayList;
import java.util.List;

public class BanSet {

    private List<String> bannedPackages = new ArrayList<>();
    private List<String> bannedClasses = new ArrayList<>();

    public List<String> getBannedPackages() {
        return bannedPackages;
    }

    public void setBannedPackages(List<String> bannedPackages) {
        this.bannedPackages = bannedPackages;
    }

    public List<String> getBannedClasses() {
        return bannedClasses;
    }

    public void setBannedClasses(List<String> bannedClasses) {
        this.bannedClasses = bannedClasses;
    }
}
```

`src/main/java/io/ituknown/ban/BanCheck.java`:

```java
package io.ituknown.ban;

import java.util.ArrayList;
import java.util.List;

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

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=BanCheckTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit (optional)**

```bash
git add ban-package-maven-plugin/src
git commit -m "feat(ban): add Scope, BanSet, effective-rule resolution"
```

## Task 4: ClassFileScanner (ASM bytecode scan)

**Files:**
- Create: `src/main/java/io/ituknown/ban/ScanResult.java`
- Create: `src/main/java/io/ituknown/ban/ClassFileScanner.java`
- Create: `src/test/java/io/ituknown/ban/fixtures/forbidden/ForbiddenStub.java`
- Create: `src/test/java/io/ituknown/ban/fixtures/FixtureUsingBanned.java`
- Test: `src/test/java/io/ituknown/ban/ClassFileScannerTest.java`

**Interfaces:**
- Produces: `ScanResult(String className, String sourceFile, Set<String> referencedTypes)`; `ClassFileScanner.scan(byte[])` returns a `ScanResult`.

- [ ] **Step 1: Create the test fixtures (types the scanner must detect)**

`src/test/java/io/ituknown/ban/fixtures/forbidden/ForbiddenStub.java`:

```java
package io.ituknown.ban.fixtures.forbidden;

public class ForbiddenStub {
    public static String hello() {
        return "banned";
    }
}
```

`src/test/java/io/ituknown/ban/fixtures/FixtureUsingBanned.java`:

```java
package io.ituknown.ban.fixtures;

import io.ituknown.ban.fixtures.forbidden.ForbiddenStub;

public class FixtureUsingBanned {

    private ForbiddenStub stub;

    public ForbiddenStub get() {
        return stub;
    }

    public String greet() {
        return ForbiddenStub.hello();
    }
}
```

- [ ] **Step 2: Write the failing test**

`src/test/java/io/ituknown/ban/ClassFileScannerTest.java`:

```java
package io.ituknown.ban;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFileScannerTest {

    private static final Path FIXTURE = Paths.get(
            "target/test-classes/io/ituknown/ban/fixtures/FixtureUsingBanned.class");

    @Test
    void collectsAllReferencedTypes() throws Exception {
        byte[] bytes = Files.readAllBytes(FIXTURE);

        ScanResult result = new ClassFileScanner().scan(bytes);

        assertEquals("io/ituknown/ban/fixtures/FixtureUsingBanned", result.className());
        Set<String> refs = result.referencedTypes();
        assertTrue(refs.contains("io/ituknown/ban/fixtures/forbidden/ForbiddenStub"),
                "missing expected reference; refs=" + refs);
    }
}
```

The fixture `.class` exists at test time because `test-compile` runs before `test` in the Maven lifecycle.

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=ClassFileScannerTest`
Expected: compilation failure — `ScanResult`, `ClassFileScanner` do not exist.

- [ ] **Step 4: Write minimal implementation**

`src/main/java/io/ituknown/ban/ScanResult.java`:

```java
package io.ituknown.ban;

import java.util.Set;

public record ScanResult(String className, String sourceFile, Set<String> referencedTypes) {
}
```

`src/main/java/io/ituknown/ban/ClassFileScanner.java`:

```java
package io.ituknown.ban;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Set;

public class ClassFileScanner {

    public ScanResult scan(byte[] bytes) {
        Collector collector = new Collector();
        new ClassReader(bytes).accept(collector, 0);
        return new ScanResult(collector.className, collector.sourceFile, collector.types);
    }

    private static final class Collector extends ClassVisitor {

        final Set<String> types = new HashSet<>();
        String className;
        String sourceFile;

        Collector() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            className = name;
            addInternal(superName);
            if (interfaces != null) {
                for (String i : interfaces) {
                    addInternal(i);
                }
            }
        }

        @Override
        public void visitSource(String source, String debug) {
            sourceFile = source;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            addDescriptor(descriptor);
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            addDescriptor(descriptor);
            if (exceptions != null) {
                for (String e : exceptions) {
                    addInternal(e);
                }
            }
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitTypeInsn(int opcode, String type) {
                    addInternal(type);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    addInternal(owner);
                    addDescriptor(descriptor);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name,
                                            String descriptor, boolean isInterface) {
                    addInternal(owner);
                    addDescriptor(descriptor);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor,
                                                   Object bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                    addDescriptor(descriptor);
                    for (Object arg : bootstrapMethodArguments) {
                        if (arg instanceof Type t) {
                            addElementType(t);
                        }
                    }
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof Type t) {
                        addElementType(t);
                    }
                }
            };
        }

        private void addInternal(String name) {
            if (name == null || name.isEmpty()) {
                return;
            }
            if (name.charAt(0) == '[') {
                addElementType(Type.getType(name));
            } else {
                types.add(name);
            }
        }

        private void addDescriptor(String descriptor) {
            if (descriptor == null || descriptor.isEmpty()) {
                return;
            }
            Type t = descriptor.charAt(0) == '(' ? Type.getMethodType(descriptor) : Type.getType(descriptor);
            if (t.getSort() == Type.METHOD) {
                for (Type arg : t.getArgumentTypes()) {
                    addElementType(arg);
                }
                addElementType(t.getReturnType());
            } else {
                addElementType(t);
            }
        }

        private void addElementType(Type t) {
            if (t == null) {
                return;
            }
            Type element = t.getElementType();
            if (element.getSort() == Type.OBJECT) {
                types.add(element.getInternalName());
            }
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=ClassFileScannerTest`
Expected: PASS.

- [ ] **Step 6: Commit (optional)**

```bash
git add ban-package-maven-plugin/src
git commit -m "feat(ban): add ASM ClassFileScanner for referenced-type collection"
```

## Task 5: BanCheck.check (scan roots into violations)

**Files:**
- Create: `src/main/java/io/ituknown/ban/Violation.java`
- Modify: `src/main/java/io/ituknown/ban/BanCheck.java` — add `check(Scope, List<Path>)`.
- Test: extend `src/test/java/io/ituknown/ban/BanCheckTest.java` with a `check` test.

**Interfaces:**
- Consumes: `ClassFileScanner.scan(byte[])` (Task 4), `BanRule` (Task 2).
- Produces: `Violation(String sourceClass, String referencedType, String matchedRule, String sourceFile)`; `BanCheck.check(Scope, List<Path> roots)` returns `List<Violation>`.

- [ ] **Step 1: Write the failing test**

Append to `src/test/java/io/ituknown/ban/BanCheckTest.java`. Add these imports (`Violation` is in the same package, so it needs no import; `List` is already imported in Task 3):

```java
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertTrue;
```

Add this test method to the class:

```java
    @Test
    void checkFindsViolationWhenBannedPackageReferenced() throws Exception {
        BanCheck check = new BanCheck(
                List.of("io.ituknown.ban.fixtures.forbidden"), List.of(), new BanSet(), new BanSet());

        List<Violation> violations = check.check(Scope.PROJECT, List.of(
                Paths.get("target/test-classes/io/ituknown/ban/fixtures")));

        assertTrue(violations.stream().anyMatch(v ->
                        v.referencedType().equals("io.ituknown.ban.fixtures.forbidden.ForbiddenStub")),
                "expected violation missing; violations=" + violations);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=BanCheckTest`
Expected: compilation failure — `Violation` and `BanCheck.check` do not exist.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/io/ituknown/ban/Violation.java`:

```java
package io.ituknown.ban;

public record Violation(String sourceClass, String referencedType, String matchedRule, String sourceFile) {
}
```

Add to `BanCheck.java` (new imports + method). Imports to add:

```java
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
```

Add the method to the `BanCheck` class:

```java
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
```

Note: the `GLOBAL` jar branch uses `InputStream` from `zip.getInputStream(entry)`, so the `java.io.InputStream` import is required.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=BanCheckTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit (optional)**

```bash
git add ban-package-maven-plugin/src
git commit -m "feat(ban): scan class roots into violations"
```

## Task 6: BanPackageMojo (the check goal)

**Files:**
- Create: `src/main/java/io/ituknown/ban/BanPackageMojo.java`
- Test: `src/test/java/io/ituknown/ban/BanPackageMojoTest.java`

**Interfaces:**
- Consumes: `BanCheck`, `Scope`, `BanSet` (earlier tasks).
- Produces: the `check` Mojo with `@Parameter` fields `scope`, `bannedPackages`, `bannedClasses`, `projectBans`, `globalBans`, `scanTests`, `failOnViolation`, `skip`, and `project`.

- [ ] **Step 1: Write the failing test**

`src/test/java/io/ituknown/ban/BanPackageMojoTest.java`:

```java
package io.ituknown.ban;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BanPackageMojoTest {

    private static final String FIXTURE_OUTPUT =
            "target/test-classes/io/ituknown/ban/fixtures";

    @Test
    void failsBuildWhenBannedPackageIsReferenced() throws Exception {
        BanPackageMojo mojo = newMojo(FIXTURE_OUTPUT, List.of("io.ituknown.ban.fixtures.forbidden"));
        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void passesWhenNothingIsBanned() throws Exception {
        BanPackageMojo mojo = newMojo(FIXTURE_OUTPUT, List.of());
        assertDoesNotThrow(mojo::execute);
    }

    private static BanPackageMojo newMojo(String outputDir, List<String> bannedPackages) throws Exception {
        BanPackageMojo mojo = new BanPackageMojo();
        Model model = new Model();
        model.setBuild(new Build());
        model.getBuild().setOutputDirectory(outputDir);
        MavenProject project = new MavenProject(model);
        set(mojo, "project", project);
        set(mojo, "bannedPackages", bannedPackages);
        set(mojo, "scope", Scope.PROJECT);
        set(mojo, "failOnViolation", true);
        set(mojo, "scanTests", false);
        return mojo;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
```

Note: this test instantiates the Mojo directly, so Maven never runs `@Parameter` injection — `@Parameter(defaultValue=...)` does NOT apply. Java's defaults take over: primitive `failOnViolation` would be `false` (Mojo would warn, not throw) and `scope` would be `null`. That is why the helper sets `scope`, `failOnViolation`, and `scanTests` explicitly.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=BanPackageMojoTest`
Expected: compilation failure — `BanPackageMojo` does not exist.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/io/ituknown/ban/BanPackageMojo.java`:

```java
package io.ituknown.ban;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BanPackageMojo extends AbstractMojo {

    @Parameter(property = "ban.scope", defaultValue = "PROJECT")
    private Scope scope;

    @Parameter
    private List<String> bannedPackages;

    @Parameter
    private List<String> bannedClasses;

    @Parameter
    private BanSet projectBans;

    @Parameter
    private BanSet globalBans;

    @Parameter(property = "ban.scanTests", defaultValue = "true")
    private boolean scanTests;

    @Parameter(property = "ban.failOnViolation", defaultValue = "true")
    private boolean failOnViolation;

    @Parameter(property = "ban.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("ban-package check skipped");
            return;
        }

        BanCheck banCheck = new BanCheck(bannedPackages, bannedClasses, projectBans, globalBans);
        List<Path> roots = new ArrayList<>();
        roots.add(Paths.get(project.getBuild().getOutputDirectory()));
        if (scanTests) {
            String testOutput = project.getBuild().getTestOutputDirectory();
            if (testOutput != null) {
                roots.add(Paths.get(testOutput));
            }
        }
        if (scope == Scope.GLOBAL) {
            for (Artifact artifact : project.getArtifacts()) {
                if (artifact.getFile() != null) {
                    roots.add(artifact.getFile().toPath());
                }
            }
        }

        List<Violation> violations;
        try {
            violations = banCheck.check(scope, roots);
        } catch (IOException e) {
            throw new MojoExecutionException("扫描类文件失败", e);
        }

        if (violations.isEmpty()) {
            getLog().info("ban-package check passed: 0 violations");
            return;
        }
        for (Violation v : violations) {
            getLog().error(format(v));
        }
        String summary = "发现 " + violations.size() + " 处禁用包引用";
        if (failOnViolation) {
            throw new MojoExecutionException(summary);
        }
        getLog().warn(summary + "（仅告警，未中断构建）");
    }

    private static String format(Violation v) {
        return String.format("禁用包违规：%s 引用了被禁类型 %s（命中规则 %s，来源文件 %s）",
                v.sourceClass(), v.referencedType(), v.matchedRule(), v.sourceFile());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl ban-package-maven-plugin test -Dtest=BanPackageMojoTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Run the full module test suite and verify the descriptor**

Run: `mvn -q -pl ban-package-maven-plugin test`
Expected: all tests PASS.

Then verify the plugin descriptor was generated with the `check` goal:

```bash
grep -o '<goal>check</goal>' ban-package-maven-plugin/target/classes/META-INF/maven/plugin.xml
```

Expected: prints `<goal>check</goal>`.

- [ ] **Step 6: Commit (optional)**

```bash
git add ban-package-maven-plugin/src
git commit -m "feat(ban): add BanPackageMojo check goal"
```

## Task 7: End-to-end verification (real parameter binding)

This task exercises Maven's real XML→field binding (which unit tests bypass) plus the full goal in a real build.

**Files:**
- Create (throwaway, under `target/` so it is gitignored): `ban-package-maven-plugin/target/it-smoke/pom.xml`, `.../src/main/java/io/ituknown/smoke/forbidden/Forbidden.java`, `.../src/main/java/io/ituknown/smoke/App.java`.

- [ ] **Step 1: Install the plugin to the local repo**

Run from repo root: `mvn -q -pl ban-package-maven-plugin -am install`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Create the smoke sample project**

`ban-package-maven-plugin/target/it-smoke/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.ituknown.smoke</groupId>
    <artifactId>ban-smoke</artifactId>
    <version>1</version>
    <packaging>jar</packaging>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.14.1</version>
                <configuration>
                    <release>21</release>
                </configuration>
            </plugin>
            <plugin>
                <groupId>io.ituknown</groupId>
                <artifactId>ban-package-maven-plugin</artifactId>
                <version>1.3.0</version>
                <executions>
                    <execution>
                        <phase>process-classes</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <bannedPackages>
                        <bannedPackage>io.ituknown.smoke.forbidden</bannedPackage>
                    </bannedPackages>
                    <scanTests>false</scanTests>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

`ban-package-maven-plugin/target/it-smoke/src/main/java/io/ituknown/smoke/forbidden/Forbidden.java`:

```java
package io.ituknown.smoke.forbidden;

public class Forbidden {
    public static String hello() {
        return "x";
    }
}
```

`ban-package-maven-plugin/target/it-smoke/src/main/java/io/ituknown/smoke/App.java`:

```java
package io.ituknown.smoke;

import io.ituknown.smoke.forbidden.Forbidden;

public class App {
    public static void main(String[] args) {
        System.out.println(Forbidden.hello());
    }
}
```

- [ ] **Step 3: Run the smoke build — expect FAILURE**

Run:

```bash
cd ban-package-maven-plugin/target/it-smoke && mvn -q process-classes; cd -
```

Expected: BUILD FAILURE, and the log contains `禁用包违规` referencing `App` and `io.ituknown.smoke.forbidden.Forbidden`. This proves real XML binding (`bannedPackages`, `scanTests`) and the full goal work.

- [ ] **Step 4: Remove the banned usage — expect SUCCESS**

Edit `App.java` to drop the forbidden reference:

```java
package io.ituknown.smoke;

public class App {
    public static void main(String[] args) {
        System.out.println("clean");
    }
}
```

Run:

```bash
cd ban-package-maven-plugin/target/it-smoke && mvn -q process-classes; cd -
```

Expected: BUILD SUCCESS, log contains `ban-package check passed: 0 violations`.

- [ ] **Step 5: Clean up the throwaway sample**

Run: `mvn -q -pl ban-package-maven-plugin clean` (removes `target/`, including the smoke project). Expected: SUCCESS.

- [ ] **Step 6: Commit (optional)**

No source changes in this task; if any tweaks were made to fix binding issues, commit them:

```bash
git add -A
git commit -m "fix(ban): correct parameter binding per E2E"
```

## Done

The plugin is complete and verified. To use it in any module of this repo, add to that module's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.ituknown</groupId>
            <artifactId>ban-package-maven-plugin</artifactId>
            <version>${revision}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <bannedPackages>
                    <bannedPackage>com.alibaba.fastjson</bannedPackage>
                </bannedPackages>
                <bannedClasses>
                    <bannedClass>com.alibaba.fastjson.JSON</bannedClass>
                </bannedClasses>
            </configuration>
        </plugin>
    </plugins>
</build>
```
