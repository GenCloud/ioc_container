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
package org.ioc.orm.persistance;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.DatabaseSessionFactory;
import org.ioc.orm.factory.FacilityBuilder;
import org.ioc.orm.factory.FacilityMapper;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.container.DataContainerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class DatabasePersistentModel implements PersistentModel {
	private final DatabaseSessionFactory databaseSessionFactory;
	private final DataContainerFactory dataContainerFactory;
	private final FacilityMetadata facilityMetadata;
	private final Object key;

	private Object instance;

	private boolean ownTransaction = false;

	public DatabasePersistentModel(DatabaseSessionFactory databaseSessionFactory,
								   final DataContainerFactory dataContainerFactory,
								   final FacilityMetadata facilityMetadata, Object key) {
		this.databaseSessionFactory = databaseSessionFactory;
		this.dataContainerFactory = dataContainerFactory;
		this.facilityMetadata = facilityMetadata;
		this.key = key;
	}

	@Override
	public Map<ColumnMetadata, Object> get() throws OrmException {
		final MemoryPersistentModel persistentModel = new MemoryPersistentModel();
		new FacilityMapper(databaseSessionFactory).save(facilityMetadata, ensureInstance(), persistentModel);
		return persistentModel.get();
	}

	@Override
	public void add(Map<ColumnMetadata, Object> objectMap) throws OrmException {
		if (!databaseSessionFactory.pending()) {
			ownTransaction = true;
			databaseSessionFactory.start();
		}

		new FacilityBuilder(databaseSessionFactory, dataContainerFactory).update(facilityMetadata, ensureInstance(), objectMap);
	}

	private Object ensureInstance() {
		if (instance != null) {
			return instance;
		}
		instance = databaseSessionFactory.fetch(facilityMetadata, key);
		return instance;
	}


	@Override
	public void close() throws IOException {
		if (ownTransaction) {
			try {
				databaseSessionFactory.commit();
				ownTransaction = false;
			} catch (OrmException e) {
				throw new IOException("Unable to commit transaction.", e);
			}
		}
		databaseSessionFactory.close();
	}
}
