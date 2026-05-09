package io.ituknown.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdCardUtilsTest {

    @Test
    public void testVerifyValid() {
        assertTrue(IdCardUtils.verify("11010519491231002X"));
        assertTrue(IdCardUtils.verify("110101199003076288"));
        assertTrue(IdCardUtils.verify("320102199003076283"));
    }

    @Test
    public void testVerifyMale() {
        assertTrue(IdCardUtils.verify("11010119900307125X"));
        assertTrue(IdCardUtils.verify("110101199003070492"));
        assertTrue(IdCardUtils.verify("110101199003077053"));
        assertTrue(IdCardUtils.verify("110101199003075293"));
        assertTrue(IdCardUtils.verify("110101199003077010"));
    }

    @Test
    public void testVerifyFemale() {
        assertTrue(IdCardUtils.verify("110101199003078451"));
        assertTrue(IdCardUtils.verify("110101199003075234"));
        assertTrue(IdCardUtils.verify("110101199003070177"));
        assertTrue(IdCardUtils.verify("11010119900307977X"));
        assertTrue(IdCardUtils.verify("110101199003078339"));
    }

    @Test
    public void testVerifyValidLowerX() {
        assertTrue(IdCardUtils.verify("11010519491231002x"));
    }

    @Test
    public void testVerifyNull() {
        assertFalse(IdCardUtils.verify(null));
    }

    @Test
    public void testVerifyEmpty() {
        assertFalse(IdCardUtils.verify(""));
    }

    @Test
    public void testVerifyWrongLength() {
        assertFalse(IdCardUtils.verify("1101051949123100"));
    }

    @Test
    public void testVerifyInvalidCheckDigit() {
        assertFalse(IdCardUtils.verify("110105194912310021"));
        assertFalse(IdCardUtils.verify("110101199003076281"));
    }
}
