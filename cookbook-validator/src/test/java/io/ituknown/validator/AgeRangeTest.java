package io.ituknown.validator;

import org.junit.jupiter.api.Test;

import java.util.Locale;

public class AgeRangeTest {
    @Test
    public void testEnglish() {
        Locale.setDefault(Locale.ENGLISH); // en
        User u = new User();
        u.setUsername("222");
        u.setAge(500);
        ValidatorUtils.validate(u);
    }

    @Test
    public void testChinese() {
        Locale.setDefault(Locale.CHINESE); // zh
        User u = new User();
        u.setUsername("222");
        u.setAge(-10);
        ValidatorUtils.validate(u);
    }

    @Test
    public void testChina() {
        Locale.setDefault(Locale.CHINA); // zh_CN
        User u = new User();
        u.setUsername("222");
        u.setAge(-10);
        ValidatorUtils.validate(u);
    }
}