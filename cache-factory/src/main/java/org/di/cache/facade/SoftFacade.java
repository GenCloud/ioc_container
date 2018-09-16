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
package org.di.cache.facade;

import org.di.cache.ICache;
import org.di.cache.impl.ReferenceICache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * This class is a simple map implementation for cache usage.
 * Value may be stored in map really long, but it for sure will be removed
 * if there is low memory (and of course there isn't any strong reference to value object)
 *
 * @author GenCloud
 * @date 16.09.2018
 * @see SoftReference
 */
public class SoftFacade<K, V> extends ReferenceICache<K, V> implements ICache<K, V> {
    public SoftFacade(String cacheName) {
        super(cacheName);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected synchronized void cleanQueue() {
        SoftEntry softEntry;
        while ((softEntry = (SoftEntry) referenceQueue.poll()) != null) {
            K key = softEntry.getKey();
            if (log.isDebugEnabled()) {
                log.debug("{} : cleaned up value for key: {}", cacheName, key);
            }
            referenceMap.remove(key);
        }
    }

    @Override
    protected Reference<V> newReference(K key, V value, ReferenceQueue<V> vReferenceQueue) {
        return new SoftEntry(key, value, vReferenceQueue);
    }

    private class SoftEntry extends SoftReference<V> {
        private K key;

        SoftEntry(K key, V referent, ReferenceQueue<? super V> q) {
            super(referent, q);
            this.key = key;
        }

        K getKey() {
            return key;
        }
    }
}