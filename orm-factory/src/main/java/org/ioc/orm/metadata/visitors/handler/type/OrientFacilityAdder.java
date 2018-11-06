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
package org.ioc.orm.metadata.visitors.handler.type;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.ioc.orm.factory.orient.session.OrientDatabaseSessionFactory;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.handler.FacilityAdder;
import org.ioc.orm.util.OrientUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientFacilityAdder implements FacilityAdder {
	private final OrientDatabaseSessionFactory databaseSession;

	public OrientFacilityAdder(OrientDatabaseSessionFactory databaseSession) {
		this.databaseSession = databaseSession;
	}

	@Override
	public boolean add(FacilityMetadata entity, Map<ColumnMetadata, Object> data) {
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
				if (existing != null) {
					if (document.containsField(name)) {
						final Object raw = document.field(name);
						final Object oldValue = raw != null ? OrientUtils.convertRaw(column, raw) : null;

						if (!Objects.equals(oldValue, value)) {
							document.field(name, packed, type);
						}
					}
				} else {
					document.field(name, packed, type);
				}
			} else {
				document.removeField(name);
			}
		}

		final ODocument saved = document.save();
		return saved != null;
	}
}
