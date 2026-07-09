package io.ituknown.httpclient5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestConfigTest {

    @Test
    void defaultValues() {
        HttpRequestConfig config = HttpRequestConfig.DEFAULT;
        assertTrue(config.isRedirects());
        assertEquals(200L, config.getConnectionRequestTimeout());
        assertEquals(3_000L, config.getResponseTimeout());
        assertNull(config.getProxy());
        assertNull(config.getCredentials());
        assertTrue(config.getHeaders().isEmpty());
    }

    @Test
    void setRedirects() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setRedirects(false);
        assertFalse(config.isRedirects());
    }

    @Test
    void setConnectionRequestTimeout() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setConnectionRequestTimeout(500L);
        assertEquals(500L, config.getConnectionRequestTimeout());
    }

    @Test
    void setResponseTimeout() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setResponseTimeout(10_000L);
        assertEquals(10_000L, config.getResponseTimeout());
    }

    @Test
    void setProxy() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setProxy("127.0.0.1:8080");
        assertEquals("127.0.0.1:8080", config.getProxy());
    }

    @Test
    void addHeader() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.addHeader("Authorization", "Bearer token123");
        assertEquals("Bearer token123", config.getHeaders().get("Authorization"));
    }

    @Test
    void addMultipleHeaders() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.addHeader("Accept", "application/json");
        config.addHeader("Authorization", "Bearer token");

        assertEquals(2, config.getHeaders().size());
        assertEquals("application/json", config.getHeaders().get("Accept"));
        assertEquals("Bearer token", config.getHeaders().get("Authorization"));
    }

    @Test
    void newInstanceIndependentFromDefault() {
        HttpRequestConfig config = new HttpRequestConfig();
        config.setRedirects(false);
        config.addHeader("X-Test", "value");

        assertTrue(HttpRequestConfig.DEFAULT.isRedirects());
        assertTrue(HttpRequestConfig.DEFAULT.getHeaders().isEmpty());
    }

    @Test
    void defaultHeadersImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> HttpRequestConfig.DEFAULT.addHeader("X", "y"));
    }

    @Test
    void copyPreservesPopulatedHeaders() {
        HttpRequestConfig src = new HttpRequestConfig();
        src.addHeader("A", "1");
        src.addHeader("B", "2");

        HttpRequestConfig copy = src.copy();

        assertEquals(2, copy.getHeaders().size());
        assertEquals("1", copy.getHeaders().get("A"));
        assertEquals("2", copy.getHeaders().get("B"));
        // independence
        copy.addHeader("C", "3");
        assertEquals(2, src.getHeaders().size());
    }

    @Test
    void copyProducesMutableIndependentInstance() {
        HttpRequestConfig copy = HttpRequestConfig.DEFAULT.copy();
        copy.addHeader("X-Test", "value");
        copy.setRedirects(false);
        copy.setResponseTimeout(9_000L);

        assertTrue(HttpRequestConfig.DEFAULT.getHeaders().isEmpty());
        assertTrue(HttpRequestConfig.DEFAULT.isRedirects());
        assertEquals(3_000L, HttpRequestConfig.DEFAULT.getResponseTimeout());
        assertEquals(1, copy.getHeaders().size());
        assertFalse(copy.isRedirects());
        assertEquals(9_000L, copy.getResponseTimeout());
    }
}
