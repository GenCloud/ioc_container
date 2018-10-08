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
package org.ioc.orm.repositories;

import org.ioc.orm.factory.SchemaQuery;
import org.ioc.orm.factory.facility.FacilityManager;
import org.ioc.utils.Assertion;

import java.util.List;
import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SampleRepository<Entity, ID> implements ProxyRepository<Entity, ID> {
	private final FacilityManager facilityManager;
	private final Class<Entity> entityClass;

	public SampleRepository(FacilityManager facilityManager, Class<Entity> entityClass) {
		Assertion.checkNotNull(facilityManager);
		Assertion.checkNotNull(entityClass);

		this.facilityManager = facilityManager;
		this.entityClass = entityClass;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> E fetch(ID id) {
		return (E) facilityManager.fetch(entityClass, id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> List<E> fetchAll() {
		return (List<E>) facilityManager.fetchAll(entityClass);
	}

	@Override
	public void save(Entity entity) {
		facilityManager.save(entity);
	}

	@Override
	public void delete(Entity entity) {
		facilityManager.delete(entity);
	}

	@Override
	public boolean exists(ID id) {
		return facilityManager.exists(entityClass, id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SchemaQuery<Entity> executePreparedQuery(String prepared, Map<String, Object> params) {
		return facilityManager.query(entityClass, prepared, params);
	}

	@Override
	public SchemaQuery<Entity> executePreparedQueryWithoutParams(String prepared) {
		return facilityManager.query(entityClass, prepared);
	}
}
