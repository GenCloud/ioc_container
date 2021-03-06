/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
 *
 * IoC Starter Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IoC Starter Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IoC Starter Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.context.factories.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.ioc.annotations.context.Order;
import org.ioc.context.factories.ICacheFactory;
import org.ioc.context.model.cache.EhFacade;
import org.ioc.context.model.cache.ICache;
import org.ioc.exceptions.IoCException;
import org.ioc.utils.MethodInvocation;


/**
 * Cache that stores invocation results in a EhCache {@link net.sf.ehcache.Cache}.
 *
 * @author GenCloud
 * @date 09/2018
 */
@Order(999)
public class EhFactory implements ICacheFactory {
	private CacheManager manager;

	private ICache<MethodInvocation, Object> invocationObjectICache;

	@Override
	public void initialize() throws IoCException {
		final Configuration configuration = new Configuration();
		configuration.updateCheck(false);
		configuration.diskStore(new DiskStoreConfiguration().path("./cache"));
		manager = new CacheManager(configuration);
		invocationObjectICache = install("eh-interface-cache");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T invoke(Class<T> interfaceType, T instance) {
		return invoke(invocationObjectICache, interfaceType, instance);
	}

	@Override
	public <K, V> ICache<K, V> install(String name, int size) {
		if (log.isDebugEnabled()) {
			log.debug("Creating cache {} with minimum size of {}", name, size);
		}

		final CacheConfiguration configuration = new CacheConfiguration(name, size);
		configuration.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);
		configuration.eternal(true);
		configuration.timeToLiveSeconds(60);
		configuration.timeToIdleSeconds(30);
		configuration.diskExpiryThreadIntervalSeconds(0);

		final Cache cache = new net.sf.ehcache.Cache(configuration);
		manager.addCache(cache);
		return new EhFacade<>(cache);
	}

	@Override
	public <K, V> ICache<K, V> installEternal(String name, int size) {
		if (log.isDebugEnabled()) {
			log.debug("Creating eternal cache {} with minimum size of {}", name, size);
		}

		final CacheConfiguration configuration = new CacheConfiguration(name, size);
		configuration.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);
		configuration.eternal(true);
		configuration.timeToIdleSeconds(0);
		configuration.timeToLiveSeconds(0);
		configuration.diskExpiryThreadIntervalSeconds(0);

		Cache cache = new net.sf.ehcache.Cache(configuration);
		manager.addCache(cache);
		return new EhFacade<>(cache);
	}

	@Override
	public <K, V> ICache<K, V> install(String name) {
		return install(name, 1000);
	}

	@Override
	public <K, V> void invalidate(ICache<K, V> cache) {
		if (manager.getStatus() == Status.STATUS_SHUTDOWN) {
			return;
		}

		if (cache instanceof EhFacade) {
			if (log.isDebugEnabled()) {
				log.debug("Disposing cache {}", cache);
			}

			manager.removeCache(((EhFacade<K, V>) cache).getCache().getName());
		} else {
			log.warn("Trying to invalidate {} cache when it is not EhCacheFacade collection");
		}
	}

	@Override
	public void destroy() {
		manager.removalAll();
		manager.shutdown();
		invocationObjectICache = null;
	}
}
