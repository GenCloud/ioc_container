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
 * Head interface for control database.
 *
 * @author GenCloud
 * @date 10/2018
 */
public interface Schema {
	/**
	 * @return return generated schema meta data
	 */
	SchemaMetadata getMetadata();

	/**
	 * @return open new schema session
	 */
	DatabaseSessionFactory openSession();

	/**
	 * Refresh database schema.
	 *
	 * @throws OrmException
	 */
	void update() throws OrmException;

	/**
	 * Function for adding named queries (functions) to database schema.
	 *
	 * @param facilityMetadata entity meta data
	 * @param name             name of query
	 * @param query            named query
	 */
	void installQuery(FacilityMetadata facilityMetadata, String name, String query);

	/**
	 * Function for remove named queries (functions) from database schema.
	 *
	 * @param name name of query
	 * @return true if query is removed.
	 */
	boolean uninstallQuery(String name);

	/**
	 * Close schema database session.
	 */
	void closeSession();
}
