package io.ituknown.ban;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个范围下的禁用补充清单,叠加在基线清单之上,含包前缀与精确类两组。
 */
@Setter
@Getter
public class BanSet {
    private List<String> bannedPackages = new ArrayList<>();
    private List<String> bannedClasses = new ArrayList<>();
}