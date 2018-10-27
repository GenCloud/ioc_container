/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
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
package org.ioc.context.model.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * Simple cache that stores invocation results in a EhCache {@link net.sf.ehcache.Cache}.
 *
 * @author GenCloud
 * @date 09/2018
 */
@SuppressWarnings("unchecked")
public class EhFacade<K, V> implements ICache<K, V> {
	private final Cache cache;

	public EhFacade(Cache cache) {
		this.cache = cache;
	}

	public Cache getCache() {
		return cache;
	}

	@Override
	public V put(K key, V value) {
		cache.put(new Element(key, value));
		return value;
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
	public V remove(K key) {
		final V element = get(key);
		if (element != null) {
			cache.remove(key);
		}
		return element;
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
	public Iterator<K> keys() {
		return cache.getKeys().iterator();
	}

	@Override
	public Iterator<V> values() {
		return (Iterator<V>) cache.getAll(cache.getKeys()).values()
				.stream()
				.map(Element::getObjectValue)
				.collect(Collectors.toList())
				.iterator();
	}
}
