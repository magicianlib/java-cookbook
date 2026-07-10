package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.HeaderHelper;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 以流的方式消费响应体。
 * <p>
 * 注意：consumer 若未读到 EOF，底层连接按 HC5 语义会被判定不可复用而关闭，连接池将重新建连——
 * 这是 streaming 消费的固有取舍，不属于缺陷。
 */
public class StreamResponseHandler extends AbstractHttpResponseHandler<Headers> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamResponseHandler.class);

    private final Consumer<InputStream> streamConsumer;

    public StreamResponseHandler(Consumer<InputStream> streamConsumer) {
        this.streamConsumer = streamConsumer;
    }

    @Override
    protected Headers handleSuccessful(ClassicHttpResponse response, int statusCode) throws IOException {
        Headers headers = HeaderHelper.resolveHeader(response);
        LOGGER.info("http response content: [STREAM], headers: {}", headers);

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            return headers;
        }

        try (InputStream in = entity.getContent()) {
            streamConsumer.accept(in);
        }

        return headers;
    }
}
