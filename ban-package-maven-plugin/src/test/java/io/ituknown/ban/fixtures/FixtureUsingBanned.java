package io.ituknown.ban.fixtures;

import io.ituknown.ban.fixtures.forbidden.ForbiddenStub;

public class FixtureUsingBanned {

    private ForbiddenStub stub;

    public ForbiddenStub get() {
        return stub;
    }

    public String greet() {
        return ForbiddenStub.hello();
    }
}
