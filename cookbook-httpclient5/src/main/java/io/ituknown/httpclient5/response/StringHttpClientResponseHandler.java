package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.Helper;
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
public class StringHttpClientResponseHandler extends AbstractHttpClientResponseHandler<StringEntityResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringHttpClientResponseHandler.class);

    @Override
    public StringEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        int statusCode = response.getCode();

        try {
            StringEntityResponse result = super.handleResponse(response);
            result.setHeader(Helper.resolveHeader(response));

            if (LOGGER.isInfoEnabled()) {
                String entity = result.getEntity();
                String logContent = (entity != null && entity.length() > 1000)
                        ? entity.substring(0, 1000) + "... [truncated, total: " + entity.length() + "]"
                        : entity;

                LOGGER.info("HTTP Success [{}], Content: {}", statusCode, logContent);
            }

            return result;
        } catch (HttpResponseException e) {
            LOGGER.warn("HTTP Failed [{}], Reason: {}", statusCode, e.getMessage());
            throw e;
        }
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