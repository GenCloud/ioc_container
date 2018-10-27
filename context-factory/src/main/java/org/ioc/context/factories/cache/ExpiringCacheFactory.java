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

import org.ioc.annotations.context.Order;
import org.ioc.context.factories.ICacheFactory;
import org.ioc.context.model.cache.ExpiringFacade;
import org.ioc.context.model.cache.ICache;
import org.ioc.context.model.cache.expiring.ExpiringBuilder;
import org.ioc.exceptions.IoCException;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Order(999)
public class ExpiringCacheFactory implements ICacheFactory {
	/**
	 * Default function for initialize installed object factories.
	 *
	 * @throws IoCException if factories throwing
	 */
	@Override
	public void initialize() throws IoCException {

	}

	@Override
	public <T> T invoke(Class<T> interfaceType, T instance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <K, V> ICache<K, V> install(String name, int size) {
		if (log.isDebugEnabled()) {
			log.debug("Creating cache {} with minimum size of {}", name, size);
		}

		final ExpiringBuilder<K, V> builder = new ExpiringBuilder<>();
		builder.setExpireAfterAccessNanos(1);
		builder.setExpireAfterWriteNanos(1);
		builder.setMaximumWeight(size);
		return builder.build();
	}

	@Override
	public <K, V> ICache<K, V> installEternal(String name, int size) {
		if (log.isDebugEnabled()) {
			log.debug("Creating eternal cache {} with minimum size of {}", name, size);
		}

		final ExpiringBuilder<K, V> builder = new ExpiringBuilder<>();
		builder.setMaximumWeight(size);
		return builder.build();
	}

	@Override
	public <K, V> ICache<K, V> install(String name) {
		return install(name, 1000);
	}

	@Override
	public <K, V> void invalidate(ICache<K, V> cache) {
		if (cache instanceof ExpiringFacade) {
			if (log.isDebugEnabled()) {
				log.debug("Disposing cache {}", cache);
			}

			final ExpiringFacade<K, V> facade = (ExpiringFacade<K, V>) cache;
			facade.clear();
		} else {
			log.warn("Trying to invalidate {} cache when it is not ExpiringFacade collection");
		}
	}

	@Override
	public void destroy() {

	}
}
