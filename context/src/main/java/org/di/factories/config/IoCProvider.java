package org.di.factories.config;

/**
 * @author GenCloud
 * @date 10.09.2018
 * @see javax.inject.Provider
 */
public interface IoCProvider<O> {
    O getInstance();
}
