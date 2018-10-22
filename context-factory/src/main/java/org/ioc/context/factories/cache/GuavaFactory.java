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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.ioc.annotations.context.Order;
import org.ioc.context.factories.ICacheFactory;
import org.ioc.context.model.cache.GuavaFacade;
import org.ioc.context.model.cache.ICache;
import org.ioc.exceptions.IoCException;
import org.ioc.utils.MethodInvocation;


/**
 * Cache that stores invocation results in a Google Guava {@link com.google.common.cache.Cache}.
 *
 * @author GenCloud
 * @date 09/2018
 */
@Order(999)
public class GuavaFactory implements ICacheFactory {
	private ICache<MethodInvocation, Object> invocationObjectICache;

	@Override
	public void initialize() throws IoCException {
		invocationObjectICache = install("guava-interface-cache");
	}

	@Override
	public <T> T invoke(Class<T> interfaceType, T instance) {
		return invoke(invocationObjectICache, interfaceType, instance);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V> ICache<K, V> install(String name, int size) {
		if (log.isDebugEnabled()) {
			log.debug("Creating cache {} with minimum size of {}", name, size);
		}

		final CacheBuilder builder = CacheBuilder.newBuilder();
		builder.maximumSize(size);

		final LoadingCache<K, V> loadingCache = builder.build(new CacheLoader<K, V>() {
			@Override
			public V load(K key) throws Exception {
				throw new Exception("Key is not bounded to any value");
			}
		});

		return new GuavaFacade<>(loadingCache);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V> ICache<K, V> installEternal(String name, int size) {
		if (log.isDebugEnabled()) {
			log.debug("Creating eternal cache {} with minimum size of {}", name, size);
		}

		final CacheBuilder builder = CacheBuilder.newBuilder();
		builder.maximumSize(Integer.MAX_VALUE);

		final LoadingCache<K, V> loadingCache = builder.build(new CacheLoader<K, V>() {
			@Override
			public V load(K key) throws Exception {
				throw new Exception("Key is not bounded to any value");
			}
		});

		return new GuavaFacade<>(loadingCache);
	}

	@Override
	public <K, V> ICache<K, V> install(String name) {
		return install(name, 1000);
	}

	@Override
	public <K, V> void invalidate(ICache<K, V> ICache) {
		if (ICache instanceof GuavaFacade) {
			if (log.isDebugEnabled()) {
				log.debug("Disposing cache {}", ICache);
			}
			((GuavaFacade<K, V>) ICache).getCache().invalidateAll();
		} else {
			log.warn("Trying to invalidate {} cache when it is not EhCacheFacade collection");
		}
	}

	@Override
	public void destroy() {
		invalidate(invocationObjectICache);
		invocationObjectICache = null;
	}
}
