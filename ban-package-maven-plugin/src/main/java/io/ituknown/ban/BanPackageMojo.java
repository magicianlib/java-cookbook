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
