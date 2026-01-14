package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.StringResponse;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;

@Contract(threading = ThreadingBehavior.STATELESS)
public class StringHttpClientResponseHandler extends AbstractHttpClientResponseHandler<StringResponse> {
    @Override
    public StringResponse handleResponse(ClassicHttpResponse response) throws IOException {
        StringResponse result = super.handleResponse(response);
        result.setHeaders(response.getHeaders());
        return result;
    }

    @Override
    public StringResponse handleEntity(HttpEntity entity) throws IOException {
        try {
            String result = EntityUtils.toString(entity);
            return new StringResponse(result);
        } catch (final ParseException ex) {
            throw new ClientProtocolException(ex);
        }
    }
}