package io.ituknown.ban.fixtures;

import io.ituknown.ban.fixtures.forbidden.ForbiddenAnnotation;

@ForbiddenAnnotation
public class AnnotatedFixture {

    @ForbiddenAnnotation
    private String annotatedField;

    @ForbiddenAnnotation
    public String annotatedMethod() {
        return annotatedField;
    }
}
