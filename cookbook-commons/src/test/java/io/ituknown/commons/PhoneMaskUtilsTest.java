package io.ituknown.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PhoneMaskUtilsTest {

    @Test
    public void testMobile() {
        assertEquals("138****5678", PhoneMaskUtils.mobile("13812345678"));
        assertEquals("186****0000", PhoneMaskUtils.mobile("18600000000"));
    }

    @Test
    public void testMobileNullOrInvalid() {
        assertNull(PhoneMaskUtils.mobile(null));
        assertEquals("1381234567", PhoneMaskUtils.mobile("1381234567"));
        assertEquals("138123456789", PhoneMaskUtils.mobile("138123456789"));
        assertEquals("138****5678", PhoneMaskUtils.mobile(" 13812345678 "));
    }

    @Test
    public void testCustom() {
        assertEquals("138******78", PhoneMaskUtils.custom("13812345678", 3, 2));
        assertEquals("1**********", PhoneMaskUtils.custom("13812345678", 1, 0));
        assertEquals("*********78", PhoneMaskUtils.custom("13812345678", 0, 2));
    }

    @Test
    public void testCustomNullOrBlank() {
        assertEquals("", PhoneMaskUtils.custom(null, 3, 4));
        assertEquals("", PhoneMaskUtils.custom("  ", 3, 4));
    }

    @Test
    public void testCustomHeadTailExceedsLength() {
        assertEquals("12345", PhoneMaskUtils.custom("12345", 3, 3));
    }

    @Test
    public void testFixedPhone() {
        assertEquals("021-****1234", PhoneMaskUtils.fixedPhone("021-62231234"));
        assertEquals("010-****5678", PhoneMaskUtils.fixedPhone("010-12345678"));
    }

    @Test
    public void testFixedPhoneShortNumber() {
        assertEquals("010-***456", PhoneMaskUtils.fixedPhone("010-123456"));
        assertEquals("010-**345", PhoneMaskUtils.fixedPhone("010-12345"));
    }

    @Test
    public void testFixedPhoneNullOrBlank() {
        assertNull(PhoneMaskUtils.fixedPhone(null));
        assertEquals("  ", PhoneMaskUtils.fixedPhone("  "));
        assertEquals("12345678", PhoneMaskUtils.fixedPhone("12345678"));
    }

    @Test
    public void testFixedPhoneWithTail() {
        assertEquals("021-****1234", PhoneMaskUtils.fixedPhone("021-62231234", 4));
        assertEquals("021-****234", PhoneMaskUtils.fixedPhone("021-62231234", 3));
    }

    @Test
    public void testFixedPhoneTailExceedsNumberLength() {
        assertEquals("021-123456", PhoneMaskUtils.fixedPhone("021-123456", 6));
    }

    @Test
    public void testAutoMobile() {
        assertEquals("138****5678", PhoneMaskUtils.auto("13812345678"));
    }

    @Test
    public void testAutoFixedPhone() {
        assertEquals("021-****1234", PhoneMaskUtils.auto("021-62231234"));
    }

    @Test
    public void testAutoOtherLength() {
        assertEquals("138*****78", PhoneMaskUtils.auto("1381234567"));
    }

    @Test
    public void testAutoNullOrBlank() {
        assertNull(PhoneMaskUtils.auto(null));
        assertEquals("  ", PhoneMaskUtils.auto("  "));
    }
}
