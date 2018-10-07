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
import org.ioc.orm.metadata.EntityMetadataSelector;
import org.ioc.orm.metadata.transaction.AbstractTransactional;
import org.ioc.orm.metadata.transaction.Tx;
import org.ioc.orm.metadata.type.EntityMetadata;
import org.ioc.utils.Assertion;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class EntityManager extends AbstractTransactional {
	private final DatabaseSessionFactory databaseSession;
	private final EntityMetadataSelector entityMetadataSelector;

	public EntityManager(DatabaseSessionFactory databaseSession, EntityMetadataSelector entityMetadataSelector) {
		Assertion.checkNotNull(databaseSession);
		Assertion.checkNotNull(entityMetadataSelector);

		this.databaseSession = databaseSession;
		this.entityMetadataSelector = entityMetadataSelector;
	}

	@Override
	public void close() {
		databaseSession.close();
	}

	public void clear() {
		databaseSession.clear();
	}

	public DatabaseSessionFactory getSession() {
		return databaseSession;
	}

	public <T> SchemaQuery<T> query(Class<T> clazz, String nameOrQuery) throws OrmException {
		return query(clazz, nameOrQuery, Collections.emptyMap());
	}

	@SuppressWarnings("unchecked")
	public <T> SchemaQuery<T> query(Class<T> clazz, String name, Map<String, Object> parameters) throws OrmException {
		Assertion.checkNotNull(clazz, "type");

		final EntityMetadata metadata = entityMetadataSelector.getMetadata(clazz);
		if (metadata == null) {
			return null;
		}

		return databaseSession.query(metadata, name, parameters);
	}

	public <T> boolean exists(Class<T> clazz, Object key) throws OrmException {
		Assertion.checkNotNull(clazz, "type");

		if (key == null) {
			return false;
		}

		final EntityMetadata metadata = entityMetadataSelector.getMetadata(clazz);
		if (metadata == null) {
			return false;
		}

		return databaseSession.exists(metadata, key);
	}

	@SuppressWarnings("unchecked")
	public <T> T fetch(Class<? extends T> clazz, Object key) throws OrmException {
		Assertion.checkNotNull(clazz, "type");

		if (key == null) {
			return null;
		}

		final EntityMetadata metadata = entityMetadataSelector.getMetadata(clazz);
		if (metadata == null) {
			return null;
		}

		final Object o = databaseSession.fetch(metadata, key);
		if (o != null) {
			return (T) o;
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> fetchAll(Class<? extends T> clazz) {
		Assertion.checkNotNull(clazz, "type");

		final EntityMetadata metadata = entityMetadataSelector.getMetadata(clazz);
		if (metadata == null) {
			return null;
		}

		final List<Object> result = databaseSession.fetchAll(metadata);
		if (result != null) {
			return (List<T>) result;
		}

		return null;
	}

	public <T> void delete(T element) throws OrmException {
		databaseSession.delete(findMetadata(element), element);
	}

	public <T> void save(T element) throws OrmException {
		databaseSession.save(findMetadata(element), element);
	}

	private <T> EntityMetadata findMetadata(T element) {
		Assertion.checkNotNull(element, "vertex");

		final Class<?> clazz = element.getClass();
		final EntityMetadata metadata = entityMetadataSelector.getMetadata(clazz);
		if (metadata == null) {
			throw new OrmException("Could not find metadata for element [" + element + "]. Register it.");
		}

		return metadata;
	}

	@Override
	public Tx openTx() throws OrmException {
		return databaseSession.openTx();
	}

	@Override
	public boolean pending() {
		return databaseSession.pending();
	}

	@Override
	public void start() throws OrmException {
		databaseSession.start();
	}

	@Override
	public void commit() throws OrmException {
		databaseSession.commit();
	}

	@Override
	public void rollback() throws OrmException {
		databaseSession.rollback();
	}
}
