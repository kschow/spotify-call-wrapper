package com.wanderingmotivation.spotify.callwrapper.util;

import java.util.function.Function;

/**
 * Declares static wrappers for throwing function interfaces to be used externally
 * Class is explicitly final so no one subclasses it
 */
public final class ThrowingFunctionWrappers {
    public static <T, R> Function<T, R> throwingFunctionWrapper(final ThrowingFunction<T, R, Exception> throwingFunction) {
        return i -> {
            try {
                return throwingFunction.apply(i);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
