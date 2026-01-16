package io.ituknown.httpclient5.response;

public record MinimalField(String name, String value) {
    @Override
    public String toString() {
        return this.name + ": " + this.value;
    }
}