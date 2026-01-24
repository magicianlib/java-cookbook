package io.ituknown.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmailMaskUtilsTest {
    @Test
    public void testMaskEmail() {
        {
            String masked = EmailMaskUtils.maskEmail("zhangsan123");
            assertEquals("z**********", masked);
        }
        {
            String masked = EmailMaskUtils.maskEmail("zhangsan123@gmail.com");
            assertEquals("z****@gmail.com", masked);
        }
        {
            String masked = EmailMaskUtils.maskEmail("ab@test.com");
            assertEquals("a****@test.com", masked);
        }
        {
            String masked = EmailMaskUtils.maskEmail("a@test.com");
            assertEquals("a****@test.com", masked);
        }
    }
}