package com.wanderingmotivation.spotify.callwrapper.util;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}