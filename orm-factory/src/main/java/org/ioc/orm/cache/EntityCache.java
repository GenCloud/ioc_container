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
package org.ioc.orm.cache;

import org.ioc.orm.metadata.type.EntityMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class EntityCache {
	private static final Logger log = LoggerFactory.getLogger(EntityCache.class);

	private final Map<EntityMetadata, Map<Object, WeakReference>> cache = new TreeMap<>();

	private int purgeCount = 0;

	public void invalidateAll() {
		cache.values().forEach(Map::clear);
		cache.clear();
	}

	@SuppressWarnings("unchecked")
	public <T> T find(EntityMetadata entityMetadata, Object key, Class<T> clazz) {
		if (entityMetadata == null || key == null) {
			return null;
		}

		final Map<Object, WeakReference> entities = cache.get(entityMetadata);
		if (entities == null) {
			return null;
		}

		final WeakReference reference = entities.get(key);
		if (reference == null) {
			return null;
		}

		final Object instance = reference.get();
		if (instance == null) {
			entities.remove(key);
			return null;
		}

		if (clazz == null || Object.class.equals(clazz)) {
			return (T) instance;
		}

		try {
			return clazz.cast(instance);
		} catch (ClassCastException e) {
			return null;
		}
	}

	public <T> Map<Object, T> find(EntityMetadata meta, Collection<?> keys, Class<T> clazz) {
		if (meta == null || keys == null || keys.isEmpty()) {
			return Collections.emptyMap();
		}

		final Map<Object, T> map = new HashMap<>();
		keys.forEach(key -> {
			final T item = find(meta, key, clazz);
			if (item != null) {
				map.put(key, item);
			}
		});
		return Collections.unmodifiableMap(map);
	}

	public boolean remove(EntityMetadata meta, Object key) {
		if (meta == null || meta.getIdVisitor() == null || key == null) {
			return false;
		}

		final Map<Object, WeakReference> entities = cache.get(meta);
		if (entities == null) {
			return false;
		}

		if (entities.remove(key) != null) {
			checkPurge();
			return true;
		} else {
			return false;
		}
	}

	public boolean put(EntityMetadata meta, Object key, Object instance) {
		if (meta == null || key == null) {
			return false;
		}

		Map<Object, WeakReference> entities = cache.get(meta);
		if (entities == null) {
			entities = new ConcurrentHashMap<>();
			cache.put(meta, entities);
		}

		try {
			final boolean updated;
			if (instance != null) {
				final WeakReference reference = new WeakReference<>(instance);
				entities.put(key, reference);
				updated = true;
			} else {
				updated = entities.remove(key) != null;
			}

			if (updated) {
				checkPurge();
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.warn("Unable to retrieve key to getEntityCache entity [" + instance + "] of type [" + meta + "].", e);
			return false;
		}
	}

	private synchronized void checkPurge() {
		purgeCount++;
		final int purgeDelayCount = 1000;
		if (purgeCount >= purgeDelayCount) {
			try {
				new ArrayList<>(cache.entrySet()).forEach(mapEntry -> {
					final Map<Object, WeakReference> map = mapEntry.getValue();
					if (map != null) {
						new ArrayList<>(map.entrySet())
								.stream()
								.filter(entry -> entry.getValue().get() == null)
								.map(Map.Entry::getKey)
								.forEachOrdered(map::remove);
					}

					if (map == null || map.isEmpty()) {
						cache.remove(mapEntry.getKey());
					}
				});
			} finally {
				purgeCount = 0;
			}
		}
	}
}
