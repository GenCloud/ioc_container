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
package org.ioc.orm.metadata.visitors.id.type;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.visitors.column.FieldColumnVisitor;
import org.ioc.orm.metadata.visitors.id.IdVisitor;
import org.ioc.utils.Assertion;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class BaseIdVisitor extends FieldColumnVisitor implements IdVisitor {
	private final ColumnMetadata columnMetadata;

	public BaseIdVisitor(Field field, ColumnMetadata columnMetadata) {
		super(field);
		Assertion.checkNotNull(columnMetadata, "column metadata");

		this.columnMetadata = columnMetadata;
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

		final Map<ColumnMetadata, Object> map = new HashMap<>(1);
		map.put(columnMetadata, key);
		return Collections.unmodifiableMap(map);
	}

	@Override
	public Object fromMap(Map<ColumnMetadata, Object> data) {
		if (data == null || data.isEmpty()) {
			return null;
		}
		return data.get(columnMetadata);
	}

	@Override
	public Object ofKey(Map<ColumnMetadata, Object> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}
		return map.get(columnMetadata);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		BaseIdVisitor that = (BaseIdVisitor) o;

		return Objects.equals(columnMetadata, that.columnMetadata);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (columnMetadata != null ? columnMetadata.hashCode() : 0);
		return result;
	}
}
