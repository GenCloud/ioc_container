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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author GenCloud
 * @date 16.09.2018
 */
public abstract class ReferenceICache<K, V> implements ICache<K, V> {
    protected final Logger log = LoggerFactory.getLogger(getClass().getSimpleName());

    protected final String cacheName;

    protected final Map<K, Reference<V>> referenceMap = new HashMap<>();

    protected final ReferenceQueue<V> referenceQueue = new ReferenceQueue<>();

    protected ReferenceICache(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public void put(K key, V value) {
        cleanQueue();

        final Reference<V> reference = newReference(key, value, referenceQueue);
        referenceMap.put(key, reference);

        if (log.isDebugEnabled()) {
            log.debug("{}: added for key: {}", cacheName, key);
        }
    }

    @Override
    public V get(K key) {
        cleanQueue();

        final Reference<V> reference = referenceMap.get(key);
        if (reference == null) {
            return null;
        }

        final V v = reference.get();
        if (v != null) {
            if (log.isDebugEnabled()) {
                log.debug("{}: obtained for key: {}", cacheName, key);
            }
        }

        return v;
    }

    @Override
    public boolean contains(K key) {
        cleanQueue();
        return referenceMap.containsKey(key);
    }

    protected abstract void cleanQueue();

    @Override
    public void remove(K key) {
        referenceMap.remove(key);
        if (log.isDebugEnabled()) {
            log.debug("{}: removed for key: {}", cacheName, key);
        }
    }

    @Override
    public void clear() {
        referenceMap.clear();
        if (log.isDebugEnabled()) {
            log.debug("{}: cleared", cacheName);
        }
    }

    @Override
    public int size() {
        return referenceMap.size();
    }

    @Override
    public Iterator<V> iterator() {
        cleanQueue();
        return new Iterator<V>() {
            private final Iterator<Reference<V>> iterator = referenceMap.values().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public V next() {
                return iterator.next().get();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    protected abstract Reference<V> newReference(K key, V value, ReferenceQueue<V> queue);
}
