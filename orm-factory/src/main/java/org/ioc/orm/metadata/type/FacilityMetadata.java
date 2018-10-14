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

import org.ioc.orm.generator.IdProducer;
import org.ioc.orm.generator.type.UUIDProducer;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.column.IdVisitor;
import org.ioc.orm.metadata.visitors.column.type.NullIdVisitor;
import org.ioc.utils.Assertion;
import org.ioc.utils.collections.ArrayListSet;

import java.util.*;

import static org.ioc.utils.ReflectionUtils.instantiateClass;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class FacilityMetadata implements Iterable<ColumnMetadata>, Comparable<FacilityMetadata> {
	private final String name;

	private final Set<Class<?>> types;
	private final Collection<ColumnMetadata> columnMetadataCollection = new ArrayListSet<>();
	private final List<IndexMetadata> indexMetadataList = new ArrayList<>();
	private final Map<Class<?>, InheritMetadata> inheritMetadataMap = new HashMap<>();
	private final Map<ColumnMetadata, ColumnVisitor> columnVisitorMap = new HashMap<>();
	private final Map<ColumnMetadata, IdProducer> idGeneratorMap = new HashMap<>();
	private final Collection<QueryMetadata> queryMetadataCollection = new ArrayListSet<>();

	private String table;
	private IdVisitor idVisitor = NullIdVisitor.getInstance();

	public FacilityMetadata(String name, String table, Collection<Class<?>> collection) {
		Assertion.checkNotNull(name, "name null");
		Assertion.checkNotNull(collection, "collection null");
		Assertion.checkArgument(!name.isEmpty(), "blank name");
		Assertion.checkArgument(!collection.isEmpty(), "collection empty");

		this.name = name;
		this.table = table;

		types = new HashSet<>(collection.size());
		types.addAll(collection);
	}

	public Map<ColumnMetadata, Object> filter(Map<ColumnMetadata, Object> objectMap, Object instance) {
		if (objectMap == null || objectMap.isEmpty()) {
			return Collections.emptyMap();
		}

		final Map<ColumnMetadata, Object> filtered = new HashMap<>(objectMap.size());
		if (!inheritMetadataMap.isEmpty()) {
			final InheritMetadata inheritMetadata = inheritMetadataMap.entrySet()
					.stream()
					.filter(entry -> entry.getKey().isInstance(instance))
					.findFirst()
					.map(Map.Entry::getValue)
					.orElse(null);

			if (inheritMetadata == null) {
				return Collections.emptyMap();
			}
			filtered.put(inheritMetadata.getColumn(), inheritMetadata.getValue());
		}

		objectMap.forEach((key, value) -> {
			if (hasColumn(key)) {
				filtered.put(key, value);
			}
		});

		return filtered;
	}

	public boolean validate(Map<ColumnMetadata, Object> objectMap) {
		if (objectMap == null || objectMap.isEmpty()) {
			return false;
		}

		return getPrimaryKeys()
				.stream()
				.noneMatch(key -> objectMap.get(key) == null);
	}

	public Object build(Map<ColumnMetadata, Object> data) {
		if (!validate(data)) {
			return null;
		}

		final Class<?> clazz = getType(data);
		if (clazz == null) {
			return null;
		}

		return instantiateClass(clazz);
	}

	public Class getType(Map<ColumnMetadata, Object> data) {
		if (data == null || data.isEmpty()) {
			return null;
		}

		if (types.isEmpty()) {
			return null;
		}

		if (inheritMetadataMap.isEmpty() || types.size() == 1) {
			return types.iterator().next();
		}

		for (Map.Entry<Class<?>, InheritMetadata> entry : inheritMetadataMap.entrySet()) {
			final Object discriminatorValue = entry.getValue().getValue();
			final Object dataValue = data.get(entry.getValue().getColumn());
			if (dataValue != null && dataValue.equals(discriminatorValue)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public Set<Class<?>> getTypes() {
		return Collections.unmodifiableSet(types);
	}

	public ColumnMetadata getPrimaryKey() {
		return getColumnMetadataCollection()
				.stream()
				.filter(ColumnMetadata::isPrimaryKey)
				.findFirst()
				.orElse(null);
	}

	public Set<ColumnMetadata> getPrimaryKeys() {
		final Set<ColumnMetadata> keys = new ArrayListSet<>(4);
		getColumnMetadataCollection()
				.stream()
				.filter(ColumnMetadata::isPrimaryKey)
				.forEach(keys::add);

		return Collections.unmodifiableSet(keys);
	}

	private boolean hasColumn(ColumnMetadata columnMetadata) {
		if (columnMetadata == null) {
			return false;
		}
		return columnMetadataCollection.contains(columnMetadata);
	}

	public Collection<ColumnMetadata> getColumnMetadataCollection() {
		return Collections.unmodifiableCollection(columnMetadataCollection);
	}

	@SuppressWarnings("unused")
	public int getSizeOfColumns() {
		return columnMetadataCollection.size();
	}

	@SuppressWarnings("unused")
	public boolean hasColumn(String nameOrProperty) {
		return findColumnMetadata(nameOrProperty) != null;
	}

	public ColumnMetadata findColumnMetadata(String type) {
		if (type == null || type.isEmpty()) {
			return null;
		}

		for (ColumnMetadata columnMetadata : columnMetadataCollection) {
			if (type.equalsIgnoreCase(columnMetadata.getName())) {
				return columnMetadata;
			}
			if (type.equalsIgnoreCase(columnMetadata.getProperty())) {
				return columnMetadata;
			}
		}
		return null;
	}

	public boolean addColumnMetadata(ColumnMetadata columnMetadata) {
		if (columnMetadata == null) {
			return false;
		}
		return columnMetadataCollection.add(columnMetadata);
	}

	@SuppressWarnings("unused")
	public boolean removeColumnMetadata(ColumnMetadata columnMetadata) {
		if (columnMetadata == null) {
			return false;
		}
		return columnMetadataCollection.remove(columnMetadata);
	}

	@Override
	public Iterator<ColumnMetadata> iterator() {
		return Collections.unmodifiableCollection(columnMetadataCollection).iterator();
	}

	public void addIndex(IndexMetadata indexMetadata) {
		if (indexMetadata == null) {
			return;
		}

		indexMetadataList.add(indexMetadata);
	}

	@SuppressWarnings("unused")
	public boolean removeIndex(IndexMetadata indexMetadata) {
		if (indexMetadata == null) {
			return false;
		}

		return indexMetadataList.remove(indexMetadata);
	}

	@SuppressWarnings("unused")
	public boolean isIndexColumn(ColumnMetadata columnMetadata) {
		if (columnMetadata == null) {
			return false;
		}

		return indexMetadataList.stream().anyMatch(index -> index.getMetadataList().contains(columnMetadata));
	}

	public void addQuery(QueryMetadata q) {
		queryMetadataCollection.add(q);
	}

	@SuppressWarnings("unused")
	public boolean removeQuery(QueryMetadata q) {
		return queryMetadataCollection.add(q);
	}

	public Collection<QueryMetadata> getQueryMetadataCollection() {
		return Collections.unmodifiableCollection(queryMetadataCollection);
	}

	public List<IndexMetadata> getIndexMetadataList() {
		return Collections.unmodifiableList(indexMetadataList);
	}

	public Iterable<Map.Entry<ColumnMetadata, ColumnVisitor>> getColumnVisitorMap() {
		return Collections.unmodifiableMap(columnVisitorMap).entrySet();
	}

	public ColumnVisitor getVisitor(ColumnMetadata column) {
		if (column == null) {
			return null;
		}

		return columnVisitorMap.get(column);
	}

	public void setInherited(Class<?> clazz, InheritMetadata discriminator) {
		if (clazz == null) {
			return;
		}

		if (discriminator != null) {
			inheritMetadataMap.put(clazz, discriminator);
		} else {
			inheritMetadataMap.remove(clazz);
		}
	}

	public IdProducer getProducer(ColumnMetadata column) {
		if (column == null) {
			return null;
		}

		return idGeneratorMap.get(column);
	}

	public void setProducer(ColumnMetadata column, IdProducer generator) {
		if (column == null) {
			return;
		}

		if (generator == null) {
			idGeneratorMap.remove(column);
			return;
		}

		idGeneratorMap.put(column, generator);
	}

	public void setVisitor(ColumnMetadata column, ColumnVisitor visitor) {
		if (column == null) {
			return;
		}

		if (visitor == null) {
			columnVisitorMap.remove(column);
		} else {
			columnVisitorMap.put(column, visitor);
		}
	}

	public boolean addColumns(Map<ColumnMetadata, ColumnVisitor> map) {
		if (map == null || map.isEmpty()) {
			return false;
		}

		map.forEach((column, visitor) -> {
			addColumnMetadata(column);
			if (column.isPrimaryKey() && column.getType().equals(UUID.class)) {
				setProducer(column, UUIDProducer.getInstance());
			}
			setVisitor(column, visitor);
		});

		return true;
	}

	public IdVisitor getIdVisitor() {
		return idVisitor;
	}

	public void setIdVisitor(IdVisitor idVisitor) {
		Assertion.checkNotNull(idVisitor, "id visitor null");

		this.idVisitor = idVisitor;
	}

	public String getName() {
		return name;
	}

	public String getTable() {
		return table;
	}

	@Override
	public int compareTo(FacilityMetadata facilityMetadata) {
		if (facilityMetadata == null) {
			return 1;
		}

		return name.compareToIgnoreCase(facilityMetadata.name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		FacilityMetadata that = (FacilityMetadata) o;

		if (!Objects.equals(name, that.name)) {
			return false;
		}

		if (!Objects.equals(types, that.types)) {
			return false;
		}

		return Objects.equals(table, that.table);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (types != null ? types.hashCode() : 0);
		result = 31 * result + (table != null ? table.hashCode() : 0);
		return result;
	}
}
