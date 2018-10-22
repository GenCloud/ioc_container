package org.ioc.context.factories;

import org.ioc.exceptions.IoCException;

/**
 * Default marker of factories member classes for module head factories instantiation.
 *
 * @author GenCloud
 * @date 09/2018
 */
public interface Factory {
	String DEFAULT_THREAD_FACTORY = "org.ioc.context.factories.threading.DefaultThreadPoolFactory";

	String DEFAULT_CACHE_FACTORY = "org.ioc.context.factories.cache.EhFactory";

	String DEFAULT_DATABASE_FACTORY = "org.ioc.orm.factory.orient.OrientSchemaFactory";

	String DEFAULT_WEB_FACTORY = "org.ioc.web.factory.HttpInitializerFactory";

	@SuppressWarnings("unchecked")
	static Class<Factory> defaultThreadFactory() {
		try {
			return (Class<Factory>) Class.forName(DEFAULT_THREAD_FACTORY);
		} catch (ClassNotFoundException e) {
			throw new IoCException("IoCError - can't instantiate default Thread Pool Factory", e);
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

	@SuppressWarnings("unchecked")
	static Class<Factory> defaultDatabaseFactory() {
		try {
			return (Class<Factory>) Class.forName(DEFAULT_DATABASE_FACTORY);
		} catch (ClassNotFoundException e) {
			throw new IoCException("IoCError - can't instantiate default Database Factory", e);
		}
	}

	@SuppressWarnings("unchecked")
	static Class<Factory> defaultWebFactory() {
		try {
			return (Class<Factory>) Class.forName(DEFAULT_WEB_FACTORY);
		} catch (ClassNotFoundException e) {
			throw new IoCException("IoCError - can't instantiate default Web Factory", e);
		}
	}

	/**
	 * Default function for initialize installed object factories.
	 *
	 * @throws IoCException if factories throwing
	 */
	void initialize() throws IoCException;
}
