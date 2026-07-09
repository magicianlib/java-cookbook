package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.HeaderHelper;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class StreamResponseHandler implements HttpClientResponseHandler<Headers> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamResponseHandler.class);

    private final Consumer<InputStream> streamConsumer;

    public StreamResponseHandler(Consumer<InputStream> streamConsumer) {
        this.streamConsumer = streamConsumer;
    }

    @Override
    public Headers handleResponse(ClassicHttpResponse response) throws IOException {
        if (response.getCode() >= 400) {
            EntityUtils.consume(response.getEntity());
            throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
        }

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
