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
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.column.FieldColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.utils.Assertion;

import java.lang.reflect.Field;
import java.util.Objects;

import static org.ioc.utils.ReflectionUtils.instantiateClass;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class NestedColumnVisitor extends FieldColumnVisitor implements ColumnVisitor {
	private final ColumnVisitor delegateColumnVisitor;
	private final Class<?> type;

	public NestedColumnVisitor(Field field, ColumnVisitor delegateColumnVisitor) {
		super(field);
		Assertion.checkNotNull(delegateColumnVisitor, "visitor");

		this.delegateColumnVisitor = delegateColumnVisitor;

		type = field.getType();
	}

	@Override
	public boolean initialized(Object o) {
		return true;
	}

	@Override
	public boolean empty(Object o) throws OrmException {
		return getValue(o, null) == null;
	}

	@Override
	public Object getValue(Object o, SessionFactory sessionFactory) throws OrmException {
		final Object base;
		try {
			base = getValue(o);
		} catch (Exception e) {
			throw new OrmException("Unable to visit base value for nested property [" + getRawField().getName() + "].", e);
		}

		if (base == null) {
			return null;
		}
		return delegateColumnVisitor.getValue(base, sessionFactory);
	}

	@Override
	public boolean setValue(Object o, DataContainer dataContainer, SessionFactory sessionFactory) throws OrmException {
		Object base;
		try {
			base = getValue(o);
		} catch (Exception e) {
			throw new OrmException("Unable to visit base value for nested property [" + getRawField().getName() + "].", e);
		}

		if (base == null) {
			if (dataContainer.empty()) {
				return false;
			}

			try {
				base = instantiateClass(type);
				setValue(o, base);
			} catch (Exception e) {
				throw new OrmException("Unable to instantiate new instance of nested/embedded bag [" + type + "] for property [" + getRawField().getName() + "].", e);
			}
		}

		return delegateColumnVisitor.setValue(base, dataContainer, sessionFactory);
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

		NestedColumnVisitor that = (NestedColumnVisitor) o;

		if (!Objects.equals(delegateColumnVisitor, that.delegateColumnVisitor)) {
			return false;
		}

		return Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (delegateColumnVisitor != null ? delegateColumnVisitor.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}
}
