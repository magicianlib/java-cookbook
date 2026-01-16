package io.ituknown.httpclient5.response;

public class StringEntityResponse {
    public StringEntityResponse(String entity) {
        this.entity = entity;
    }

    private Header header;

    private String entity;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return "entity: " + entity + ", header: " + header;
    }
}