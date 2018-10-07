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
package org.ioc.orm.metadata.visitors.column.type;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.column.FieldColumnVisitor;
import org.ioc.orm.metadata.visitors.column.IdVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.orm.metadata.visitors.container.type.BaseDataContainer;
import org.ioc.utils.Assertion;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.ioc.utils.ReflectionUtils.instantiateClass;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class CompoundIdVisitor extends FieldColumnVisitor implements IdVisitor {
	private final Map<ColumnMetadata, ColumnVisitor> visitorMap;

	public CompoundIdVisitor(Field field, Map<ColumnMetadata, ColumnVisitor> visitorMap) {
		super(field);
		Assertion.checkNotNull(visitorMap, "map");
		Assertion.checkArgument(!visitorMap.isEmpty(), "Map cannot be empty.");

		this.visitorMap = new LinkedHashMap<>(visitorMap);
	}

	@Override
	public Object fromObject(Object entity) {
		if (entity == null) {
			return null;
		}

		try {
			return getValue(entity);
		} catch (Exception e) {
			throw new OrmException("Unable to visit field [" + getRawField().getName() + "] from entityMetadata [" + entity + "].", e);
		}
	}

	@Override
	public Map<ColumnMetadata, Object> fromKey(Object key) {
		if (key == null) {
			return Collections.emptyMap();
		}

		if (!getRawField().getType().isInstance(key)) {
			return Collections.emptyMap();
		}

		final Map<ColumnMetadata, Object> metadataObjectMap = new LinkedHashMap<>(visitorMap.size());
		visitorMap.forEach((columnMetadata, columnVisitor) -> {
			try {
				final Object value = columnVisitor.getValue(key, null);
				if (value != null) {
					metadataObjectMap.put(columnMetadata, value);
				}
			} catch (Exception e) {
				throw new OrmException("Unable to ofObject key properties from [" + key + "].", e);
			}
		});
		return Collections.unmodifiableMap(metadataObjectMap);
	}

	@Override
	public Object ofKey(Map<ColumnMetadata, Object> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}
		return extractData(map);
	}

	private void addValue(Map<ColumnMetadata, Object> objectMap, Object instance) throws OrmException {
		visitorMap.forEach((columnMetadata, columnVisitor) -> {
			final Object value = objectMap.get(columnMetadata);
			final DataContainer container = new BaseDataContainer(value);
			columnVisitor.setValue(instance, container, null);
		});
	}

	private Object extractData(Map<ColumnMetadata, Object> objectMap) {
		try {
			final Object instance = instantiateClass(getRawField().getType());
			addValue(objectMap, instance);
			return instance;
		} catch (Exception e) {
			throw new OrmException("Unable to instantiate composite key from field [" + getRawField() + "].", e);
		}
	}
}
