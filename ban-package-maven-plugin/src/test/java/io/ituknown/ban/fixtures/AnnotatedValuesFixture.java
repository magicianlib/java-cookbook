package io.ituknown.ban.fixtures;

import io.ituknown.ban.fixtures.forbidden.ForbiddenAnnotation;
import io.ituknown.ban.fixtures.forbidden.ForbiddenStub;

@ForbiddenAnnotation(classes = ForbiddenStub.class)
public class AnnotatedValuesFixture {
}
