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
import org.ioc.orm.generator.IdGenerator;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.EntityMetadata;
import org.ioc.orm.metadata.type.MappedColumnMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.orm.metadata.visitors.container.type.BaseDataContainer;
import org.ioc.orm.metadata.visitors.handler.EntityAdder;
import org.ioc.utils.Assertion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class EntityPersistence {
	private final SessionFactory sessionFactory;

	public EntityPersistence(SessionFactory sessionFactory) {
		Assertion.checkNotNull(sessionFactory, "session");

		this.sessionFactory = sessionFactory;
	}

	public void save(EntityMetadata entityMetadata, Object instance, EntityAdder entityAdder) throws OrmException {
		entityMetadata.getColumnMetadataCollection().forEach(column -> {
			final ColumnVisitor visitor = entityMetadata.getVisitor(column);
			final IdGenerator generator = entityMetadata.getGenerator(column);
			if (generator != null && visitor != null && visitor.empty(instance)) {
				final Object generated = generator.create(sessionFactory, entityMetadata);
				final DataContainer container = new BaseDataContainer(generated);
				visitor.setValue(instance, container, sessionFactory);
			}
		});

		final Map<ColumnMetadata, Object> data = mapData(entityMetadata, instance);
		final Map<ColumnMetadata, Object> filtered = entityMetadata.filter(data, instance);
		if (!entityAdder.add(entityMetadata, filtered)) {
			throw new OrmException("Unable to add instance, unknown error.");
		}
	}

	private Map<ColumnMetadata, Object> mapData(EntityMetadata entityMetadata, Object instance) throws OrmException {
		final Map<ColumnMetadata, Object> objectMap = new LinkedHashMap<>();
		for (ColumnMetadata column : entityMetadata) {
			if (!(column instanceof MappedColumnMetadata)) {
				final ColumnVisitor visitor = entityMetadata.getVisitor(column);
				if (visitor != null && visitor.initialized(instance)) {
					final Object value = visitor.getValue(instance, sessionFactory);
					if (value != null) {
						objectMap.put(column, value);
					}
				}
			}
		}
		return objectMap;
	}
}
