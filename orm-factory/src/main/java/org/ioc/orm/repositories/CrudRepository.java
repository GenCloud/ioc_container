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
package org.ioc.orm.repositories;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author GenCloud
 * @date 10/2018
 */
public interface CrudRepository<Entity, ID> {
	/**
	 * Finds all instances of a given @{@code FacilityMetadata}-annotated class bag.
	 *
	 * @return non-empty ofList of result ofList. never <code>null</code>.
	 */
	@Transactional
	<E> E fetch(ID id);

	/**
	 * Finds all instances of a given @{@code FacilityMetadata}-annotated class bag.
	 *
	 * @return non-empty ofList of result ofList. never <code>null</code>.
	 */
	@Transactional
	<E> List<E> fetchAll();

	/**
	 * Inserts the instance to the databaseDocument as row and then binds generated id.
	 * <p>
	 * Postcondition: Instance is persistent.
	 */
	@Transactional
	void save(Entity entity);

	/**
	 * Deletes current instance from the databaseDocument and makes it transient.
	 * <p>
	 * Precondition: instance is persistent. Postcondition: instance is
	 * transient (non-persistent).
	 */
	@Transactional
	void delete(Entity entity);

	@Transactional
	boolean exists(ID id);
}
