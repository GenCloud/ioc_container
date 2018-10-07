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
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.type.SchemaMetadata;

/**
 * Manager for instantiating entity manager and configuring schema database.
 *
 * @author GenCloud
 * @date 10/2018
 */
public class FacilityManagerFactory {
	private final Schema schema;
	private final SchemaMetadata schemaMetadata;
	private boolean configured = false;

	public FacilityManagerFactory(Schema schema, SchemaMetadata schemaMetadata) {
		this.schema = schema;
		this.schemaMetadata = schemaMetadata;
	}

	/**
	 * Create entity manager with session factory.
	 *
	 * @return entity manager instance
	 * @throws OrmException
	 */
	public FacilityManager create() throws OrmException {
		installDatabase();
		return new FacilityManager(schema.openSession(), schemaMetadata);
	}

	private synchronized void installDatabase() throws OrmException {
		if (configured) {
			return;
		}

		try {
			schema.update();
			schemaMetadata.collectAll();

			for (FacilityMetadata facilityMetadata : schemaMetadata) {
				facilityMetadata.getQueryMetadataCollection()
						.forEach(query -> schema.installQuery(facilityMetadata, query.getName(), query.getQuery()));
			}
			configured = true;
		} catch (Exception e) {
			throw new OrmException("Unable to update schema.", e);
		}
	}

	public void close() {
		schema.closeSession();
	}
}
