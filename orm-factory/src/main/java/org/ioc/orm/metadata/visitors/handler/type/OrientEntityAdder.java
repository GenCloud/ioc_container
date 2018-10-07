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
package org.ioc.orm.metadata.visitors.handler.type;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.ioc.orm.factory.orient.session.OrientDatabaseSession;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.EntityMetadata;
import org.ioc.orm.metadata.visitors.handler.EntityAdder;
import org.ioc.orm.util.OrientUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientEntityAdder implements EntityAdder {
	private final OrientDatabaseSession databaseSession;

	public OrientEntityAdder(OrientDatabaseSession databaseSession) {
		this.databaseSession = databaseSession;
	}

	@Override
	public boolean add(EntityMetadata entity, Map<ColumnMetadata, Object> data) {
		final Map<ColumnMetadata, Object> keymap = new LinkedHashMap<>();
		entity.getPrimaryKeys().forEach(column -> {
			final Object value = data.get(column);
			if (value != null) {
				keymap.put(column, value);
			}
		});

		ODocument document = null;
		final OIdentifiable existing = databaseSession.findIdentifyByMap(entity, keymap);
		if (existing != null) {
			document = databaseSession.findDocument(existing);
		} else if (!keymap.isEmpty()) {
			final String schemaName = entity.getTable();
			document = databaseSession.getDocument().newInstance(schemaName);
		}

		if (document == null) {
			return false;
		}

		if (data.isEmpty() || keymap.isEmpty()) {
			document.delete();
			return true;
		}

		for (Map.Entry<ColumnMetadata, Object> entry : data.entrySet()) {
			final ColumnMetadata column = entry.getKey();
			final String name = column.getName();
			final Object value = entry.getValue();
			if (value != null) {
				final OType type = OrientUtils.columnType(column);
				final Object packed = OrientUtils.convertValue(column, value);
				document.field(name, packed, type);
			} else {
				document.removeField(name);
			}
		}

		final ODocument saved = document.save();
		return null != saved;
	}
}
