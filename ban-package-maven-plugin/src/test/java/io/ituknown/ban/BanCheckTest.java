package io.ituknown.ban;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    private static BanSet banSet(String... packages) {
        BanSet set = new BanSet();
        for (String p : packages) {
            set.getBannedPackages().add(p);
        }
        return set;
    }
}