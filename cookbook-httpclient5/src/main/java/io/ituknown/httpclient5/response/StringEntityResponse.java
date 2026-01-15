package io.ituknown.httpclient5.response;

public class StringEntityResponse extends HeaderResponse {
    public StringEntityResponse(String entity) {
        this.entity = entity;
    }

    private final String entity;

    public String getEntity() {
        return entity;
    }
}