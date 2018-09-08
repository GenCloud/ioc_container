package org.di.context.analyze;

/**
 * The main interface for subsequent implementations of analyzers.
 *
 * @param <R> - returned result from analyze
 * @param <T> - object or class for analyze
 * @author GenCloud
 * @date 05.09.2018
 */
public interface Analyzer<R, T> {
    R analyze(T tested) throws Exception;

    /**
     * Support for analysis for the current {@param tested}
     *
     * @param tested - object or class for analyze
     * @return {@code true} ir {@code false}
     */
    boolean supportFor(T tested);
}
