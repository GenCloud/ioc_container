package org.di.context.factories.config;

import org.di.context.excepton.IoCException;

/**
 * Default marker of factories member classes for module head factories instantiation.
 *
 * @author GenCloud
 * @date 14.09.2018
 */
public interface Factory {
    String DEFAULT_DATABASE_FACTORY = "org.di.database.factories.OrientDataBaseFactory";

    String DEFAULT_THREAD_FACTORY = "org.di.threads.factory.DefaultThreadingFactory";

    String DEFAULT_CACHE_FACTORY = "org.di.cache.impl.EhFactory";

    @SuppressWarnings("unchecked")
    static Class<Factory> defaultDatabaseFactory() {
        try {
            return (Class<Factory>) Class.forName(DEFAULT_DATABASE_FACTORY);
        } catch (ClassNotFoundException e) {
            throw new IoCException("IoCError - can't instantiate default Database Factory", e);
        }
    }

    @SuppressWarnings("unchecked")
    static Class<Factory> defaultThreadFactory() {
        try {
            return (Class<Factory>) Class.forName(DEFAULT_THREAD_FACTORY);
        } catch (ClassNotFoundException e) {
            throw new IoCException("IoCError - Unavailable create instance of type [org.di.threads.factories.DefaultThreadingFactory]." +
                    "Could not find thread factories class in context. Maybe unresolvable module?", e);
        }
    }

    @SuppressWarnings("unchecked")
    static Class<Factory> defaultCacheFactory() {
        try {
            return (Class<Factory>) Class.forName(DEFAULT_CACHE_FACTORY);
        } catch (ClassNotFoundException e) {
            throw new IoCException("IoCError - can't instantiate default Cache Factory", e);
        }
    }

    /**
     * Default function for initialize installed object factories.
     *
     * @throws IoCException if factories throwing
     */
    void initialize() throws IoCException;
}
