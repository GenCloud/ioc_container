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
package org.ioc.orm.generator.type;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.factory.orient.OrientSchemaFactory;
import org.ioc.orm.factory.orient.session.OrientDatabaseSession;
import org.ioc.orm.generator.IdProducer;
import org.ioc.orm.metadata.type.FacilityMetadata;

/**
 * Sequenced id generator.
 *
 * @author GenCloud
 * @date 10/2018
 */
public class OrientIdProducer implements IdProducer {
	private static final Object lock = new Object();

	private final String tableName;
	private final String indexName;
	private final String keyColumn;
	private final String valueColumn;
	private final String keyValue;
	private final OrientSchemaFactory schemaFactory;

	public OrientIdProducer(String tableName, String indexName, String keyColumn, String valueColumn,
							OrientSchemaFactory schemaFactory) {
		this.tableName = tableName;
		this.indexName = indexName;
		this.keyColumn = keyColumn;
		this.valueColumn = valueColumn;
		this.keyValue = keyColumn;
		this.schemaFactory = schemaFactory;
	}

	@Override
	public Long create(SessionFactory sessionFactory, FacilityMetadata facilityMetadata) throws OrmException {
		if (sessionFactory == null) {
			return null;
		}

		if (facilityMetadata == null) {
			return null;
		}

		synchronized (lock) {
			try {
				if (sessionFactory instanceof OrientDatabaseSession) {
					final ODatabaseDocument db = ((OrientDatabaseSession) sessionFactory).getDocument();
					return create(db);
				} else {
					try (ODatabaseDocument db = schemaFactory.createSchema()) {
						return create(db);
					}
				}
			} catch (Exception e) {
				throw new OrmException("Unable to increment counter id [" + keyValue + "] from table [" + tableName + "].", e);
			}
		}
	}

	private Long create(ODatabaseDocument databaseDocument) {
		databaseDocument.begin();
		for (int i = 0; i < 10; i++) {
			final Long value = increment(databaseDocument);
			if (value != null) {
				databaseDocument.commit();
				return value;
			}
		}
		databaseDocument.rollback();

		throw new OrmException("Unable to generate counter id.");
	}

	private Long increment(ODatabaseDocument databaseDocument) {
		ODocument document = findDocument(databaseDocument);
		if (document == null) {
			document = databaseDocument.newInstance(tableName);
			document.field(keyColumn, keyValue);
		}

		final Long current = document.field(valueColumn);
		final Long next;
		if (current == null) {
			next = 1L;
		} else {
			next = current + 1;
		}

		document.field(valueColumn, next);
		document.save();
		document.detach();
		return next;
	}

	private ODocument findDocument(ODatabaseDocument databaseDocument) {
		final OIndex index = databaseDocument.getMetadata().getIndexManager().getClassIndex(tableName, indexName);
		if (index == null) {
			throw new OrmException("Unable to locate database index [" + indexName + "].");
		}

		final OIdentifiable identifiable = (OIdentifiable) index.get(keyValue);
		if (identifiable == null) {
			return null;
		}

		final ORID identity = identifiable.getIdentity();
		if (identity == null) {
			return null;
		}

		final ODocument entries = databaseDocument.load(identity);
		if (entries == null) {
			throw new OrmException("Unable to load database record id [" + identity + "].");
		}
		return entries;
	}
}
