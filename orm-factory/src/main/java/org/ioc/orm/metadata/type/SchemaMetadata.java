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

import org.ioc.orm.metadata.EntityMetadataSelector;
import org.ioc.utils.Assertion;

import java.util.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SchemaMetadata implements EntityMetadataSelector, Iterable<FacilityMetadata> {
	private final FacilityMetadataCollection facilityMetadataCollection;

	public SchemaMetadata(Collection<FacilityMetadata> collection) {
		Assertion.checkNotNull(collection, "collection");
		Assertion.checkArgument(!collection.isEmpty(), "empty");

		facilityMetadataCollection = new FacilityMetadataCollection(collection);
	}

	static boolean findEntityMetadata(String labelOrType, FacilityMetadata facilityMetadata) {
		if (labelOrType.equalsIgnoreCase(facilityMetadata.getName())) {
			return true;
		} else if (labelOrType.equalsIgnoreCase(facilityMetadata.getTable())) {
			return true;
		} else {
			return facilityMetadata.getTypes()
					.stream()
					.anyMatch(clazz -> labelOrType.equalsIgnoreCase(clazz.getSimpleName()));
		}
	}

	public Set<Class> getTypes() {
		return facilityMetadataCollection.getTypes();
	}

	public Collection<String> getTables() {
		final Set<String> tables = new TreeSet<>();
		for (FacilityMetadata metadata : facilityMetadataCollection) {
			tables.add(metadata.getTable());
		}
		return Collections.unmodifiableCollection(tables);
	}

	public Collection<FacilityMetadata> getFacilityMetadataCollection() {
		return facilityMetadataCollection.collectAll();
	}

	public FacilityMetadata getEntity(String labelOrType) {
		if (labelOrType == null || labelOrType.isEmpty()) {
			return null;
		}

		for (FacilityMetadata facilityMetadata : facilityMetadataCollection) {
			if (findEntityMetadata(labelOrType, facilityMetadata)) {
				return facilityMetadata;
			}
		}
		return null;
	}

	@Override
	public Iterator<FacilityMetadata> iterator() {
		return facilityMetadataCollection.iterator();
	}

	@Override
	public FacilityMetadata getMetadata(Class<?> clazz) {
		return facilityMetadataCollection.getMetadata(clazz);
	}

	@Override
	public FacilityMetadata getMetadata(String labelOrType) {
		return facilityMetadataCollection.getMetadata(labelOrType);
	}

	@Override
	public boolean contains(Class<?> clazz) {
		return facilityMetadataCollection.contains(clazz);
	}

	@Override
	public boolean contains(FacilityMetadata facilityMetadata) {
		return facilityMetadataCollection.contains(facilityMetadata);
	}

	@Override
	public Collection<FacilityMetadata> collectAll() {
		return facilityMetadataCollection.collectAll();
	}

	@Override
	public int size() {
		return facilityMetadataCollection.size();
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

		return Objects.equals(facilityMetadataCollection, that.facilityMetadataCollection);
	}

	@Override
	public int hashCode() {
		return facilityMetadataCollection != null ? facilityMetadataCollection.hashCode() : 0;
	}
}
