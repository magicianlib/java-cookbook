package io.ituknown.ban.fixtures;

import io.ituknown.ban.fixtures.forbidden.ForbiddenStub;

import java.util.function.Supplier;

public class MethodRefFixture {

    public Supplier<String> supplier() {
        return ForbiddenStub::hello;
    }
}
