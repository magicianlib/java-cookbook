package io.ituknown.ban;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个范围下的禁用补充清单,叠加在基线清单之上,含包前缀与精确类两组。
 */
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
