///*
// * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
// *
// * This file is part of DI (IoC) Container Project.
// *
// * DI (IoC) Container Project is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * DI (IoC) Container Project is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with DI (IoC) Container Project.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.di.cache.impl;
//
//import org.di.cache.ICache;
//import org.di.cache.ICacheFactory;
//import org.di.cache.facade.InfinispanFacade;
//import org.di.context.excepton.IoCException;
//import org.di.context.excepton.starter.IoCStopException;
//import org.di.context.utils.MethodInvocation;
//import org.infinispan.configuration.cache.CacheMode;
//import org.infinispan.configuration.cache.Configuration;
//import org.infinispan.configuration.cache.ConfigurationBuilder;
//import org.infinispan.configuration.global.GlobalConfigurationBuilder;
//import org.infinispan.manager.DefaultCacheManager;
//import org.infinispan.manager.EmbeddedCacheManager;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * Cache that stores invocation results in a Infinispan {@link org.infinispan.Cache}.
// *
// * @author GenCloud
// * @date 16.09.2018
// */
//public class InfinispanFactory implements ICacheFactory {
//    private EmbeddedCacheManager manager;
//
//    private ICache<MethodInvocation, Object> invocationObjectICache;
//
//    @Override
//    public void initialize() throws IoCException {
//        manager = new DefaultCacheManager();
//        invocationObjectICache = install("infinispan-interface-cache");
//    }
//
//    @Override
//    public <T> T invoke(Class<T> interfaceType, T instance) {
//        return invoke(invocationObjectICache, interfaceType, instance);
//    }
//
//    @Override
//    public <K, V> ICache<K, V> install(String name, int size) {
//        if (log.isDebugEnabled()) {
//            log.debug("Creating cache {} with minimum size of {}", name, size);
//        }
//
//        final ConfigurationBuilder builder = new ConfigurationBuilder();
//        builder.jmxStatistics().enabled(true);
//        builder.clustering().cacheMode(CacheMode.REPL_SYNC).remoteTimeout(20000, TimeUnit.MILLISECONDS);
//
//        final GlobalConfigurationBuilder globalBuilder = new GlobalConfigurationBuilder();
//        globalBuilder.globalJmxStatistics().enabled(true).jmxDomain("infinispan").allowDuplicateDomains(true);
//        globalBuilder.transport().clusterName("local-infinispan-cluster").initialClusterSize(size);
//
//        final Configuration configuration = builder.build(globalBuilder.build());
//
//        final org.infinispan.Cache<K, V> cache = manager.administration().createCache(name, configuration);
//        return new InfinispanFacade<>(cache);
//    }
//
//    @Override
//    public <K, V> ICache<K, V> installEternal(String name, int size) {
//        if (log.isDebugEnabled()) {
//            log.debug("Creating eternal cache {} with minimum size of {}", name, Integer.MAX_VALUE);
//        }
//
//        final ConfigurationBuilder builder = new ConfigurationBuilder();
//        builder.jmxStatistics().enabled(true);
//        builder.clustering().cacheMode(CacheMode.REPL_SYNC).remoteTimeout(20000, TimeUnit.MILLISECONDS);
//
//        final GlobalConfigurationBuilder globalBuilder = new GlobalConfigurationBuilder();
//        globalBuilder.globalJmxStatistics().enabled(true).jmxDomain("infinispan").allowDuplicateDomains(true);
//        globalBuilder.transport().clusterName("local-infinispan-cluster").initialClusterSize(Integer.MAX_VALUE);
//
//        final Configuration configuration = builder.build(globalBuilder.build());
//
//        final org.infinispan.Cache<K, V> cache = manager.administration().createCache(name, configuration);
//        return new InfinispanFacade<>(cache);
//    }
//
//    @Override
//    public <K, V> ICache<K, V> install(String name) {
//        return install(name, 1000);
//    }
//
//    @Override
//    public <K, V> void invalidate(ICache<K, V> ICache) {
//        if (ICache instanceof InfinispanFacade) {
//            log.debug("Disposing cache {}", ICache);
//            manager.administration().removeCache(((InfinispanFacade<K, V>) ICache).getCache().getName());
//        } else {
//            log.warn("Trying to dispose {} cache when it is not EhCacheFacade type");
//        }
//    }
//
//    @Override
//    public void destroy() throws IoCStopException {
//        invalidate(invocationObjectICache);
//        invocationObjectICache = null;
//    }
//}
