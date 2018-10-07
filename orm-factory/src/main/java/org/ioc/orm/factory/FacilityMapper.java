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
import org.ioc.orm.generator.IdProducer;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.type.MappedColumnMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.orm.metadata.visitors.container.type.BaseDataContainer;
import org.ioc.orm.metadata.visitors.handler.FacilityAdder;
import org.ioc.utils.Assertion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class FacilityMapper {
	private final SessionFactory sessionFactory;

	public FacilityMapper(SessionFactory sessionFactory) {
		Assertion.checkNotNull(sessionFactory);

		this.sessionFactory = sessionFactory;
	}

	/**
	 * Identify and generate primary key, save entity with new PK to database.
	 *
	 * @param facilityMetadata meta data of entity
	 * @param instance         entity initialized instance
	 * @param facilityAdder    utility class for save entity instance
	 * @throws OrmException if entity not iserted to database
	 */
	public void save(FacilityMetadata facilityMetadata, Object instance, FacilityAdder facilityAdder) throws OrmException {
		facilityMetadata.getColumnMetadataCollection().forEach(column -> {
			final ColumnVisitor visitor = facilityMetadata.getVisitor(column);
			final IdProducer generator = facilityMetadata.getProducer(column);
			if (generator != null && visitor != null && visitor.empty(instance)) {
				final Object generated = generator.create(sessionFactory, facilityMetadata);
				final DataContainer container = new BaseDataContainer(generated);
				visitor.setValue(instance, container, sessionFactory);
			}
		});

		final Map<ColumnMetadata, Object> data = mapData(facilityMetadata, instance);
		final Map<ColumnMetadata, Object> filtered = facilityMetadata.filter(data, instance);
		if (!facilityAdder.add(facilityMetadata, filtered)) {
			throw new OrmException("Unable to add instance, unknown error.");
		}
	}


	private Map<ColumnMetadata, Object> mapData(FacilityMetadata facilityMetadata, Object instance) throws OrmException {
		final Map<ColumnMetadata, Object> objectMap = new LinkedHashMap<>();
		for (ColumnMetadata column : facilityMetadata) {
			if (!(column instanceof MappedColumnMetadata)) {
				final ColumnVisitor visitor = facilityMetadata.getVisitor(column);
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
