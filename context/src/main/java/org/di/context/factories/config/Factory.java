package org.di.context.factories.config;

import org.di.context.excepton.IoCException;

/**
 * Default marker of factory member classes for module head factory instantiation.
 *
 * @author GenCloud
 * @date 14.09.2018
 */
public interface Factory {
    String DEFAULT_DATABASE_FACTORY = "org.di.database.factory.DefaultDataBaseFactory";

    String DEFAULT_THREAD_FACTORY = "org.di.threads.factory.DefaultThreadingFactory";
    /**
     * Default function for initialize installed object factory.
     *
     * @throws IoCException if factory throwing
     */
    void initialize() throws IoCException;
}
