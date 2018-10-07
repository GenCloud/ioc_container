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
import org.ioc.orm.metadata.relation.BagMapper;
import org.ioc.orm.metadata.relation.LazyBag;
import org.ioc.orm.metadata.relation.bag.LazyFacilityListBag;
import org.ioc.orm.metadata.relation.bag.LazyFacilitySetBag;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.column.FieldColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.utils.collections.ArrayListSet;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class ManyJoinColumnVisitor extends FieldColumnVisitor implements ColumnVisitor {
	private final BagMapper bagMapper;
	private final FacilityMetadata facilityMetadata;
	private final boolean lazy;

	public ManyJoinColumnVisitor(Field field, FacilityMetadata facilityMetadata, boolean lazy,
								 final BagMapper bagMapper) {
		super(field);
		this.bagMapper = bagMapper;
		this.facilityMetadata = facilityMetadata;
		this.lazy = lazy;
	}

	public FacilityMetadata getMetadata() {
		return facilityMetadata;
	}

	public boolean isLazy() {
		return lazy;
	}

	@Override
	public boolean initialized(Object o) throws OrmException {
		if (o == null) {
			return false;
		}
		try {
			final Object value = getValue(o);
			if (value == null) {
				return false;
			}

			if (!(value instanceof Collection)) {
				return false;
			}

			if (value instanceof LazyBag) {
				return ((LazyBag) value).isInitialized();
			} else {
				return true;
			}
		} catch (Exception e) {
			throw new OrmException("Unable to determine if many-join visitor is loaded.", e);
		}
	}

	private Collection<?> getBag(Object entity) throws IllegalAccessException {
		final Object obj = getValue(entity);
		if (obj == null) {
			return Collections.emptyList();
		}

		if (obj instanceof Collection) {
			return (Collection) obj;
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public boolean empty(Object o) throws OrmException {
		try {
			return getBag(o).isEmpty();
		} catch (Exception e) {
			throw new OrmException("Unable to query join-column collection.", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection getValue(Object o, SessionFactory sessionFactory) throws OrmException {
		try {
			final Collection objects = getBag(o);
			if (objects instanceof LazyBag) {
				return ((LazyBag) objects).copy();
			}

			if (objects.isEmpty()) {
				return Collections.emptyList();
			}

			final List<?> keys = bagMapper.formObjects(sessionFactory, getBag(o).toArray());
			if (Set.class.isAssignableFrom(getRawField().getType())) {
				return Collections.unmodifiableSet(new ArrayListSet<>(keys));
			} else {
				return Collections.unmodifiableList(keys);
			}
		} catch (Exception e) {
			throw new OrmException("Unable to visit join-column collection.", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean setValue(Object o, DataContainer dataContainer, SessionFactory sessionFactory) throws OrmException {
		if (lazy) {
			try {
				if (Set.class.isAssignableFrom(getRawField().getType())) {
					return setValue(o, new LazyFacilitySetBag(sessionFactory, facilityMetadata, dataContainer, bagMapper));
				} else {
					return setValue(o, new LazyFacilityListBag(sessionFactory, facilityMetadata, dataContainer, bagMapper));
				}
			} catch (Exception e) {
				throw new OrmException("Unable to setValue lazy join-column collection from dataContainer holder [" + dataContainer + "].", e);
			}
		} else {
			final Object of = dataContainer.of();
			if (!(of instanceof Collection)) {
				throw new OrmException("Expect collection of primary key values for [" + getRawField() + "] but found [" + of + "].");
			}
			try {
				final Collection keys = (Collection) of;
				if (keys.isEmpty()) {
					setValue(o, new ArrayListSet());
				} else {
					final List<Object> relations = sessionFactory.fetch(facilityMetadata, keys.toArray());
					if (relations == null || relations.isEmpty()) {
						setValue(o, new ArrayListSet());
					} else {
						setValue(o, new ArrayListSet<>(relations));
					}
				}
				return true;
			} catch (Exception e) {
				throw new OrmException("Unable to setValue join-column collection from value [" + of + "].", e);
			}
		}
	}
}
