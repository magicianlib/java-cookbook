package io.ituknown.httpclient5;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SyncClientTest {

    /** 记录实际读取字节数的 InputStream。 */
    static final class CountingInputStream extends FilterInputStream {
        final AtomicLong counter;

        CountingInputStream(InputStream in, AtomicLong counter) {
            super(in);
            this.counter = counter;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) counter.incrementAndGet();
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) counter.addAndGet(n);
            return n;
        }
    }

    /**
     * 测试用 entity：可配置 repeatable，每次 getContent() 返回新的计数流。
     * 注意 HC5 AbstractHttpEntity 无无参构造器，必须显式 super(ct, null)。
     */
    static final class TestEntity extends AbstractHttpEntity {
        private final byte[] data;
        private final AtomicLong counter;
        private final boolean repeatable;

        TestEntity(String text, ContentType ct, AtomicLong counter, boolean repeatable) {
            super(ct, null);
            this.data = text.getBytes(StandardCharsets.UTF_8);
            this.counter = counter;
            this.repeatable = repeatable;
        }

        @Override
        public boolean isRepeatable() {
            return repeatable;
        }

        @Override
        public long getContentLength() {
            return data.length;
        }

        @Override
        public InputStream getContent() {
            return new CountingInputStream(new ByteArrayInputStream(data), counter);
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        public void close() {
        }
    }

    @Test
    void payloadLogBoundedForLargeTextEntity() {
        String big = "x".repeat(10_000);
        AtomicLong counter = new AtomicLong();
        HttpEntity entity = new TestEntity(big, ContentType.TEXT_PLAIN, counter, true);

        String payload = SyncClient.resolvePayloadForLog(entity);

        int contentPart = payload.contains("... [truncated")
                ? payload.indexOf("... [truncated")
                : payload.length();
        assertTrue(contentPart <= 1000, "内容部分应 <= 1000, 实际 " + contentPart);
        assertTrue(payload.contains("[truncated"));
        assertTrue(counter.get() <= 1000, "底层读取应 <= 1000, 实际 " + counter.get());
    }

    @Test
    void payloadLogSkipsBinaryContentType() {
        String big = "x".repeat(10_000);
        AtomicLong counter = new AtomicLong();
        HttpEntity entity = new TestEntity(big, ContentType.APPLICATION_OCTET_STREAM, counter, true);

        String payload = SyncClient.resolvePayloadForLog(entity);

        assertEquals("Binary/Large Content", payload);
        assertEquals(0, counter.get(), "二进制类型不应触发读取");
    }

    @Test
    void payloadLogSkipsNonRepeatableEntity() {
        AtomicLong counter = new AtomicLong();
        HttpEntity entity = new TestEntity("x".repeat(10_000), ContentType.TEXT_PLAIN, counter, false);
        assertEquals("Binary/Large Content", SyncClient.resolvePayloadForLog(entity));
        assertEquals(0, counter.get(), "非 repeatable 不应触发读取");
    }

    @Test
    void payloadLogNullEntity() {
        assertEquals("Binary/Large Content", SyncClient.resolvePayloadForLog(null));
    }

    @Test
    void payloadLogSmallTextEntityFull() {
        HttpEntity entity = new StringEntity("hello", ContentType.TEXT_PLAIN);
        assertEquals("hello", SyncClient.resolvePayloadForLog(entity));
    }
}
