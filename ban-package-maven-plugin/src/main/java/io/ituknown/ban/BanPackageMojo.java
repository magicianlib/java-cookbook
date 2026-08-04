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

/**
 * 禁用包检查目标:绑定到测试类编译阶段,扫描项目已编译产物(按需含依赖 jar)
 * 中对被禁包或类的引用,命中时按配置中断构建或仅告警。
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BanPackageMojo extends AbstractMojo {

    /** 检查范围:仅自身编译产物,或额外纳入依赖 jar;默认仅查自身代码。 */
    @Parameter(property = "ban.scope", defaultValue = "PROJECT")
    private Scope scope;

    /** 基线禁用包前缀清单,两种范围下都生效。 */
    @Parameter
    private List<String> bannedPackages;

    /** 基线禁用类精确清单,两种范围下都生效。 */
    @Parameter
    private List<String> bannedClasses;

    /** 项目级补充清单,仅在全局范围下叠加。 */
    @Parameter
    private BanSet projectBans;

    /** 全局补充清单,仅在项目级范围下叠加。 */
    @Parameter
    private BanSet globalBans;

    /** 是否一并扫描测试编译产物,默认开启。 */
    @Parameter(property = "ban.scanTests", defaultValue = "true")
    private boolean scanTests;

    /** 命中禁用引用时是否中断构建;关闭则仅告警,默认中断。 */
    @Parameter(property = "ban.failOnViolation", defaultValue = "true")
    private boolean failOnViolation;

    /** 是否跳过本次检查,默认不跳过。 */
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
        // 汇总待扫描根:主代码输出目录,按需纳入测试输出,全局范围下再纳入全部依赖产物。
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
