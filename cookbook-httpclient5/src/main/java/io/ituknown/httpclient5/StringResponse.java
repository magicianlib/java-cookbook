package io.ituknown.httpclient5;

import org.apache.hc.core5.http.Header;

public class StringResponse {
    public StringResponse(String entity) {
        this.entity = entity;
    }

    private final String entity;
    private Header[] headers;

    public String getEntity() {
        return entity;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public void setHeaders(Header[] headers) {
        this.headers = headers;
    }
}