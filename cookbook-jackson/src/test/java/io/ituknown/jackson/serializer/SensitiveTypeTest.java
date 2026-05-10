package io.ituknown.jackson.serializer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveTypeTest {

    @Test
    void nameMask_normal() {
        assertEquals("张**", SensitiveType.NAME.mask("张三丰"));
        assertEquals("李**", SensitiveType.NAME.mask("李四"));
    }

    @Test
    void nameMask_singleChar() {
        // 单字符不匹配正则 (\\S)\\S+，原样返回
        assertEquals("张", SensitiveType.NAME.mask("张"));
    }

    @Test
    void mobileMask_normal() {
        assertEquals("138****5678", SensitiveType.MOBILE.mask("13812345678"));
    }

    @Test
    void emailMask_normal() {
        assertEquals("t****@example.com", SensitiveType.EMAIL.mask("test@example.com"));
    }

    @Test
    void idCardMask_18digits() {
        String id18 = "110101199001011234";
        String masked = SensitiveType.ID_CARD.mask(id18);
        assertTrue(masked.startsWith("110101"));
        assertTrue(masked.endsWith("1234"));
        assertEquals("110101****1234", masked);
    }

    @Test
    void idCardMask_15digits() {
        String id15 = "110101900101123";
        String masked = SensitiveType.ID_CARD.mask(id15);
        assertTrue(masked.startsWith("110101"));
        assertTrue(masked.endsWith("1123"));
        assertEquals("110101****1123", masked);
    }

    @Test
    void addressMask_withComma() {
        // 逗号分隔：保留省市区
        assertEquals("江苏省南京市玄武区****", SensitiveType.ADDRESS.mask("江苏省,南京市,玄武区,某某街道"));
        // 3段：保留省市
        assertEquals("江苏省南京市****", SensitiveType.ADDRESS.mask("江苏省,南京市,玄武区"));
        // 2段：保留省
        assertEquals("江苏省****", SensitiveType.ADDRESS.mask("江苏省,南京市"));
    }

    @Test
    void addressMask_withoutComma_regex() {
        // 无逗号走正则：.+? 非贪婪，匹配第一个 市/区/县 后停止
        assertEquals("北京市****", SensitiveType.ADDRESS.mask("北京市朝阳区建国路"));
        assertEquals("上海市****", SensitiveType.ADDRESS.mask("上海市浦东新区陆家嘴"));
    }

    @Test
    void mask_null() {
        assertNull(SensitiveType.NAME.mask(null));
        assertNull(SensitiveType.MOBILE.mask(null));
    }

    @Test
    void mask_empty() {
        assertEquals("", SensitiveType.NAME.mask(""));
        assertEquals("", SensitiveType.MOBILE.mask(""));
    }
}
