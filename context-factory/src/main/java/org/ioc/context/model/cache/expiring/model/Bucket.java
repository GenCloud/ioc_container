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
package org.ioc.context.model.cache.expiring.model;

import javafx.util.Pair;
import org.ioc.context.model.cache.expiring.lock.LimitLock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A cache segment.
 * <p>
 * A CacheSegment is backed by a HashMap and is protected by a read/write lock.
 *
 * @param <K> the type of the keys
 * @param <V> the type of the values
 * @author GenCloud
 * @date 10/2018
 */
public class Bucket<K, V> {
	// read/write lock protecting mutations to the segment
	private ReadWriteLock segmentLock = new ReentrantReadWriteLock();

	private LimitLock readLock = new LimitLock(segmentLock.readLock());
	private LimitLock writeLock = new LimitLock(segmentLock.writeLock());

	private Map<K, CompletableFuture<Element<K, V>>> map = new HashMap<>();

	public ReadWriteLock getSegmentLock() {
		return segmentLock;
	}

	public LimitLock getReadLock() {
		return readLock;
	}

	public LimitLock getWriteLock() {
		return writeLock;
	}

	public Map<K, CompletableFuture<Element<K, V>>> getMap() {
		return map;
	}

	public void setMap(Map<K, CompletableFuture<Element<K, V>>> map) {
		this.map = map;
	}

	public CompletableFuture<Element<K, V>> getValueFromMap(K key) {
		return map.get(key);
	}

	public void removeFromMap(K key) {
		map.remove(key);
	}

	public CompletableFuture<Element<K, V>> putInMapIfAbsent(K key, CompletableFuture<Element<K, V>> value) {
		return map.putIfAbsent(key, value);
	}

	/**
	 * get an entry from the segment; expired entries will be returned as null but not removed from the cache until the LRU list is
	 * pruned or performed however a caller can take action using the provided callback
	 *
	 * @param key          the key of the entry to get from the cache
	 * @param now          the access time of this entry
	 * @param isExpired    test if the entry is expired
	 * @param onExpiration a callback if the entry associated to the key is expired
	 * @return the entry if there was one, otherwise null
	 */
	public Element<K, V> get(K key, long now, Predicate<Element<K, V>> isExpired, Consumer<Element<K, V>> onExpiration) {
		CompletableFuture<Element<K, V>> future;
		Element<K, V> element = null;
		try (LimitLock ignored = readLock.acquire()) {
			future = map.get(key);
		}

		if (future != null) {
			try {
				element = future.handle((ok, ex) -> {
					if (ok != null && !isExpired.test(ok)) {
						ok.setAccessTime(now);
						return ok;
					} else {
						if (ok != null) {
							assert isExpired.test(ok);
							onExpiration.accept(ok);
						}
						return null;
					}
				}).get();
			} catch (ExecutionException | InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}
		return element;
	}

	/**
	 * put an entry into the segment
	 *
	 * @param key   the key of the entry to add to the cache
	 * @param value the value of the entry to add to the cache
	 * @param now   the access time of this entry
	 * @return a tuple of the new entry and the existing entry, if there was one otherwise null
	 */
	public Pair<Element<K, V>, Element<K, V>> put(K key, V value, long now) {
		Element<K, V> element = new Element<>(key, value, now);
		Element<K, V> existing = null;
		try (LimitLock ignored = writeLock.acquire()) {
			try {
				CompletableFuture<Element<K, V>> future = map.put(key, CompletableFuture.completedFuture(element));
				if (future != null) {
					existing = future.handle((ok, ex) -> {
						if (ok != null) {
							return ok;
						} else {
							return null;
						}
					}).get();
				}
			} catch (ExecutionException | InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}
		return new Pair<>(element, existing);
	}

	public boolean contains(K key) {
		return map.containsKey(key);
	}

	/**
	 * remove an entry from the segment
	 *
	 * @param key the key of the entry to remove from the cache
	 * @return the removed entry if there was one, otherwise null
	 */
	public Element<K, V> remove(K key) {
		CompletableFuture<Element<K, V>> future;
		Element<K, V> element = null;
		try (LimitLock ignored = writeLock.acquire()) {
			future = map.remove(key);
		}
		if (future != null) {
			try {
				element = future.handle((ok, ex) -> {
					if (ok != null) {
						return ok;
					} else {
						return null;
					}
				}).get();
			} catch (ExecutionException | InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}
		return element;
	}
}
