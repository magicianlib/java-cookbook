package io.ituknown.httpclient5;

import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestOptionsTest {

    @Test
    void defaultValues() {
        RequestOptions config = RequestOptions.DEFAULT;
        assertTrue(config.isFollowRedirects());
        assertEquals(200L, config.getConnectionRequestTimeout());
        assertEquals(3_000L, config.getResponseTimeout());
        assertNull(config.getProxy());
        assertNull(config.getCredentials());
        assertTrue(config.getHeaders().isEmpty());
    }

    @Test
    void setFollowRedirects() {
        RequestOptions config = new RequestOptions();
        config.setFollowRedirects(false);
        assertFalse(config.isFollowRedirects());
    }

    @Test
    void setConnectionRequestTimeout() {
        RequestOptions config = new RequestOptions();
        config.setConnectionRequestTimeout(500L);
        assertEquals(500L, config.getConnectionRequestTimeout());
    }

    @Test
    void setResponseTimeout() {
        RequestOptions config = new RequestOptions();
        config.setResponseTimeout(10_000L);
        assertEquals(10_000L, config.getResponseTimeout());
    }

    @Test
    void rejectNegativeConnectionRequestTimeout() {
        RequestOptions config = new RequestOptions();
        assertThrows(IllegalArgumentException.class,
                () -> config.setConnectionRequestTimeout(-1L));
    }

    @Test
    void rejectNegativeResponseTimeout() {
        RequestOptions config = new RequestOptions();
        assertThrows(IllegalArgumentException.class,
                () -> config.setResponseTimeout(-1L));
    }

    @Test
    void setProxyStringParsesHttpHost() {
        RequestOptions config = new RequestOptions();
        config.setProxy("http://127.0.0.1:8080");
        HttpHost proxy = config.getProxy();
        assertNotNull(proxy);
        assertEquals("127.0.0.1", proxy.getHostName());
        assertEquals(8080, proxy.getPort());
    }

    @Test
    void setProxyBlankClearsProxy() {
        RequestOptions config = new RequestOptions();
        config.setProxy("http://127.0.0.1:8080");
        assertNotNull(config.getProxy());
        config.setProxy("   ");
        assertNull(config.getProxy());
    }

    @Test
    void setProxyInvalidStringFailsEarly() {
        RequestOptions config = new RequestOptions();
        // HttpHost.create 不允许含空格，非法格式在 set 时即抛出（早失败），而非推迟到请求发起
        assertThrows(IllegalArgumentException.class,
                () -> config.setProxy("127.0.0.1 8080"));
    }

    @Test
    void setProxyHttpHost() {
        RequestOptions config = new RequestOptions();
        HttpHost host = new HttpHost("http", "127.0.0.1", 8080);
        config.setProxy(host);
        assertSame(host, config.getProxy());
    }

    @Test
    void addHeader() {
        RequestOptions config = new RequestOptions();
        config.addHeader("Authorization", "Bearer token123");
        assertEquals("Bearer token123", config.getHeaders().get("Authorization"));
    }

    @Test
    void addMultipleHeaders() {
        RequestOptions config = new RequestOptions();
        config.addHeader("Accept", "application/json");
        config.addHeader("Authorization", "Bearer token");

        assertEquals(2, config.getHeaders().size());
        assertEquals("application/json", config.getHeaders().get("Accept"));
        assertEquals("Bearer token", config.getHeaders().get("Authorization"));
    }

    @Test
    void newInstanceIndependentFromDefault() {
        RequestOptions config = new RequestOptions();
        config.setFollowRedirects(false);
        config.addHeader("X-Test", "value");

        assertTrue(RequestOptions.DEFAULT.isFollowRedirects());
        assertTrue(RequestOptions.DEFAULT.getHeaders().isEmpty());
    }

    @Test
    void getHeadersReturnsImmutableView() {
        RequestOptions config = new RequestOptions();
        config.addHeader("A", "1");
        // 即便是普通（可变）实例，getHeaders() 返回的也是不可变视图，无法绕过 addHeader 修改
        assertThrows(UnsupportedOperationException.class,
                () -> config.getHeaders().put("B", "2"));
    }

    @Test
    void defaultIsFrozen() {
        // DEFAULT 冻结后任何修改都必须被拒绝（不再只是 headers 不可变）
        assertThrows(UnsupportedOperationException.class,
                () -> RequestOptions.DEFAULT.setFollowRedirects(false));
        assertThrows(UnsupportedOperationException.class,
                () -> RequestOptions.DEFAULT.setResponseTimeout(1L));
        assertThrows(UnsupportedOperationException.class,
                () -> RequestOptions.DEFAULT.setConnectionRequestTimeout(1L));
        assertThrows(UnsupportedOperationException.class,
                () -> RequestOptions.DEFAULT.setProxy("http://127.0.0.1:8080"));
        assertThrows(UnsupportedOperationException.class,
                () -> RequestOptions.DEFAULT.setProxy(new HttpHost("http", "127.0.0.1", 8080)));
        assertThrows(UnsupportedOperationException.class,
                () -> RequestOptions.DEFAULT.setCredentials(null));
        assertThrows(UnsupportedOperationException.class,
                () -> RequestOptions.DEFAULT.addHeader("X", "y"));
    }

    @Test
    void copyPreservesPopulatedHeaders() {
        RequestOptions src = new RequestOptions();
        src.addHeader("A", "1");
        src.addHeader("B", "2");

        RequestOptions copy = src.copy();

        assertEquals(2, copy.getHeaders().size());
        assertEquals("1", copy.getHeaders().get("A"));
        assertEquals("2", copy.getHeaders().get("B"));
        // independence
        copy.addHeader("C", "3");
        assertEquals(2, src.getHeaders().size());
    }

    @Test
    void copyProducesMutableIndependentInstance() {
        RequestOptions copy = RequestOptions.DEFAULT.copy();
        copy.addHeader("X-Test", "value");
        copy.setFollowRedirects(false);
        copy.setResponseTimeout(9_000L);

        assertTrue(RequestOptions.DEFAULT.getHeaders().isEmpty());
        assertTrue(RequestOptions.DEFAULT.isFollowRedirects());
        assertEquals(3_000L, RequestOptions.DEFAULT.getResponseTimeout());
        assertEquals(1, copy.getHeaders().size());
        assertFalse(copy.isFollowRedirects());
        assertEquals(9_000L, copy.getResponseTimeout());
    }

    @Test
    void copyPreservesProxyAndTimeouts() {
        RequestOptions src = new RequestOptions();
        src.setProxy("http://127.0.0.1:8080");
        src.setResponseTimeout(7_000L);
        src.setConnectionRequestTimeout(300L);

        RequestOptions copy = src.copy();

        assertEquals(src.getProxy(), copy.getProxy());
        assertEquals(7_000L, copy.getResponseTimeout());
        assertEquals(300L, copy.getConnectionRequestTimeout());
    }
}
