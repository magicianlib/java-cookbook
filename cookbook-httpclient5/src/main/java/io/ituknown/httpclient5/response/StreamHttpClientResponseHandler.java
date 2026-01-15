package io.ituknown.httpclient5.response;

import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class StreamHttpClientResponseHandler extends AbstractHttpClientResponseHandler<HeaderResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamHttpClientResponseHandler.class);

    private final Consumer<InputStream> streamConsumer;

    public StreamHttpClientResponseHandler(Consumer<InputStream> streamConsumer) {
        this.streamConsumer = streamConsumer;
    }

    @Override
    public HeaderResponse handleResponse(ClassicHttpResponse response) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Header header : response.getHeaders()) {
            builder.append(" ").append(header.getName()).append(": ").append(header.getValue());
        }
        LOGGER.info("http response content: [STREAM], header: [{}]", builder.substring(1));

        super.handleResponse(response);
        HeaderResponse result = new HeaderResponse();
        result.setHeaders(response.getHeaders());
        return result;
    }

    @Override
    public HeaderResponse handleEntity(HttpEntity entity) throws IOException {
        try (InputStream in = entity.getContent()) {
            streamConsumer.accept(in);
        }
        return null;
    }
}
