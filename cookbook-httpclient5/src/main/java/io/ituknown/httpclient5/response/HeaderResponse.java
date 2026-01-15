package io.ituknown.httpclient5.response;

import org.apache.hc.core5.http.Header;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HeaderResponse {
    private static final Header[] EMPTY = new Header[]{};

    private Header[] headers;

    public HeaderResponse() {
    }

    public HeaderResponse(Header[] headers) {
        this.headers = headers;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public Header[] getHeadersUseEmptyIsAbsent() {
        return Optional.ofNullable(headers).orElse(EMPTY);
    }

    public void setHeaders(Header[] headers) {
        this.headers = headers;
    }

    public Header getHeader(final String name) {
        if (headers != null) {
            for (final Header header : headers) {
                if (header.getName().equalsIgnoreCase(name)) {
                    return header;
                }
            }
        }
        return null;
    }

    public Header[] getHeaders(final String name) {
        List<Header> headersFound = null;
        if (headers != null) {
            for (final Header header : headers) {
                if (header.getName().equalsIgnoreCase(name)) {
                    if (headersFound == null) {
                        headersFound = new ArrayList<>();
                    }
                    headersFound.add(header);
                }
            }
        }
        return headersFound != null ? headersFound.toArray(EMPTY) : EMPTY;
    }
}