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

import org.ioc.orm.generator.IdGenerator;
import org.ioc.orm.generator.type.UUIDGenerator;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.id.IdVisitor;
import org.ioc.orm.metadata.visitors.id.type.NullIdVisitor;
import org.ioc.utils.Assertion;
import org.ioc.utils.collections.ArrayListSet;

import java.util.*;
import java.util.stream.Collectors;

import static org.ioc.utils.ReflectionUtils.instantiateClass;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class EntityMetadata implements Iterable<ColumnMetadata>, Comparable<EntityMetadata> {
	private final String name;

	private final Set<Class<?>> types;
	private final Collection<ColumnMetadata> columnMetadataCollection = new ArrayListSet<>();
	private final List<IndexMetadata> indexMetadataList = new ArrayList<>();
	private final Map<Class<?>, InheritMetadata> inheritMetadataMap = new HashMap<>();
	private final Map<ColumnMetadata, ColumnVisitor> columnVisitorMap = new HashMap<>();
	private final Map<ColumnMetadata, IdGenerator> idGeneratorMap = new HashMap<>();
	private final Collection<QueryMetadata> queryMetadataCollection = new ArrayListSet<>();

	private String table;
	private IdVisitor idVisitor = NullIdVisitor.getInstance();

	public EntityMetadata(String name, String table, Collection<Class<?>> collection) {
		Assertion.checkNotNull(name, "name null");
		Assertion.checkNotNull(collection, "collection null");
		Assertion.checkArgument(!name.isEmpty(), "blank name");
		Assertion.checkArgument(!collection.isEmpty(), "collection empty");

		this.name = name;
		this.table = table;

		types = new HashSet<>(collection.size());
		types.addAll(collection);
	}

	public Map<ColumnMetadata, Object> filter(Map<ColumnMetadata, Object> data, Object instance) {
		if (data == null || data.isEmpty()) {
			return Collections.emptyMap();
		}

		final Map<ColumnMetadata, Object> filtered = new HashMap<>(data.size());
		if (!inheritMetadataMap.isEmpty()) {
			InheritMetadata inheritMetadata = inheritMetadataMap.entrySet()
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

		data.forEach((key, value) -> {
			if (hasColumn(key)) {
				filtered.put(key, value);
			}
		});

		return filtered;
	}

	public boolean validate(Map<ColumnMetadata, Object> data) {
		if (data == null || data.isEmpty()) {
			return false;
		}

		return getPrimaryKeys()
				.stream()
				.noneMatch(key -> data.get(key) == null);
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

	public Collection<ColumnMetadata> getLazyLoaded() {
		return Collections.unmodifiableList(getColumnMetadataCollection()
				.stream()
				.filter(ColumnMetadata::isLazyLoaded)
				.collect(Collectors.toList()));
	}

	public Collection<ColumnMetadata> getEagerLoaded() {
		return Collections.unmodifiableList(getColumnMetadataCollection()
				.stream()
				.filter(column -> !column.isLazyLoaded())
				.collect(Collectors.toList()));
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

	public boolean hasColumn(ColumnMetadata column) {
		if (column == null) {
			return false;
		}
		return columnMetadataCollection.contains(column);
	}

	public Collection<ColumnMetadata> getColumnMetadataCollection() {
		return Collections.unmodifiableCollection(columnMetadataCollection);
	}

	public int getNumColumns() {
		return columnMetadataCollection.size();
	}

	public boolean hasColumn(String nameOrProperty) {
		return findColumn(nameOrProperty) != null;
	}

	public ColumnMetadata findColumn(String nameOrProperty) {
		if (nameOrProperty == null || nameOrProperty.isEmpty()) {
			return null;
		}

		for (ColumnMetadata meta : columnMetadataCollection) {
			if (nameOrProperty.equalsIgnoreCase(meta.getName())) {
				return meta;
			}
			if (nameOrProperty.equalsIgnoreCase(meta.getProperty())) {
				return meta;
			}
		}
		return null;
	}

	public boolean addColumn(ColumnMetadata column) {
		if (column == null) {
			return false;
		}
		return columnMetadataCollection.add(column);
	}

	public boolean removeColumn(ColumnMetadata column) {
		if (column == null) {
			return false;
		}
		return columnMetadataCollection.remove(column);
	}

	@Override
	public Iterator<ColumnMetadata> iterator() {
		return Collections.unmodifiableCollection(columnMetadataCollection).iterator();
	}

	public boolean addIndex(IndexMetadata column) {
		if (column == null) {
			return false;
		}
		return indexMetadataList.add(column);
	}

	public boolean removeIndexed(IndexMetadata column) {
		if (column == null) {
			return false;
		}

		return indexMetadataList.remove(column);
	}

	public boolean isIndexed(ColumnMetadata column) {
		if (column == null) {
			return false;
		}

		return indexMetadataList.stream().anyMatch(index -> index.getMetadataList().contains(column));
	}

	public boolean addQuery(QueryMetadata q) {
		return queryMetadataCollection.add(q);
	}

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

	public ColumnVisitor getVisitor(String columnName) {
		if (columnName == null || columnName.isEmpty()) {
			return null;
		}

		for (Map.Entry<ColumnMetadata, ColumnVisitor> entry : columnVisitorMap.entrySet()) {
			final ColumnMetadata column = entry.getKey();
			if (columnName.equalsIgnoreCase(column.getName())
					|| columnName.equalsIgnoreCase(column.getProperty())) {
				return entry.getValue();
			}
		}
		return null;
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

	public IdGenerator getGenerator(ColumnMetadata column) {
		if (column == null) {
			return null;
		}

		return idGeneratorMap.get(column);
	}

	public boolean setGenerator(ColumnMetadata column, IdGenerator generator) {
		if (column == null) {
			return false;
		}

		if (generator == null) {
			return idGeneratorMap.remove(column) != null;
		}

		idGeneratorMap.put(column, generator);
		return true;
	}

	public boolean setVisitor(ColumnMetadata column, ColumnVisitor visitor) {
		if (column == null) {
			return false;
		}

		if (visitor == null) {
			return columnVisitorMap.remove(column) != null;
		} else {
			columnVisitorMap.put(column, visitor);
			return true;
		}
	}

	public boolean putColumns(Map<ColumnMetadata, ColumnVisitor> map) {
		if (map == null || map.isEmpty()) {
			return false;
		}

		map.forEach((column, visitor) -> {
			addColumn(column);
			if (column.isPrimaryKey() && column.getType().equals(UUID.class)) {
				setGenerator(column, UUIDGenerator.getInstance());
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

	void setTable(String table) {
		this.table = table;
	}

	@Override
	public int compareTo(EntityMetadata entityMetadata) {
		if (entityMetadata == null) {
			return 1;
		}
		return name.compareToIgnoreCase(entityMetadata.name);
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

		EntityMetadata that = (EntityMetadata) o;

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
