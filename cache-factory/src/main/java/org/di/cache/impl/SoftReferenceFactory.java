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

import org.di.cache.ICache;
import org.di.cache.ICacheFactory;
import org.di.cache.facade.EternalFacade;
import org.di.cache.facade.SoftFacade;
import org.di.context.excepton.IoCException;
import org.di.context.excepton.starter.IoCStopException;
import org.di.context.utils.MethodInvocation;

/**
 * @author GenCloud
 * @date 16.09.2018
 */
public class SoftReferenceFactory implements ICacheFactory {
    private ICache<MethodInvocation, Object> invocationObjectICache;

    @Override
    public void initialize() throws IoCException {
        invocationObjectICache = install("soft-reference-interface-cache");
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
        return new SoftFacade<>(name);
    }

    @Override
    public <K, V> ICache<K, V> installEternal(String name, int size) {
        if (log.isDebugEnabled()) {
            log.debug("Creating eternal cache {} with minimum size of {}", name, size);
        }
        return new EternalFacade<>(name);
    }

    @Override
    public <K, V> ICache<K, V> install(String name) {
        return install(name, 200);
    }

    @Override
    public <K, V> void invalidate(ICache<K, V> ICache) {
        if (log.isDebugEnabled()) {
            log.debug("Disposing {}", ICache);
        }
        ICache.clear();
    }

    @Override
    public void destroy() throws IoCStopException {
        invalidate(invocationObjectICache);
        invocationObjectICache = null;
    }
}
