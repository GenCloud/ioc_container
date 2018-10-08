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
package org.ioc.cache.facade;

import net.sf.ehcache.Element;
import org.ioc.cache.ICache;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple cache that stores invocation results in a EhCache {@link net.sf.ehcache.Cache}.
 *
 * @author GenCloud
 * @date 09/2018
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
	@SuppressWarnings("unchecked")
	public Iterator<V> iterator() {
		final Map<Object, Element> mapElements = cache.getAll(cache.getKeys());
		return (Iterator<V>) mapElements.values()
				.stream()
				.map(Element::getObjectValue)
				.collect(Collectors.toList())
				.iterator();
	}
}
