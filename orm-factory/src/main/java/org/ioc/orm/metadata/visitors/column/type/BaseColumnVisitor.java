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
package org.ioc.orm.metadata.visitors.column.type;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.column.FieldColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.utils.Assertion;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class BaseColumnVisitor extends FieldColumnVisitor implements ColumnVisitor {
	private final Class<?> clazz;
	private final boolean primitive;

	public BaseColumnVisitor(Field field, Class<?> clazz) {
		super(field);
		Assertion.checkNotNull(clazz, "class");

		this.clazz = clazz;

		primitive = clazz.equals(long.class) || clazz.equals(int.class) || clazz.equals(char.class)
				|| clazz.equals(short.class) || clazz.equals(boolean.class);
	}

	@Override
	public boolean initialized(Object o) {
		return true;
	}

	@Override
	public boolean empty(Object o) throws OrmException {
		final Object obj = getValue(o, null);
		if (obj == null) {
			return true;
		}

		if (obj instanceof Number) {
			return ((Number) obj).longValue() == 0;
		} else {
			return false;
		}
	}


	@Override
	public Object getValue(Object o, SessionFactory sessionFactory) throws OrmException {
		try {
			final Object obj = getValue(o);
			if (obj == null) {
				return null;
			}

			if (primitive) {
				return obj;
			} else {
				return clazz.cast(obj);
			}
		} catch (Exception e) {
			throw new OrmException("Unable to visit property [" + getRawField().getName() + "] on entityMetadata [" + o + "].", e);
		}
	}


	@Override
	public boolean setValue(Object o, DataContainer dataContainer, SessionFactory sessionFactory) throws OrmException {
		final Object value = dataContainer != null && !dataContainer.empty() ? dataContainer.of() : null;
		if (primitive && value == null) {
			return false;
		}

		try {
			return setValue(o, value);
		} catch (Exception e) {
			throw new OrmException("Unable to setValue property [" + getRawField().getName() + "] on entityMetadata [" + o + "].", e);
		}
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

		BaseColumnVisitor that = (BaseColumnVisitor) o;

		return Objects.equals(clazz, that.clazz);
	}


	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
		return result;
	}
}
