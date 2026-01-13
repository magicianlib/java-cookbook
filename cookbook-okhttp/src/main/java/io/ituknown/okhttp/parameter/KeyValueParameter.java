package io.ituknown.okhttp.parameter;

import okhttp3.FormBody;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class KeyValueParameter {

    private final List<Part> parameters = new ArrayList<>();

    public void addParameter(String name, Object value) {
        parameters.add(new Part(name, value));
    }

    protected static class Part {
        private final String name;
        private final Object value;

        public Part(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    @Override
    public String toString() {
        StringJoiner builder = new StringJoiner("&");
        for (Part pair : parameters) {
            builder.add(pair.name + "=" + pair.value);
        }
        return builder.toString();
    }

    public String toUrl(String url) {
        if (parameters.isEmpty()) {
            return url;
        }

        String query = toString();

        if (url.contains("?")) {
            return url + "&" + query;
        }

        return url + "?" + query;
    }

    public FormBody toForm() {
        FormBody.Builder builder = new FormBody.Builder();
        parameters.forEach(pair -> builder.add(pair.name, pair.value.toString()));
        return builder.build();
    }
}