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

import net.sf.ehcache.Element;
import org.di.cache.ICache;

import java.util.Iterator;

/**
 * Simple cache that stores invocation results in a EhCache {@link net.sf.ehcache.Cache}.
 *
 * @author GenCloud
 * @date 16.09.2018
 */
public class EhFacade<K, V> implements ICache<K, V> {
    private final net.sf.ehcache.Cache cache;

    public EhFacade(net.sf.ehcache.Cache cache) {
        this.cache = cache;
    }

    public net.sf.ehcache.Cache getCache() {
        return cache;
    }

    @Override
    public void put(K key, V value) {
        cache.put(new Element(key, value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(K key) {
        final Element element = cache.get(key);
        if (element == null) {
            return null;
        }
        return (V) element.getObjectValue();
    }

    @Override
    public boolean contains(K key) {
        return cache.get(key) != null;
    }

    @Override
    public void remove(K key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.removeAll();
    }

    @Override
    public int size() {
        return cache.getSize();
    }

    @Override
    public Iterator<V> iterator() {
        return null;
    }
}
