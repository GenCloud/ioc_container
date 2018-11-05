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
package org.ioc.orm.cache;

import org.ioc.context.model.cache.ICache;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.utils.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom implementation of cache for entities.
 *
 * @author GenCloud
 * @date 10/2018
 */
public class FacilityCacheManager {
	private static final Logger log = LoggerFactory.getLogger(FacilityCacheManager.class);

	private final ICache<FacilityMetadata, Map<Object, WeakReference>> cache;

	public FacilityCacheManager(ICache<FacilityMetadata, Map<Object, WeakReference>> cache) {
		Assertion.checkNotNull(cache);

		this.cache = cache;
	}

	public <T> Map<Object, T> getAll(FacilityMetadata facilityMetadata, Class<T> clazz) {
		if (facilityMetadata == null) {
			return null;
		}

		final Map<Object, WeakReference> entities = cache.get(facilityMetadata);
		if (entities == null) {
			return null;
		}

		final Map<Object, T> map = new HashMap<>();
		entities.forEach((k, v) -> {
			final Object instance = v.get();
			if (instance != null) {
				map.putIfAbsent(k, (T) instance);
			}
		});

		return Collections.unmodifiableMap(map);
	}

	/**
	 * Getting entity from cache.
	 *
	 * @param facilityMetadata entity meta data for find
	 * @param key              identifiable primary key
	 * @param clazz            entity type
	 * @param <T>              entity generic type
	 * @return cached entity
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(FacilityMetadata facilityMetadata, Object key, Class<T> clazz) {
		if (facilityMetadata == null || key == null) {
			return null;
		}

		final Map<Object, WeakReference> entities = cache.get(facilityMetadata);
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

	/**
	 * Getting entities from cache.
	 *
	 * @param facilityMetadata entity meta data for find
	 * @param objects          keys of finned entities
	 * @param clazz            entity type
	 * @param <T>              entity generic type
	 * @return bucket of entity
	 */
	public <T> Map<Object, T> get(FacilityMetadata facilityMetadata, Collection<?> objects, Class<T> clazz) {
		if (facilityMetadata == null || objects == null || objects.isEmpty()) {
			return Collections.emptyMap();
		}

		final Map<Object, T> map = new HashMap<>();
		objects.forEach(o -> {
			final T item = get(facilityMetadata, o, clazz);
			if (item != null) {
				map.put(o, item);
			}
		});
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Remove entity from cache.
	 *
	 * @param facilityMetadata entity meta data for remove
	 * @param key              identifiable primary key
	 * @return true if entity has been removed
	 */
	public boolean delete(FacilityMetadata facilityMetadata, Object key) {
		if (facilityMetadata == null || facilityMetadata.getIdVisitor() == null || key == null) {
			return false;
		}

		final Map<Object, WeakReference> referenceMap = cache.get(facilityMetadata);
		if (referenceMap == null) {
			return false;
		}

		return referenceMap.remove(key) != null;
	}

	/**
	 * Add entity to cache.
	 *
	 * @param facilityMetadata entity meta data for add (key)
	 * @param key              identifiable primary key
	 * @param instance         initialized instance of entity
	 * @return true if entity is added to cache
	 */
	public boolean add(FacilityMetadata facilityMetadata, Object key, Object instance) {
		if (facilityMetadata == null || key == null) {
			return false;
		}

		Map<Object, WeakReference> referenceMap = cache.get(facilityMetadata);
		if (referenceMap == null) {
			referenceMap = new ConcurrentHashMap<>();
			cache.put(facilityMetadata, referenceMap);
		}

		try {
			final boolean updated;
			if (instance != null) {
				final WeakReference reference = new WeakReference<>(instance);
				referenceMap.put(key, reference);
				updated = true;
			} else {
				updated = referenceMap.remove(key) != null;
			}

			return updated;
		} catch (Exception e) {
			log.warn("Unable to retrieve key to getEntityCache entity [" + instance + "] of type [" + facilityMetadata + "].", e);
			return false;
		}
	}
}
