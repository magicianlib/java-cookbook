package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.Helper;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class StreamHttpClientResponseHandler extends AbstractHttpClientResponseHandler<Headers> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamHttpClientResponseHandler.class);

    private final Consumer<InputStream> streamConsumer;

    public StreamHttpClientResponseHandler(Consumer<InputStream> streamConsumer) {
        this.streamConsumer = streamConsumer;
    }

    @Override
    public Headers handleResponse(ClassicHttpResponse response) throws IOException {
        super.handleResponse(response);
        Headers headers = Helper.resolveHeader(response);
        LOGGER.info("http response content: [STREAM], headers: {}", headers);
        return headers;
    }

    @Override
    public Headers handleEntity(HttpEntity entity) throws IOException {
        try (InputStream in = entity.getContent()) {
            streamConsumer.accept(in);
        }
        return null;
    }
}