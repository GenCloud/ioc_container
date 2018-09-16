/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of DI (IoC) Container Project.
 *
 * DI (IoC) Container Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DI (IoC) Container Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DI (IoC) Container Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.di.test.cache;

import org.di.cache.ICache;
import org.di.cache.impl.EhFactory;
import org.di.context.annotations.IoCComponent;
import org.di.context.contexts.sensibles.CacheFactorySensible;
import org.di.context.excepton.IoCException;
import org.di.context.factories.config.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * @author GenCloud
 * @date 16.09.2018
 */
@IoCComponent
public class CacheComponentTest implements CacheFactorySensible {
    private static final Logger log = LoggerFactory.getLogger(CacheComponentTest.class);

    private EhFactory factory;

    private ICache<String, String> sampleCache;

    @PostConstruct
    public void initializeCache() {
        sampleCache = factory.installEternal("sample-test-cache", 200);

        log.info("Creating sample cache - [{}]", sampleCache);

        sampleCache.put("1", "First");
        sampleCache.put("2", "Second");
        sampleCache.put("3", "Third");
        sampleCache.put("4", "Fourth");

        log.info("Loaded size - [{}]", sampleCache.size());
    }

    public String getElement(String key) {
        final String value = sampleCache.get(key);
        log.info("Getting value from cache - [{}]", value);
        return value;
    }

    public void removeElement(String key) {
        log.info("Remove object from cache");
        sampleCache.remove(key);
    }

    public void invalidate() {
        sampleCache.clear();
        log.info("Clear all cache, size - [{}]", sampleCache.size());
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
