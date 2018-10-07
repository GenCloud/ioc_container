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

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.orient.OrientDocumentConverter;
import org.ioc.orm.factory.orient.session.OrientDatabaseSession;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.container.DataContainer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientLazyQueryContainer implements DataContainer {
	private final AtomicBoolean loaded = new AtomicBoolean(false);

	private final OrientDatabaseSession databaseSession;
	private final FacilityMetadata facilityMetadata;
	private final boolean collection;
	private final String query;
	private final List<Object> parameters;
	private final OrientDocumentConverter converter;

	private final List<ODocument> documents = new ArrayList<>();

	public OrientLazyQueryContainer(OrientDatabaseSession databaseSession, FacilityMetadata facilityMetadata, boolean collection,
									String query, List<Object> parameters, OrientDocumentConverter converter) {
		this.databaseSession = databaseSession;
		this.facilityMetadata = facilityMetadata;
		this.collection = collection;
		this.query = query;
		this.parameters = new ArrayList<>(parameters);
		this.converter = converter;
	}

	@Override
	public boolean empty() {
		try {
			return ensureResults().isEmpty();
		} catch (Exception e) {
			throw new OrmException("Unable to query lazy loaded results from [" + facilityMetadata + "].", e);
		}
	}

	@Override
	public Object of() throws OrmException {
		final Collection<ODocument> results = ensureResults();
		if (results.isEmpty()) {
			return null;
		}
		try {
			if (collection) {
				return Collections.unmodifiableList(results
						.stream()
						.map(converter::convert)
						.filter(Objects::nonNull)
						.collect(Collectors.toCollection(() -> new ArrayList<>(results.size()))));
			} else {
				return converter.convert(results.iterator().next());
			}
		} catch (Exception e) {
			throw new OrmException("Unable to entity lazy loaded results for metadata [" + facilityMetadata + "].", e);
		}
	}

	private List<ODocument> ensureResults() throws OrmException {
		if (loaded.get()) {
			return Collections.unmodifiableList(documents);
		}

		try {
			documents.clear();
			databaseSession.getDocument().query(new OSQLSynchQuery(query), parameters.toArray())
					.stream()
					.filter(item -> item instanceof ODocument)
					.map(item -> (ODocument) item)
					.forEach(documents::add);

			loaded.getAndSet(true);
		} catch (Exception e) {
			throw new OrmException("Unable to fetch document by query [" + query + "].", e);
		}
		return Collections.unmodifiableList(documents);
	}
}
