package com.wanderingmotivation.spotify.callwrapper.util;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Exception> {
    R apply(T t, U u) throws E;
}