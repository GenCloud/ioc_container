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
package org.ioc.orm.factory.orient.query;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SchemaQuery;
import org.ioc.orm.factory.orient.session.OrientDatabaseSession;
import org.ioc.orm.metadata.type.FacilityMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientSchemaQuery<T> implements SchemaQuery<T> {
	private final OrientDatabaseSession databaseSession;
	private final FacilityMetadata facilityMetadata;
	private final AutoClosingQuery closingQuery;

	private T first = null;

	public OrientSchemaQuery(OrientDatabaseSession databaseSession, FacilityMetadata facilityMetadata, AutoClosingQuery closingQuery) {
		this.databaseSession = databaseSession;
		this.facilityMetadata = facilityMetadata;
		this.closingQuery = closingQuery;
	}

	@Override
	public T first() {
		if (first != null) {
			return first;
		}

		for (T item : this) {
			if (item != null) {
				first = item;
				return item;
			}
		}

		return null;
	}

	@Override
	public List<T> list() {
		final List<T> list = new ArrayList<>();
		for (T item : this) {
			if (item != null) {
				list.add(item);
			}
		}
		return Collections.unmodifiableList(list);
	}

	@Override
	public boolean empty() {
		return first() != null;
	}

	@Override
	public Iterator<T> iterator() {
		final Iterator<ODocument> iterator = closingQuery.iterator();
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public T next() {
				final ODocument next = iterator.next();
				if (next == null) {
					return null;
				}

				try {
					return databaseSession.install(facilityMetadata, next);
				} catch (Exception e) {
					throw new OrmException("Unable to fetch next entity [" + facilityMetadata + "] from document [" + next.getIdentity() + "].", e);
				}
			}
		};
	}

	@Override
	public void close() throws Exception {
		closingQuery.close();
	}
}
