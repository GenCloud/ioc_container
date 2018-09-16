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
package org.di.cache.impl;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.di.cache.ICache;
import org.di.cache.ICacheFactory;
import org.di.cache.facade.EhFacade;
import org.di.context.excepton.IoCException;
import org.di.context.excepton.starter.IoCStopException;
import org.di.context.utils.MethodInvocation;

/**
 * Cache that stores invocation results in a EhCache {@link net.sf.ehcache.Cache}.
 *
 * @author GenCloud
 * @date 16.09.2018
 */
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
        configuration.overflowToDisk(true);
        configuration.eternal(true);
        configuration.timeToLiveSeconds(60);
        configuration.timeToIdleSeconds(30);
        configuration.diskPersistent(false);
        configuration.diskExpiryThreadIntervalSeconds(0);

        final net.sf.ehcache.Cache cache = new net.sf.ehcache.Cache(configuration);
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
        configuration.overflowToDisk(true);
        configuration.eternal(true);
        configuration.diskExpiryThreadIntervalSeconds(0);

        net.sf.ehcache.Cache cache = new net.sf.ehcache.Cache(configuration);
        manager.addCache(cache);
        return new EhFacade<>(cache);
    }

    @Override
    public <K, V> ICache<K, V> install(String name) {
        return install(name, 1000);
    }

    @Override
    public <K, V> void invalidate(ICache<K, V> ICache) {
        if (ICache instanceof EhFacade) {
            if (log.isDebugEnabled()) {
                log.debug("Disposing cache {}", ICache);
            }

            manager.removeCache(((EhFacade<K, V>) ICache).getCache().getName());
        } else {
            log.warn("Trying to invalidate {} cache when it is not EhCacheFacade type");
        }
    }

    @Override
    public void destroy() throws IoCStopException {
        manager.removalAll();
        manager.shutdown();
        invocationObjectICache = null;
    }
}
