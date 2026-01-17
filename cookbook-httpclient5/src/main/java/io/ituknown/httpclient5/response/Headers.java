package io.ituknown.httpclient5.response;

import java.util.*;

public class Headers implements Iterable<MinimalField> {
    private final List<MinimalField> fields;
    private final Map<String, List<MinimalField>> fieldMap;

    public Headers() {
        this.fields = new ArrayList<>();
        this.fieldMap = new HashMap<>();
    }

    public void addField(final MinimalField field) {
        if (field == null) {
            return;
        }

        fields.add(field);

        String key = field.name().toLowerCase(Locale.ROOT);
        fieldMap.computeIfAbsent(key, k -> new LinkedList<>()).add(field);
    }

    public List<MinimalField> getFields() {
        return fields;
    }

    public MinimalField getField(final String name) {
        if (name == null) {
            return null;
        }

        String key = name.toLowerCase(Locale.ROOT);
        List<MinimalField> list = fieldMap.get(key);
        if (list != null && !list.isEmpty()) {
            return list.getFirst();
        }
        return null;
    }

    public List<MinimalField> getFields(final String name) {
        if (name == null) {
            return null;
        }

        String key = name.toLowerCase(Locale.ROOT);
        List<MinimalField> list = fieldMap.get(key);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        } else {
            return list;
        }
    }

    @Override
    public Iterator<MinimalField> iterator() {
        return Collections.unmodifiableList(fields).iterator();
    }

    @Override
    public String toString() {
        return fields.toString();
    }
}