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
