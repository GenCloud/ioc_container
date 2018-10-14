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
package org.ioc.orm.factory.facility;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
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
public class FacilityBuilder {
	private final SessionFactory sessionFactory;

	private final DataContainerFactory dataContainerFactory;

	public FacilityBuilder(SessionFactory sessionFactory, DataContainerFactory dataContainerFactory) {
		this.sessionFactory = sessionFactory;
		this.dataContainerFactory = dataContainerFactory;
	}

	public Object build(FacilityMetadata facilityMetadata, Map<ColumnMetadata, Object> objectMap) throws OrmException {
		Assertion.checkNotNull(facilityMetadata, "entity metadata");

		if (objectMap == null || objectMap.isEmpty()) {
			return null;
		}

		final Object instance;
		try {
			instance = facilityMetadata.build(objectMap);
			if (instance == null) {
				return null;
			}

			if (!update(facilityMetadata, instance, objectMap)) {
				return null;
			}
		} catch (Exception e) {
			throw new OrmException("Unable to construct instance for facilityMetadata [" + facilityMetadata + "].", e);
		}

		final Object o = facilityMetadata.getIdVisitor().fromObject(instance);
		for (ColumnMetadata metadata : facilityMetadata) {
			if (!objectMap.containsKey(metadata)) {
				final ColumnVisitor visitor = facilityMetadata.getVisitor(metadata);
				if (visitor != null && !visitor.initialized(instance)) {
					DataContainer lazy = null;
					if (metadata instanceof JoinColumnMetadata) {
						lazy = dataContainerFactory.ofJoinColumn(facilityMetadata, (JoinColumnMetadata) metadata, o);
					} else if (metadata instanceof JoinBagMetadata) {
						lazy = dataContainerFactory.ofJoinBag(facilityMetadata, (JoinBagMetadata) metadata, o);
					} else if (metadata instanceof MappedColumnMetadata) {
						lazy = dataContainerFactory.ofMappedColumn(facilityMetadata, (MappedColumnMetadata) metadata, o);
					} else if (!metadata.isEmbedded() && !objectMap.containsKey(metadata)) {
						lazy = dataContainerFactory.ofLazy(facilityMetadata, metadata, o);
					}

					if (lazy != null) {
						visitor.setValue(instance, lazy, sessionFactory);
					}
				}
			}
		}
		return instance;
	}

	public boolean update(FacilityMetadata facilityMetadata, Object instance,
						  final Map<ColumnMetadata, Object> objectMap) throws OrmException {
		if (objectMap == null || objectMap.isEmpty()) {
			return false;
		}

		boolean updated = false;
		for (Map.Entry<ColumnMetadata, Object> entry : objectMap.entrySet()) {
			final ColumnMetadata column = entry.getKey();
			final Object value = entry.getValue();
			final ColumnVisitor visitor = facilityMetadata.getVisitor(column);
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