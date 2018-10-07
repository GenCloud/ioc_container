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

import org.ioc.orm.factory.EntityManager;
import org.ioc.orm.factory.SchemaQuery;
import org.ioc.utils.Assertion;

import java.util.List;
import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SampleRepository<Entity, ID> implements ProxyRepository<Entity, ID> {
	private final EntityManager entityManager;
	private final Class<Entity> entityClass;

	public SampleRepository(EntityManager entityManager, Class<Entity> entityClass) {
		Assertion.checkNotNull(entityManager);
		Assertion.checkNotNull(entityClass);

		this.entityManager = entityManager;
		this.entityClass = entityClass;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> E fetch(ID id) {
		return (E) entityManager.fetch(entityClass, id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> List<E> fetchAll() {
		return (List<E>) entityManager.fetchAll(entityClass);
	}

	@Override
	public void save(Entity entity) {
		entityManager.save(entity);
	}

	@Override
	public void delete(Entity entity) {
		entityManager.delete(entity);
	}

	@Override
	public boolean exists(ID id) {
		return entityManager.exists(entityClass, id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SchemaQuery<Entity> executePreparedQuery(String prepared, Map<String, Object> params) {
		return entityManager.query(entityClass, prepared, params);
	}

	@Override
	public SchemaQuery<Entity> executePreparedQueryWithoutParams(String prepared) {
		return entityManager.query(entityClass, prepared);
	}
}
