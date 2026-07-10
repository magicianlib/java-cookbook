package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.HeaderHelper;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StringResponseHandler extends AbstractHttpResponseHandler<StringEntityResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringResponseHandler.class);

    @Override
    protected StringEntityResponse handleSuccessful(ClassicHttpResponse response, int statusCode) throws IOException {
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

    private StringEntityResponse handleEntity(HttpEntity entity) throws IOException {
        try {
            String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            return new StringEntityResponse(result);
        } catch (final ParseException ex) {
            throw new ClientProtocolException(ex);
        }
    }
}
