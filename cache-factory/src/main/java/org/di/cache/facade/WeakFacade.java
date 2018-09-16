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
import java.lang.ref.WeakReference;

/**
 * This class is a simple map implementation for cache usage.
 * Values from map will be removed after first garbage collector run
 * if there isn't any strong reference to value object.
 *
 * @author GenCloud
 * @date 16.09.2018
 */
public class WeakFacade<K, V> extends ReferenceICache<K, V> implements ICache<K, V> {
    public WeakFacade(String cacheName) {
        super(cacheName);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected synchronized void cleanQueue() {
        Entry entry;
        while ((entry = (Entry) referenceQueue.poll()) != null) {
            K key = entry.getKey();
            if (log.isDebugEnabled()) {
                log.debug("{}: cleaned up value for key: {}", cacheName, key);
            }
            referenceMap.remove(key);
        }
    }

    @Override
    protected Reference<V> newReference(K key, V value, ReferenceQueue<V> vReferenceQueue) {
        return new Entry(key, value, vReferenceQueue);
    }

    private class Entry extends WeakReference<V> {
        private K key;

        Entry(K key, V referent, ReferenceQueue<? super V> q) {
            super(referent, q);
            this.key = key;
        }

        K getKey() {
            return key;
        }
    }
}
