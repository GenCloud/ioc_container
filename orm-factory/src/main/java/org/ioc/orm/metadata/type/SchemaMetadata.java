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

import org.ioc.orm.metadata.EntityMetadataSelector;
import org.ioc.utils.Assertion;

import java.util.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SchemaMetadata implements EntityMetadataSelector, Iterable<EntityMetadata> {
	private final EntityMetadataCollection entityMetadataCollection;

	public SchemaMetadata(Collection<EntityMetadata> collection) {
		Assertion.checkNotNull(collection, "collection");
		Assertion.checkArgument(!collection.isEmpty(), "empty");

		entityMetadataCollection = new EntityMetadataCollection(collection);
	}

	static boolean findEntityMetadata(String labelOrType, EntityMetadata meta) {
		if (labelOrType.equalsIgnoreCase(meta.getName())) {
			return true;
		} else if (labelOrType.equalsIgnoreCase(meta.getTable())) {
			return true;
		} else {
			return meta.getTypes()
					.stream()
					.anyMatch(clazz -> labelOrType.equalsIgnoreCase(clazz.getSimpleName()));
		}
	}

	public Set<Class> getTypes() {
		return entityMetadataCollection.getTypes();
	}

	public Collection<String> getTables() {
		final Set<String> tables = new TreeSet<>();
		for (EntityMetadata metadata : entityMetadataCollection) {
			tables.add(metadata.getTable());
		}
		return Collections.unmodifiableCollection(tables);
	}

	public Collection<EntityMetadata> getEntityMetadataCollection() {
		return entityMetadataCollection.collectAll();
	}

	public EntityMetadata getEntity(String labelOrType) {
		if (labelOrType == null || labelOrType.isEmpty()) {
			return null;
		}

		for (EntityMetadata meta : entityMetadataCollection) {
			if (findEntityMetadata(labelOrType, meta)) {
				return meta;
			}
		}
		return null;
	}

	@Override
	public Iterator<EntityMetadata> iterator() {
		return entityMetadataCollection.iterator();
	}

	@Override
	public EntityMetadata getMetadata(Class<?> clazz) {
		return entityMetadataCollection.getMetadata(clazz);
	}

	@Override
	public EntityMetadata getMetadata(String labelOrType) {
		return entityMetadataCollection.getMetadata(labelOrType);
	}

	@Override
	public boolean contains(Class<?> clazz) {
		return entityMetadataCollection.contains(clazz);
	}

	@Override
	public boolean contains(EntityMetadata entityMetadata) {
		return entityMetadataCollection.contains(entityMetadata);
	}

	@Override
	public Collection<EntityMetadata> collectAll() {
		return entityMetadataCollection.collectAll();
	}

	@Override
	public int size() {
		return entityMetadataCollection.size();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SchemaMetadata that = (SchemaMetadata) o;

		return Objects.equals(entityMetadataCollection, that.entityMetadataCollection);
	}

	@Override
	public int hashCode() {
		return entityMetadataCollection != null ? entityMetadataCollection.hashCode() : 0;
	}
}
