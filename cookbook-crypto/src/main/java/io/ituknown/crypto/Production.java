package io.ituknown.crypto;

import java.util.function.Function;

@FunctionalInterface
public interface Production<R> extends Function<byte[], R> {
}