package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.HeaderHelper;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Contract(threading = ThreadingBehavior.STATELESS)
public class StringResponseHandler extends AbstractHttpClientResponseHandler<StringEntityResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringResponseHandler.class);

    @Override
    public StringEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        int statusCode = response.getCode();

        if (statusCode >= 400) {
            EntityUtils.consume(response.getEntity());
            LOGGER.warn("HTTP Failed [{}], Reason: {}", statusCode, response.getReasonPhrase());
            throw new HttpResponseException(statusCode, response.getReasonPhrase());
        }

        HttpEntity entity = response.getEntity();
        StringEntityResponse result = (entity == null)
                ? new StringEntityResponse(null)
                : handleEntity(entity);
        result.setHeaders(HeaderHelper.resolveHeader(response));

        if (LOGGER.isInfoEnabled()) {
            String body = result.getEntity();
            String logContent = (body != null && body.length() > 1000)
                    ? body.substring(0, 1000) + "... [truncated, total: " + body.length() + "]"
                    : body;
            LOGGER.info("HTTP Success [{}], Content: {}", statusCode, logContent);
        }

        return result;
    }

    @Override
    public StringEntityResponse handleEntity(HttpEntity entity) throws IOException {
        try {
            String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            return new StringEntityResponse(result);
        } catch (final ParseException ex) {
            throw new ClientProtocolException(ex);
        }
    }
}
