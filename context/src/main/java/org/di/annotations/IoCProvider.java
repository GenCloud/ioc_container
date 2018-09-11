package org.di.annotations;

/**
 * @author GenCloud
 * @date 10.09.2018
 * @see javax.inject.Provider
 */
public interface IoCProvider<O> {
    O getInstance();
}
