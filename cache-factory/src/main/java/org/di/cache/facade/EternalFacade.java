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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cache class for an eternal cache. Entries in this cache instance won't ever
 * be automatically removed, even if the JVM is running out of memory.
 *
 * @author GenCloud
 * @date 16.09.2018
 */
public class EternalFacade<K, V> implements ICache<K, V> {
    private static final Logger log = LoggerFactory.getLogger(EternalFacade.class);

    private final String cacheName;

    private final Map<K, V> map = new HashMap<>();

    public EternalFacade(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
        if (log.isDebugEnabled()) {
            log.debug("{}: added for key: {}", cacheName, key);
        }
    }

    @Override
    public V get(K key) {
        V v = map.get(key);
        if (v != null) {
            if (log.isDebugEnabled()) {
                log.debug("{}: obtained for key: {}", cacheName, key);
            }
        }
        return v;
    }

    @Override
    public boolean contains(K key) {
        return map.containsKey(key);
    }

    @Override
    public void remove(K key) {
        map.remove(key);
        if (log.isDebugEnabled()) {
            log.debug("{}: removed for key: {}", cacheName, key);
        }
    }

    @Override
    public void clear() {
        map.clear();
        if (log.isDebugEnabled()) {
            log.debug("{}: cleared", cacheName);
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Iterator<V> iterator() {
        return map.values().iterator();
    }
}
