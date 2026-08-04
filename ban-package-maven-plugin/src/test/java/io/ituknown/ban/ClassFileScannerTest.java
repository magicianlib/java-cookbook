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

    @Test
    void collectsAnnotationTypes() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(
                "target/test-classes/io/ituknown/ban/fixtures/AnnotatedFixture.class"));

        ScanResult result = new ClassFileScanner().scan(bytes);

        assertTrue(result.referencedTypes().contains("io/ituknown/ban/fixtures/forbidden/ForbiddenAnnotation"),
                "annotation type not collected; refs=" + result.referencedTypes());
    }

    @Test
    void collectsMethodReferenceOwners() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(
                "target/test-classes/io/ituknown/ban/fixtures/MethodRefFixture.class"));

        ScanResult result = new ClassFileScanner().scan(bytes);

        assertTrue(result.referencedTypes().contains("io/ituknown/ban/fixtures/forbidden/ForbiddenStub"),
                "method-reference owner not collected; refs=" + result.referencedTypes());
    }

    @Test
    void collectsGenericSignatureTypes() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(
                "target/test-classes/io/ituknown/ban/fixtures/GenericFixture.class"));

        ScanResult result = new ClassFileScanner().scan(bytes);

        assertTrue(result.referencedTypes().contains("io/ituknown/ban/fixtures/forbidden/ForbiddenStub"),
                "generic signature type not collected; refs=" + result.referencedTypes());
    }

    @Test
    void collectsAnnotationClassArrayValues() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(
                "target/test-classes/io/ituknown/ban/fixtures/AnnotatedValuesFixture.class"));

        ScanResult result = new ClassFileScanner().scan(bytes);

        assertTrue(result.referencedTypes().contains("io/ituknown/ban/fixtures/forbidden/ForbiddenStub"),
                "annotation class-array value not collected; refs=" + result.referencedTypes());
    }
}
