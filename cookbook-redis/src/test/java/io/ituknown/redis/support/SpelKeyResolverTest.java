package io.ituknown.redis.support;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpelKeyResolverTest {

    static class Fixture {
        public void login(String userId) {
        }

        public void query(String userId, int page) {
        }
    }

    @Test
    void empty_key_falls_back_to_method_signature() throws Exception {
        Method method = Fixture.class.getMethod("login", String.class);

        assertEquals("Fixture#login", new SpelKeyResolver().resolve(method, new Object[]{"42"}, ""));
    }

    @Test
    void spel_key_appends_evaluated_value() throws Exception {
        Method method = Fixture.class.getMethod("login", String.class);

        assertEquals("Fixture#login#42", new SpelKeyResolver().resolve(method, new Object[]{"42"}, "#userId"));
    }

    @Test
    void spel_key_can_read_later_param() throws Exception {
        Method method = Fixture.class.getMethod("query", String.class, int.class);

        assertEquals("Fixture#query#3", new SpelKeyResolver().resolve(method, new Object[]{"u1", 3}, "#page"));
    }
}
