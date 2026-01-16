package io.ituknown.httpclient5.response;

public class MinimalField {

    private final String name;
    private final String value;

    public MinimalField(final String name, final String value) {
        super();
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getBody() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.name + ": " + this.value;
    }
}