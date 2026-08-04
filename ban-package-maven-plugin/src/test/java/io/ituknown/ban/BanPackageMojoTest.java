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
