/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of IoC Starter Project.
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
package org.ioc.test.cache;

import org.ioc.annotations.context.IoCComponent;
import org.ioc.context.factories.Factory;
import org.ioc.context.factories.ICache;
import org.ioc.context.factories.cache.EhFactory;
import org.ioc.context.sensible.factories.CacheFactorySensible;
import org.ioc.exceptions.IoCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 09/2018
 */
@IoCComponent
public class CacheComponentTest implements CacheFactorySensible {
	private static final Logger log = LoggerFactory.getLogger(CacheComponentTest.class);

	private EhFactory factory;

	private ICache<String, String> sampleCache;

	public void initializeCache() {
		sampleCache = factory.installEternal("sample-test-getEntityCache", 200);

		log.info("Creating sample getEntityCache - [{}]", sampleCache);

		sampleCache.put("1", "First");
		sampleCache.put("2", "Second");
		sampleCache.put("3", "Third");
		sampleCache.put("4", "Fourth");

		log.info("Loaded size - [{}]", sampleCache.size());
	}

	public String getElement(String key) {
		final String value = sampleCache.get(key);
		log.info("Getting value from getEntityCache - [{}]", value);
		return value;
	}

	public void removeElement(String key) {
		log.info("Remove object from getEntityCache");
		sampleCache.remove(key);
	}

	public void invalidate() {
		sampleCache.clear();
		log.info("Clear all getEntityCache, size - [{}]", sampleCache.size());
	}

	@Override
	public void factoryInform(Factory factory) throws IoCException {
		this.factory = (EhFactory) factory;
	}

	@Override
	public String toString() {
		return "CacheComponentTest{" +
				"factory=" + factory +
				", sampleCache=" + sampleCache +
				'}';
	}
}
