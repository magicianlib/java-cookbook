package io.ituknown.validator;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class AgeRangeTest {

    // ========== validate / 正向测试 ==========

    @Test
    void validate_validUser() {
        User u = new User();
        u.setUsername("alice");
        u.setAge(25);
        assertDoesNotThrow(() -> ValidatorUtils.validate(u));
    }

    @Test
    void validate_nullAge() {
        User u = new User();
        u.setUsername("bob");
        u.setAge(null);
        assertDoesNotThrow(() -> ValidatorUtils.validate(u));
    }

    @Test
    void validate_ageAtMinBoundary() {
        User u = new User();
        u.setUsername("charlie");
        u.setAge(0);
        assertDoesNotThrow(() -> ValidatorUtils.validate(u));
    }

    @Test
    void validate_ageAtMaxBoundary() {
        User u = new User();
        u.setUsername("dave");
        u.setAge(200);
        assertDoesNotThrow(() -> ValidatorUtils.validate(u));
    }

    // ========== validate / 反向测试 ==========

    @Test
    void validate_ageBelowMin() {
        User u = new User();
        u.setUsername("eve");
        u.setAge(-1);
        assertThrows(ConstraintViolationException.class, () -> ValidatorUtils.validate(u));
    }

    @Test
    void validate_ageAboveMax() {
        User u = new User();
        u.setUsername("frank");
        u.setAge(201);
        assertThrows(ConstraintViolationException.class, () -> ValidatorUtils.validate(u));
    }

    // ========== validate / Locale ==========

    @Test
    void validate_englishLocale() {
        User u = new User();
        u.setUsername("alice");
        u.setAge(500);
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> ValidatorUtils.validate(Locale.ENGLISH, u));
        assertTrue(ex.getMessage().toLowerCase().contains("age"));
    }

    @Test
    void validate_chineseLocale() {
        User u = new User();
        u.setUsername("alice");
        u.setAge(-10);
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> ValidatorUtils.validate(Locale.CHINA, u));
        assertTrue(ex.getMessage().contains("年龄"));
    }

    @Test
    void validate_traditionalChineseLocale() {
        User u = new User();
        u.setUsername("alice");
        u.setAge(-10);
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> ValidatorUtils.validate(Locale.TAIWAN, u));
        assertTrue(ex.getMessage().contains("年齡"));
    }

    // ========== validateProperty ==========

    @Test
    void validateProperty_validAge() {
        User u = new User();
        u.setUsername("alice");
        u.setAge(30);
        assertDoesNotThrow(() -> ValidatorUtils.validateProperty(u, "age"));
    }

    @Test
    void validateProperty_invalidAge() {
        User u = new User();
        u.setUsername("alice");
        u.setAge(500);
        assertThrows(ConstraintViolationException.class,
                () -> ValidatorUtils.validateProperty(u, "age"));
    }

    @Test
    void validateProperty_nullAge() {
        User u = new User();
        u.setUsername("alice");
        u.setAge(null);
        assertDoesNotThrow(() -> ValidatorUtils.validateProperty(u, "age"));
    }

    // ========== validate / 分组 ==========

    @Test
    void validate_modifyGroup_blankUsername() {
        User u = new User();
        u.setUsername("");
        u.setAge(25);
        assertThrows(ConstraintViolationException.class,
                () -> ValidatorUtils.validate(u, User.Modify.class));
    }

    @Test
    void validate_modifyGroup_validUsername() {
        User u = new User();
        u.setUsername("alice");
        u.setAge(25);
        assertDoesNotThrow(() -> ValidatorUtils.validate(u, User.Modify.class));
    }

    @Test
    void validate_defaultGroup_skipsModifyConstraint() {
        User u = new User();
        u.setUsername("");
        u.setAge(25);
        assertDoesNotThrow(() -> ValidatorUtils.validate(u));
    }
}
