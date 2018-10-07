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
import org.ioc.orm.metadata.transaction.ITransactional;
import org.ioc.orm.metadata.type.FacilityMetadata;

import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public interface DatabaseSessionFactory extends SessionFactory, ITransactional {
	void close();

	void clear();

	void save(FacilityMetadata facilityMetadata, Object o) throws OrmException;

	void delete(FacilityMetadata facilityMetadata, Object o) throws OrmException;

	SchemaQuery query(FacilityMetadata facilityMetadata, String query, Map<String, Object> params) throws OrmException;
}
