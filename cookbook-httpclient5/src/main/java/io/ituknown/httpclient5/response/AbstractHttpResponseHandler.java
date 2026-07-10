package io.ituknown.httpclient5.response;

import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 响应处理器公共骨架：先做「状态码 >= 400 → 消费实体并抛 HttpResponseException」的统一失败处理，
 * 再把成功（2xx/3xx）响应交给 {@link #handleSuccessful}。子类只关心成功路径，失败语义集中在此处。
 */
abstract class AbstractHttpResponseHandler<T> implements HttpClientResponseHandler<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpResponseHandler.class);

    @Override
    public final T handleResponse(ClassicHttpResponse response) throws IOException {
        int statusCode = response.getCode();
        if (statusCode >= 400) {
            EntityUtils.consume(response.getEntity());
            LOGGER.warn("HTTP Failed [{}], Reason: {}", statusCode, response.getReasonPhrase());
            throw new HttpResponseException(statusCode, response.getReasonPhrase());
        }
        return handleSuccessful(response, statusCode);
    }

    /**
     * 处理成功响应（已保证状态码 < 400）。
     */
    protected abstract T handleSuccessful(ClassicHttpResponse response, int statusCode) throws IOException;
}
