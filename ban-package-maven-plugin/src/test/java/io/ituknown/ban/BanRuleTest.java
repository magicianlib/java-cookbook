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
