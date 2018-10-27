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

import org.ioc.context.model.cache.expiring.Loader;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

/**
 * Interface represents a Map structure for cache usage.
 *
 * @author GenCloud
 * @date 09/2018
 */
public interface ICache<K, V> {
	/**
	 * Add pair <K, V> to cache.
	 * <p>
	 * Notice: if there is already a value with given id in map,
	 * {@link IllegalArgumentException} will be thrown.
	 *
	 * @param key   key name
	 * @param value cache content value
	 */
	V put(K key, V value);

	/**
	 * If the specified key is not already associated with a value (or is mapped to null), attempts to compute its
	 * value using the given mapping function and enters it into this map unless null. The load method for a given key
	 * will be invoked at most once.
	 * <p>
	 * Use of different {@link Loader} implementations on the same key concurrently may result in only the first
	 * loader function being called and the second will be returned the result provided by the first including any exceptions
	 * thrown during the execution of the first.
	 *
	 * @param key    the key whose associated value is to be returned or computed for if non-existent
	 * @param loader the function to compute a value given a key
	 * @return the current (existing or computed) non-null value associated with the specified key
	 * @throws ExecutionException thrown if loader throws an exception or returns a null value
	 */
	default V computeIfAbsent(K key, Loader<K, V> loader) throws ExecutionException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns cached value correlated to given key.
	 *
	 * @param key key
	 * @return cached value for this key
	 */
	V get(K key);

	/**
	 * Checks whether this map contains a value related to given key.
	 *
	 * @param key key
	 * @return true if key has an value
	 */
	boolean contains(K key);

	/**
	 * Removes an entry from map, that has given key.
	 *
	 * @param key key
	 */
	V remove(K key);

	/**
	 * Clears cache.
	 */
	void clear();

	/**
	 * @return size of cache map
	 */
	int size();

	/**
	 * Returns an iterator over elements of type {@code K}.
	 *
	 * @return an Iterator.
	 */
	Iterator<K> keys();

	/**
	 * Returns an iterator over elements of type {@code V}.
	 *
	 * @return an Iterator.
	 */
	Iterator<V> values();
}
