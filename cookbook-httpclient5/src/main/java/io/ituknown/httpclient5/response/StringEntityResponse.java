package io.ituknown.httpclient5.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StringEntityResponse {
    public StringEntityResponse(String entity) {
        this.entity = entity;
    }

    private Headers headers;

    @Setter(AccessLevel.NONE)
    private String entity;

    @Override
    public String toString() {
        return "entity: " + entity + ", headers: " + headers;
    }
}