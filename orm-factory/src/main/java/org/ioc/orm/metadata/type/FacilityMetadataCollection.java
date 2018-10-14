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
package org.ioc.orm.metadata.type;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.metadata.EntityMetadataSelector;
import org.ioc.utils.Assertion;
import org.ioc.utils.collections.ArrayListSet;

import java.util.*;

import static org.ioc.orm.metadata.type.SchemaMetadata.findEntityMetadata;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class FacilityMetadataCollection implements EntityMetadataSelector, Iterable<FacilityMetadata> {
	private final Map<Class, FacilityMetadata> classMap = new HashMap<>();

	public FacilityMetadataCollection(Iterable<FacilityMetadata> metadataIterable) {
		Assertion.checkNotNull(metadataIterable);

		for (FacilityMetadata metadata : metadataIterable) {
			metadata.getTypes().forEach(clazz -> classMap.put(clazz, metadata));
		}
	}

	public Set<Class> getTypes() {
		return classMap.keySet();
	}

	@Override
	public FacilityMetadata getMetadata(Class<?> clazz) {
		if (clazz == null) {
			return null;
		}

		final FacilityMetadata facilityMetadata = classMap.get(clazz);
		if (facilityMetadata != null) {
			return facilityMetadata;
		}

		final Collection<FacilityMetadata> collection = findMeta(clazz);
		if (collection.isEmpty()) {
			return null;
		}

		if (collection.size() != 1) {
			throw new OrmException("Multiple entities available for [" + clazz + "].");
		}

		return collection.iterator().next();
	}

	@Override
	public final FacilityMetadata getMetadata(String type) {
		if (type == null || type.isEmpty()) {
			return null;
		}

		return classMap.values()
				.stream()
				.filter(entityMetadata -> findEntityMetadata(type, entityMetadata))
				.findFirst()
				.orElse(null);
	}

	private Collection<FacilityMetadata> findMeta(Class<?> clazz) {
		if (clazz == null) {
			return Collections.emptyList();
		}

		final FacilityMetadata facilityMetadata = classMap.get(clazz);
		if (facilityMetadata != null) {
			return Collections.singletonList(facilityMetadata);
		}

		final Set<FacilityMetadata> set = new ArrayListSet<>(4);
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
	public Collection<FacilityMetadata> collectAll() {
		return Collections.unmodifiableCollection(classMap.values());
	}

	@Override
	public boolean contains(Class<?> clazz) {
		return !findMeta(clazz).isEmpty();
	}

	@Override
	public boolean contains(FacilityMetadata facilityMetadata) {
		if (facilityMetadata == null) {
			return false;
		}

		return classMap.entrySet()
				.stream()
				.anyMatch(entry -> facilityMetadata == entry.getValue()
						|| facilityMetadata.compareTo(entry.getValue()) == 0);
	}

	@Override
	public int size() {
		return classMap.size();
	}

	@Override
	public Iterator<FacilityMetadata> iterator() {
		return Collections.unmodifiableCollection(classMap.values()).iterator();
	}
}
