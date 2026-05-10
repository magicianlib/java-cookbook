package io.ituknown.jackson.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ituknown.jackson.JacksonUtils;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveSerializerTest {

    @Setter
    @Getter
    static class User {
        private String name;

        @Sensitive(SensitiveType.MOBILE)
        private String mobile;

        @Sensitive(SensitiveType.EMAIL)
        private String email;

        @Sensitive(SensitiveType.ID_CARD)
        private String idCard;

        public User() {}

        User(String name, String mobile, String email, String idCard) {
            this.name = name;
            this.mobile = mobile;
            this.email = email;
            this.idCard = idCard;
        }

    }

    @Test
    void sensitiveFieldsAreMasked() throws JsonProcessingException {
        User user = new User("张三", "13812345678", "test@example.com", "110101199001011234");
        String json = JacksonUtils.toJson(user);

        assertTrue(json.contains("张三"));
        assertTrue(json.contains("138****5678"));
        assertTrue(json.contains("t****@example.com"));
        assertTrue(json.contains("110101****1234"));

        assertFalse(json.contains("13812345678"));
        assertFalse(json.contains("test@example.com"));
        assertFalse(json.contains("110101199001011234"));
    }

    @Test
    void nonSensitiveFieldsArePreserved() throws JsonProcessingException {
        User user = new User("张三", "13812345678", "test@example.com", "110101199001011234");
        String json = JacksonUtils.toJson(user);
        assertTrue(json.contains("\"name\":\"张三\""));
    }

    @Test
    void sensitiveFieldWithNullValue() {
        User user = new User();
        user.setName("张三");
        String json = JacksonUtils.toJson(user);
        assertNotNull(json);
    }
}