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
package org.ioc.orm.metadata.relation.mapper;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.factory.orient.session.OrientDatabaseSession;
import org.ioc.orm.metadata.relation.BagMapper;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.utils.Assertion;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientBagMapper<T> implements BagMapper<T> {
	private final FacilityMetadata facilityMetadata;

	public OrientBagMapper(FacilityMetadata facilityMetadata) {
		Assertion.checkNotNull(facilityMetadata);

		this.facilityMetadata = facilityMetadata;
	}

	@Override
	public ORID fromKey(SessionFactory sessionFactory, Object key) {
		if (key == null) {
			return null;
		}

		final OrientDatabaseSession databaseSession = (OrientDatabaseSession) sessionFactory;
		final OIdentifiable rid = databaseSession.findIdentifyByKey(facilityMetadata, key);
		return rid != null ? rid.getIdentity() : null;
	}

	@Override
	public ORID formObject(SessionFactory sessionFactory, T value) throws OrmException {
		if (value == null) {
			return null;
		}

		final Object key = facilityMetadata.getIdVisitor().fromObject(value);
		return fromKey(sessionFactory, key);
	}

	@SafeVarargs
	@Override
	public final List<?> formObjects(SessionFactory sessionFactory, T... values) throws OrmException {
		if (values == null || values.length <= 0) {
			return Collections.emptyList();
		}

		final List<ORID> list = Arrays.stream(values)
				.map(value -> facilityMetadata.getIdVisitor().fromObject(value))
				.map(key -> fromKey(sessionFactory, key))
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(() -> new ArrayList<>(values.length)));

		return Collections.unmodifiableList(list);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T ofObject(SessionFactory sessionFactory, Object value) {
		return (T) sessionFactory.fetch(facilityMetadata, value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> ofObjects(SessionFactory sessionFactory, Object... values) {
		return (List<T>) sessionFactory.fetch(facilityMetadata, values);
	}
}
