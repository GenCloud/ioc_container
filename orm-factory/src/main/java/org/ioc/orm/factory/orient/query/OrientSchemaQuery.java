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
package org.ioc.orm.factory.orient.query;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SchemaQuery;
import org.ioc.orm.factory.orient.session.OrientDatabaseSessionFactory;
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
	private final OrientDatabaseSessionFactory databaseSession;
	private final FacilityMetadata facilityMetadata;
	private final AutoClosingQuery closingQuery;

	private T first = null;

	public OrientSchemaQuery(OrientDatabaseSessionFactory databaseSession, FacilityMetadata facilityMetadata, AutoClosingQuery closingQuery) {
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
