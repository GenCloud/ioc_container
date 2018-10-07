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
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.column.FieldColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.orm.util.RelationsUtils;

import java.lang.reflect.Field;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SingleJoinColumnVisitor extends FieldColumnVisitor implements ColumnVisitor {
	private final BagMapper bagMapper;
	private final FacilityMetadata facilityMetadata;
	private final boolean isLazyLoading;

	public SingleJoinColumnVisitor(Field field, FacilityMetadata facilityMetadata, boolean isLazyLoading,
								   final BagMapper bagMapper) {
		super(field);
		this.facilityMetadata = facilityMetadata;
		this.isLazyLoading = isLazyLoading;
		this.bagMapper = bagMapper;
	}

	@Override
	public boolean empty(Object o) throws OrmException {
		try {
			return getValue(o) == null;
		} catch (Exception e) {
			throw new OrmException("Unable to visit join-column.", e);
		}
	}

	@Override
	public boolean initialized(Object o) throws OrmException {
		try {
			final Object relation = getValue(o);
			if (relation == null) {
				return true;
			}

			if (!RelationsUtils.isNotProxy(relation)) {
				return true;
			}

			return RelationsUtils.isInitialized(relation);
		} catch (Exception e) {
			throw new OrmException("Unable to determine if facilityMetadata/proxy column [" + getRawField().getName() + "] is loaded.", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getValue(Object o, SessionFactory sessionFactory) throws OrmException {
		final Object relatedEntity;
		try {
			relatedEntity = getValue(o);
		} catch (Exception e) {
			throw new OrmException("Unable to visit join-column [" + getRawField().getName() + "].", e);
		}
		if (relatedEntity == null) {
			return null;
		}
		return bagMapper.formObject(sessionFactory, relatedEntity);
	}

	@Override
	public boolean setValue(Object o, DataContainer dataContainer, SessionFactory sessionFactory) throws OrmException {
		if (dataContainer == null || dataContainer.empty()) {
			try {
				setValue(o, null);
				return true;
			} catch (Exception e) {
				throw new OrmException("Unable to setValue join-column [" + getRawField().getName() + "] to empty/null value.", e);
			}
		} else {
			final Object key = dataContainer.of();
			try {
				final Object related;
				if (isLazyLoading) {
					related = RelationsUtils.createProxy(facilityMetadata, key, sessionFactory, bagMapper);
				} else {
					related = bagMapper.ofObject(sessionFactory, key);
				}
				setValue(o, related);
				return true;
			} catch (Exception e) {
				throw new OrmException("Unable to setValue join-column [" + getRawField().getName() + "] from dataContainer [" + dataContainer.getClass().getSimpleName() + "].", e);
			}
		}
	}
}