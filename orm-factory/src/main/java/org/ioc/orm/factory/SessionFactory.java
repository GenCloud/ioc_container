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
package org.ioc.orm.factory;

import org.ioc.orm.metadata.type.FacilityMetadata;

import java.util.List;

/**
 * Session container for retrieving database/cache entities.
 *
 * @author GenCloud
 * @date 10/2018
 */
public interface SessionFactory {
	/**
	 * Check if entity with key exists in database.
	 *
	 * @param facilityMetadata entity meta data
	 * @param key              entity primary key
	 * @return true if entity exists
	 */
	boolean exists(FacilityMetadata facilityMetadata, Object key);

	/**
	 * Fetching entity from database by primary key.
	 *
	 * @param facilityMetadata entity meta data.
	 * @param key              entity primary key
	 * @return entity instance if exists
	 */
	Object fetch(FacilityMetadata facilityMetadata, Object key);

	/**
	 * Fetching entities from database by primary key's.
	 *
	 * @param facilityMetadata entity meta data.
	 * @param keys             collection entity primary key
	 * @return collection of entity instances if exists
	 */
	List<Object> fetch(FacilityMetadata facilityMetadata, Object... keys);

	/**
	 * Fetching all entities from database.
	 *
	 * @param facilityMetadata entity meta data.
	 * @return collection of entity
	 */
	List<Object> fetchAll(FacilityMetadata facilityMetadata);
}
