package io.ituknown.httpclient5.response;

public class StringEntityResponse {
    public StringEntityResponse(String entity) {
        this.entity = entity;
    }

    private Headers headers;

    private String entity;

    public Headers getHeader() {
        return headers;
    }

    public void setHeader(Headers headers) {
        this.headers = headers;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return "entity: " + entity + ", headers: " + headers;
    }
}