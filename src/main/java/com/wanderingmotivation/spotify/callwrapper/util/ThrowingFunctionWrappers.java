package com.wanderingmotivation.spotify.callwrapper.util;

import org.apache.log4j.Logger;

import java.util.function.BiFunction;
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

    public static <T, U, R> BiFunction<T, U, R> throwingBiFunctionWrapper(final ThrowingBiFunction<T, U, R, Exception> throwingBiFunction) {
        return (i, j) -> {
            try {
                return throwingBiFunction.apply(i, j);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
