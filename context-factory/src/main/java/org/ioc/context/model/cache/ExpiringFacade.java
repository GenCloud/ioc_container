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

import javafx.util.Pair;
import org.ioc.context.model.cache.expiring.Loader;
import org.ioc.context.model.cache.expiring.RemovalFact;
import org.ioc.context.model.cache.expiring.lock.LimitLock;
import org.ioc.context.model.cache.expiring.model.Bucket;
import org.ioc.context.model.cache.expiring.model.Element;
import org.ioc.context.model.cache.expiring.model.RemovalNotify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.ToLongBiFunction;
import java.util.stream.IntStream;

import static org.ioc.context.model.cache.expiring.model.Element.State.DELETED;
import static org.ioc.context.model.cache.expiring.model.Element.State.EXISTING;
import static org.ioc.context.model.cache.expiring.model.RemovalNotify.RemovalReason.*;

/**
 * A simple concurrent cache.
 * <p>
 * Cache is a simple concurrent cache that supports time-based and weight-based evictions, with notifications for all
 * evictions. The design goals for this cache were simplicity and read performance. This means that we are willing to
 * accept reduced write performance in exchange for easy-to-understand code. Cache statistics for hits, misses and
 * evictions are exposed.
 * <p>
 * The design of the cache is relatively simple. The cache is segmented into 256 segments which are backed by HashMaps.
 * Each segment is protected by a re-entrant read/write lock. The read/write locks permit multiple concurrent readers
 * without contention, and the segments gives us write throughput without impacting readers (so readers are blocked only
 * if they are reading a segment that a writer is writing to).
 *
 * @author GenCloud
 * @date 10/2018
 */
@SuppressWarnings("unchecked")
public class ExpiringFacade<K, V> implements ICache<K, V> {
	private static final int NUMBER_OF_SEGMENTS = 256;
	private final Bucket<K, V>[] buckets = new Bucket[NUMBER_OF_SEGMENTS];
	private long expireAfterAccessNanos = -1, expireAfterWriteNanos = -1;
	private long weight = 0, maximumWeight = -1;
	private boolean entriesExpireAfterAccess, entriesExpireAfterWrite;
	private int count = 0;
	private ToLongBiFunction<K, V> weigher = (k, v) -> 1;
	private RemovalFact<K, V> removalFact = notification -> {
	};
	private Element<K, V> head, tail;
	private LimitLock lruLock = new LimitLock(new ReentrantLock());

	public ExpiringFacade() {
		Arrays.setAll(buckets, i -> new Bucket<>());
	}

	public void setExpireAfterAccessNanos(long expireAfterAccessNanos) {
		this.expireAfterAccessNanos = expireAfterAccessNanos;
		entriesExpireAfterAccess = true;
	}

	public void setExpireAfterWriteNanos(long expireAfterWriteNanos) {
		this.expireAfterWriteNanos = expireAfterWriteNanos;
		entriesExpireAfterWrite = true;
	}

	public void setMaximumWeight(long maximumWeight) {
		this.maximumWeight = maximumWeight;
	}

	public void setWeigher(ToLongBiFunction<K, V> weigher) {
		this.weigher = weigher;
	}

	public void setRemovalFact(RemovalFact<K, V> removalFact) {
		this.removalFact = removalFact;
	}

	/**
	 * Add pair <K, V> to cache.
	 * <p>
	 * Notice: if there is already a value with given id in map,
	 * {@link IllegalArgumentException} will be thrown.
	 *
	 * @param key   key name
	 * @param value cache content value
	 */
	@Override
	public V put(K key, V value) {
		final long now = now();
		final Bucket<K, V> bucket = getBucket(key);
		final Pair<Element<K, V>, Element<K, V>> pair = bucket.put(key, value, now);
		boolean replaced = false;
		try (LimitLock ignored = lruLock.acquire()) {
			if (pair.getValue() != null && pair.getValue().getState() == EXISTING) {
				if (unlink(pair.getValue())) {
					replaced = true;
				}
			}
			promote(pair.getKey(), now);
		}

		if (replaced) {
			removalFact.onRemoval(new RemovalNotify<>(pair.getValue().getKey(), pair.getKey().getValue(), REPLACED));
		}

		return value;
	}

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
	@Override
	public V computeIfAbsent(K key, Loader<K, V> loader) throws ExecutionException {
		final long now = now();
		V value = get(key, now, e -> {
			try (LimitLock ignored = lruLock.acquire()) {
				evictEntry(e);
			}
		});

		if (value == null) {
			final Bucket<K, V> bucket = getBucket(key);
			CompletableFuture<Element<K, V>> future;
			final CompletableFuture<Element<K, V>> completableFuture = new CompletableFuture<>();

			try (LimitLock ignored = bucket.getWriteLock().acquire()) {
				future = bucket.putInMapIfAbsent(key, completableFuture);
			}

			final BiFunction<? super Element<K, V>, Throwable, ? extends V> handler = (ok, ex) -> {
				if (ok != null) {
					try (LimitLock ignored = lruLock.acquire()) {
						promote(ok, now);
					}
					return ok.getValue();
				} else {
					try (LimitLock ignored = bucket.getWriteLock().acquire()) {
						final CompletableFuture<Element<K, V>> fromMap = bucket.getValueFromMap(key);
						if (fromMap != null && fromMap.isCompletedExceptionally()) {
							bucket.removeFromMap(key);
						}
					}
					return null;
				}
			};

			CompletableFuture<V> completableValue;
			if (future == null) {
				future = completableFuture;
				completableValue = future.handle(handler);
				V loaded;
				try {
					loaded = loader.load(key);
				} catch (Exception e) {
					future.completeExceptionally(e);
					throw new ExecutionException(e);
				}
				if (loaded == null) {
					final NullPointerException npe = new NullPointerException("Loader returned a null value");
					future.completeExceptionally(npe);
					throw new ExecutionException(npe);
				} else {
					future.complete(new Element<>(key, loaded, now));
				}
			} else {
				completableValue = future.handle(handler);
			}

			try {
				value = completableValue.get();
				if (future.isCompletedExceptionally()) {
					future.get();
					throw new IllegalStateException("Future was completed exceptionally but no exception was thrown");
				}
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}
		return value;
	}

	/**
	 * Returns cached value correlated to given key.
	 *
	 * @param key key
	 * @return cached value for this key
	 */
	@Override
	public V get(K key) {
		return get(key, now(), e -> {
		});
	}

	private V get(K key, long now, Consumer<Element<K, V>> onExpiration) {
		final Bucket<K, V> bucket = getBucket(key);
		final Element<K, V> element = bucket.get(key, now, e -> isExpired(e, now), onExpiration);
		if (element == null) {
			return null;
		} else {
			promote(element, now);
			return element.getValue();
		}
	}

	/**
	 * Checks whether this map contains a value related to given key.
	 *
	 * @param key key
	 * @return true if key has an value
	 */
	@Override
	public boolean contains(K key) {
		final Bucket<K, V> bucket = getBucket(key);
		return bucket.contains(key);
	}

	/**
	 * Removes an entry from map, that has given key.
	 *
	 * @param key key
	 */
	@Override
	public V remove(K key) {
		final Bucket<K, V> bucket = getBucket(key);
		final Element<K, V> element = bucket.remove(key);
		if (element != null) {
			try (LimitLock ignored = lruLock.acquire()) {
				delete(element, INVALIDATED);
			}

			return element.getValue();
		}

		return null;
	}

	/**
	 * Clears cache.
	 */
	@Override
	public void clear() {
		Element<K, V> h;

		boolean[] haveSegmentLock = new boolean[NUMBER_OF_SEGMENTS];
		try {
			IntStream.range(0, NUMBER_OF_SEGMENTS).forEach(i -> {
				buckets[i].getSegmentLock().writeLock().lock();
				haveSegmentLock[i] = true;
			});

			try (LimitLock ignored = lruLock.acquire()) {
				h = head;
				Arrays.stream(buckets).forEach(segment -> segment.setMap(new HashMap<>()));
				Element<K, V> current = head;
				while (current != null) {
					current.setState(DELETED);
					current = current.getAfter();
				}

				head = tail = null;
				count = 0;
				weight = 0;
			}
		} finally {
			for (int i = NUMBER_OF_SEGMENTS - 1; i >= 0; i--) {
				if (haveSegmentLock[i]) {
					buckets[i].getSegmentLock().writeLock().unlock();
				}
			}
		}

		while (h != null) {
			removalFact.onRemoval(new RemovalNotify<>(h.getKey(), h.getValue(), INVALIDATED));
			h = h.getAfter();
		}
	}

	/**
	 * @return size of cache map
	 */
	@Override
	public int size() {
		return count;
	}

	@Override
	public Iterator<K> keys() {
		return new Iterator<K>() {
			private CacheIterator iterator = new CacheIterator(head);

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public K next() {
				return iterator.next().getKey();
			}

			@Override
			public void remove() {
				iterator.remove();
			}
		};
	}

	@Override
	public Iterator<V> values() {
		return new Iterator<V>() {
			private CacheIterator iterator = new CacheIterator(head);

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public V next() {
				return iterator.next().getValue();
			}
		};
	}

	private void promote(Element<K, V> element, long now) {
		boolean promoted = true;
		try (LimitLock ignored = lruLock.acquire()) {
			switch (element.getState()) {
				case DELETED:
					promoted = false;
					break;
				case EXISTING:
					relinkAtHead(element);
					break;
				case NEW:
					linkAtHead(element);
					break;
			}

			if (promoted) {
				evict(now);
			}
		}
	}

	private void evict(long now) {
		assert lruLock.isHeldByCurrentThread();

		while (tail != null && shouldPrune(tail, now)) {
			evictEntry(tail);
		}
	}

	private void evictEntry(Element<K, V> element) {
		assert lruLock.isHeldByCurrentThread();

		final Bucket<K, V> bucket = getBucket(element.getKey());
		if (bucket != null) {
			bucket.remove(element.getKey());
		}

		delete(element, EVICTED);
	}

	private void delete(Element<K, V> element, RemovalNotify.RemovalReason removalReason) {
		assert lruLock.isHeldByCurrentThread();

		if (unlink(element)) {
			removalFact.onRemoval(new RemovalNotify<>(element.getKey(), element.getValue(), removalReason));
		}
	}

	private boolean shouldPrune(Element<K, V> element, long now) {
		return exceedsWeight() || isExpired(element, now);
	}

	private boolean exceedsWeight() {
		return maximumWeight != -1 && weight > maximumWeight;
	}

	private boolean isExpired(Element<K, V> element, long now) {
		return (entriesExpireAfterAccess && now - element.getAccessTime() > expireAfterAccessNanos) ||
				(entriesExpireAfterWrite && now - element.getWriteTime() > expireAfterWriteNanos);
	}

	private boolean unlink(Element<K, V> element) {
		assert lruLock.isHeldByCurrentThread();

		if (element.getState() == EXISTING) {
			final Element<K, V> before = element.getBefore();
			final Element<K, V> after = element.getAfter();

			if (before == null) {
				assert head == element;
				head = after;
				if (head != null) {
					head.setBefore(null);
				}
			} else {
				before.setAfter(after);
				element.setBefore(null);
			}

			if (after == null) {
				assert tail == element;
				tail = before;
				if (tail != null) {
					tail.setAfter(null);
				}
			} else {
				after.setBefore(before);
				element.setAfter(null);
			}

			count--;
			weight -= weigher.applyAsLong(element.getKey(), element.getValue());
			element.setState(DELETED);
			return true;
		} else {
			return false;
		}
	}

	private void linkAtHead(Element<K, V> element) {
		assert lruLock.isHeldByCurrentThread();

		Element<K, V> h = head;
		element.setBefore(null);
		element.setAfter(head);
		head = element;
		if (h == null) {
			tail = element;
		} else {
			h.setBefore(element);
		}

		count++;
		weight += weigher.applyAsLong(element.getKey(), element.getValue());
		element.setState(EXISTING);
	}

	private void relinkAtHead(Element<K, V> element) {
		assert lruLock.isHeldByCurrentThread();

		if (head != element) {
			unlink(element);
			linkAtHead(element);
		}
	}

	private Bucket<K, V> getBucket(K key) {
		return buckets[key.hashCode() & 0xff];
	}

	protected long now() {
		return entriesExpireAfterAccess || entriesExpireAfterWrite ? System.nanoTime() : 0;
	}

	public class CacheIterator implements Iterator<Element<K, V>> {
		private Element<K, V> current;
		private Element<K, V> next;

		CacheIterator(Element<K, V> head) {
			current = null;
			next = head;
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public Element<K, V> next() {
			current = next;
			next = next.getAfter();
			return current;
		}

		@Override
		public void remove() {
			Element<K, V> element = current;
			if (element != null) {
				final Bucket<K, V> bucket = getBucket(element.getKey());
				bucket.remove(element.getKey());
				try (LimitLock ignored = lruLock.acquire()) {
					current = null;
					delete(element, INVALIDATED);
				}
			}
		}
	}
}
