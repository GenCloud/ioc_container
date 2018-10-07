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
package org.ioc.orm.metadata.visitors.container.type;

import org.ioc.orm.factory.orient.OrientDocumentConverter;
import org.ioc.orm.factory.orient.session.OrientDatabaseSession;
import org.ioc.orm.metadata.type.*;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.orm.metadata.visitors.container.DataContainerFactory;
import org.ioc.orm.util.OrientUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.ioc.orm.util.OrientUtils.convertValue;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientContainerFactory implements DataContainerFactory {
	private final OrientDatabaseSession databaseSession;

	public OrientContainerFactory(OrientDatabaseSession databaseSession) {
		this.databaseSession = databaseSession;
	}

	@Override
	public DataContainer createStatic(Object value) {
		return new BaseDataContainer(value);
	}

	@Override
	public DataContainer ofLazy(FacilityMetadata entity, ColumnMetadata column, Object key) {
		if (entity == null || column == null || key == null) {
			return null;
		}
		return new OrientLazyDataContainer(databaseSession, entity, column, key);
	}

	@Override
	public DataContainer ofJoinBag(FacilityMetadata entity, JoinBagMetadata column, Object key) {
		if (entity == null || column == null || key == null) {
			return null;
		}

		final Map<ColumnMetadata, Object> keymap = entity.getIdVisitor().fromKey(key);
		if (keymap == null || keymap.isEmpty()) {
			return null;
		}

		final List<Object> parameters = new ArrayList<>(keymap.size());
		final StringBuilder query = new StringBuilder();
		query.append("select ").append(column.getName()).append(" from ").append(entity.getTable()).append(" where ");
		boolean first = true;
		for (Map.Entry<ColumnMetadata, Object> entry : keymap.entrySet()) {
			if (!first) {
				query.append(" and ");
			}

			final String columnName = entry.getKey().getName();
			final Object value = entry.getValue();
			query.append(columnName).append(" = ?");
			first = false;
			parameters.add(value);
		}
		final OrientDocumentConverter handler = document -> convertValue(document, column);
		return new OrientLazyQueryContainer(databaseSession, entity, column.isBag(), query.toString(), parameters, handler);
	}

	@Override
	public DataContainer ofJoinColumn(FacilityMetadata entity, JoinColumnMetadata column, Object key) {
		if (entity == null || column == null || key == null) {
			return null;
		}

		final Map<ColumnMetadata, Object> keymap = entity.getIdVisitor().fromKey(key);
		if (keymap == null || keymap.isEmpty()) {
			return null;
		}

		final List<Object> parameters = new ArrayList<>(keymap.size());
		final StringBuilder query = new StringBuilder();
		query.append("select from ").append(entity.getTable()).append(" where ");
		boolean first = true;
		for (Map.Entry<ColumnMetadata, Object> entry : keymap.entrySet()) {
			if (!first) {
				query.append(" and ");
			}
			final String columnName = entry.getKey().getName();
			final Object value = entry.getValue();
			query.append(columnName).append(" = ?");
			first = false;
			parameters.add(value);
		}
		final OrientDocumentConverter handler = document -> OrientUtils.convertKey(entity, document);
		return new OrientLazyQueryContainer(databaseSession, entity, column.isBag(), query.toString(), parameters, handler);
	}

	@Override
	public DataContainer ofMappedColumn(FacilityMetadata entity, MappedColumnMetadata column, Object key) {
		if (entity == null || column == null || key == null) {
			return null;
		}

		final FacilityMetadata mappedEntity = column.getFacilityMetadata();
		final ColumnMetadata mappedColumn = column.getColumnMetadata();

		final String query = "select from " + mappedEntity.getTable() + " " +
				"where " + mappedColumn.getName() + " = ?";
		final OrientDocumentConverter handler = document -> OrientUtils.convertKey(mappedEntity, document);
		final Object rid = databaseSession.findIdentifyByKey(entity, key);
		return new OrientLazyQueryContainer(databaseSession, mappedEntity, column.isBag(), query, Collections.singletonList(rid), handler);
	}
}
