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
package org.ioc.orm.metadata.type;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.metadata.EntityMetadataSelector;
import org.ioc.utils.Assertion;
import org.ioc.utils.collections.ArrayListSet;

import java.util.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class EntityMetadataCollection implements EntityMetadataSelector, Iterable<EntityMetadata> {
	private final Map<Class, EntityMetadata> classMap = new HashMap<>();

	public EntityMetadataCollection(Iterable<EntityMetadata> metadataIterable) {
		Assertion.checkNotNull(metadataIterable, "meta iter");

		for (EntityMetadata metadata : metadataIterable) {
			metadata.getTypes().forEach(clazz -> classMap.put(clazz, metadata));
		}
	}

	public Set<Class> getTypes() {
		return classMap.keySet();
	}

	@Override
	public EntityMetadata getMetadata(Class<?> clazz) {
		if (clazz == null) {
			return null;
		}

		final EntityMetadata entityMetadata = classMap.get(clazz);
		if (entityMetadata != null) {
			return entityMetadata;
		}

		final Collection<EntityMetadata> collection = findMeta(clazz);
		if (collection.isEmpty()) {
			return null;
		}

		if (collection.size() != 1) {
			throw new OrmException("Multiple entities available for [" + clazz + "].");
		}

		return collection.iterator().next();
	}

	@Override
	public final EntityMetadata getMetadata(String labelOrType) {
		if (labelOrType == null || labelOrType.isEmpty()) {
			return null;
		}

		return classMap.values()
				.stream()
				.filter(meta -> SchemaMetadata.findEntityMetadata(labelOrType, meta))
				.findFirst()
				.orElse(null);
	}

	private Collection<EntityMetadata> findMeta(Class<?> clazz) {
		if (clazz == null) {
			return Collections.emptyList();
		}

		final EntityMetadata entityMetadata = classMap.get(clazz);
		if (entityMetadata != null) {
			return Collections.singletonList(entityMetadata);
		}

		final Set<EntityMetadata> set = new ArrayListSet<>(4);
		classMap.forEach((key, entityMeta) -> {
			if (((Class<?>) key).isAssignableFrom(clazz) || clazz.isAssignableFrom(key)) {
				set.add(entityMeta);
			}
		});

		if (set.isEmpty()) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableCollection(set);
		}
	}

	@Override
	public Collection<EntityMetadata> collectAll() {
		return Collections.unmodifiableCollection(classMap.values());
	}

	@Override
	public boolean contains(Class<?> clazz) {
		return !findMeta(clazz).isEmpty();
	}

	@Override
	public boolean contains(EntityMetadata entityMetadata) {
		if (entityMetadata == null) {
			return false;
		}

		return classMap.entrySet()
				.stream()
				.anyMatch(entry -> entityMetadata == entry.getValue()
						|| entityMetadata.compareTo(entry.getValue()) == 0);
	}

	@Override
	public int size() {
		return classMap.size();
	}

	@Override
	public Iterator<EntityMetadata> iterator() {
		return Collections.unmodifiableCollection(classMap.values()).iterator();
	}
}
