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
package org.ioc.orm.metadata.visitors.column;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.metadata.relation.LazyBag;
import org.ioc.utils.Assertion;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public abstract class BagColumnVisitor extends FieldColumnVisitor implements ColumnVisitor {
	private final Class<?> clazz;
	private final boolean lazyLoading;

	public BagColumnVisitor(Field field, Class<?> clazz, boolean lazyLoading) {
		super(field);
		Assertion.checkNotNull(clazz, "class");

		this.clazz = clazz;
		this.lazyLoading = lazyLoading;
	}

	protected boolean isLazyLoading() {
		return lazyLoading;
	}

	@Override
	public boolean initialized(Object o) throws OrmException {
		try {
			final Collection<?> collection = getBag(o);

			if (collection instanceof LazyBag) {
				return ((LazyBag) collection).isInitialized();
			} else {
				return !collection.isEmpty();
			}
		} catch (Exception e) {
			throw new OrmException("Unable to determine if many-join visitor is loaded.", e);
		}
	}

	@Override
	public boolean empty(Object o) throws OrmException {
		return getBag(o).isEmpty();
	}

	protected boolean setBag(Object entity, Collection<?> collection) throws OrmException {
		if (entity == null) {
			return false;
		}

		try {
			return setValue(entity, collection);
		} catch (Exception e) {
			throw new OrmException("Unable to visit property [" + getRawField().getName() + "] from entityMetadata [" + entity + "].", e);
		}
	}

	@SuppressWarnings("unchecked")
	protected Collection<?> getBag(Object entity) throws OrmException {
		if (entity == null) {
			return Collections.emptyList();
		}
		try {
			final Object obj = getValue(entity);
			if (obj == null) {
				return Collections.emptyList();
			}

			if (obj instanceof Collection) {
				return new ArrayList<>((Collection) obj);
			} else {
				throw new OrmException("Expected value of bag collection, found [" + obj + "].");
			}
		} catch (Exception e) {
			throw new OrmException("Unable to visit property [" + getRawField().getName() + "] from entityMetadata [" + entity + "].", e);
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

		BagColumnVisitor that = (BagColumnVisitor) o;

		return Objects.equals(clazz, that.clazz);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
		return result;
	}
}
