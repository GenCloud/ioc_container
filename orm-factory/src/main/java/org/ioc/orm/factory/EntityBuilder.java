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
package org.ioc.orm.factory;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.metadata.type.*;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.orm.metadata.visitors.container.DataContainerFactory;
import org.ioc.utils.Assertion;

import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class EntityBuilder {
	private final SessionFactory sessionFactory;

	private final DataContainerFactory dataContainerFactory;

	public EntityBuilder(SessionFactory sessionFactory, DataContainerFactory dataContainerFactory) {
		this.sessionFactory = sessionFactory;
		this.dataContainerFactory = dataContainerFactory;
	}

	public Object build(EntityMetadata entityMetadata, Map<ColumnMetadata, Object> objectMap) throws OrmException {
		Assertion.checkNotNull(entityMetadata, "entity metadata");

		if (objectMap == null || objectMap.isEmpty()) {
			return null;
		}

		final Object instance;
		try {
			instance = entityMetadata.build(objectMap);
			if (instance == null) {
				return null;
			}

			if (!update(entityMetadata, instance, objectMap)) {
				return null;
			}
		} catch (Exception e) {
			throw new OrmException("Unable to construct instance for entityMetadata [" + entityMetadata + "].", e);
		}

		final Object o = entityMetadata.getIdVisitor().fromObject(instance);
		for (ColumnMetadata metadata : entityMetadata) {
			if (!objectMap.containsKey(metadata)) {
				final ColumnVisitor visitor = entityMetadata.getVisitor(metadata);
				if (visitor != null && !visitor.initialized(instance)) {
					DataContainer lazy = null;
					if (metadata instanceof JoinColumnMetadata) {
						lazy = dataContainerFactory.ofJoinColumn(entityMetadata, (JoinColumnMetadata) metadata, o);
					} else if (metadata instanceof JoinBagMetadata) {
						lazy = dataContainerFactory.ofJoinBag(entityMetadata, (JoinBagMetadata) metadata, o);
					} else if (metadata instanceof MappedColumnMetadata) {
						lazy = dataContainerFactory.ofMappedColumn(entityMetadata, (MappedColumnMetadata) metadata, o);
					} else if (!metadata.isEmbedded() && !objectMap.containsKey(metadata)) {
						lazy = dataContainerFactory.ofLazy(entityMetadata, metadata, o);
					}

					if (lazy != null) {
						visitor.setValue(instance, lazy, sessionFactory);
					}
				}
			}
		}
		return instance;
	}

	public boolean update(EntityMetadata entityMetadata, Object instance,
						  final Map<ColumnMetadata, Object> objectMap) throws OrmException {
		if (objectMap == null || objectMap.isEmpty()) {
			return false;
		}

		boolean updated = false;
		for (Map.Entry<ColumnMetadata, Object> entry : objectMap.entrySet()) {
			final ColumnMetadata column = entry.getKey();
			final Object value = entry.getValue();
			final ColumnVisitor visitor = entityMetadata.getVisitor(column);
			if (value != null && visitor != null) {
				final DataContainer placeholder = dataContainerFactory.createStatic(value);
				if (placeholder != null) {
					visitor.setValue(instance, placeholder, sessionFactory);
					updated = true;
				}
			}
		}
		return updated;
	}
}