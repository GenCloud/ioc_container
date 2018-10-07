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

import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.relation.BagMapper;
import org.ioc.orm.metadata.type.EntityMetadata;
import org.ioc.utils.Assertion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class BaseBagMapper<T> implements BagMapper<T> {
	private final EntityMetadata entityMetadata;

	public BaseBagMapper(EntityMetadata entityMetadata) {
		Assertion.checkNotNull(entityMetadata, "entity metadata");

		this.entityMetadata = entityMetadata;
	}

	public EntityMetadata getEntityMetadata() {
		return entityMetadata;
	}

	@Override
	public Object fromKey(SessionFactory sessionFactory, Object key) {
		return key;
	}

	@Override
	public Object formObject(SessionFactory sessionFactory, T value) {
		return entityMetadata.getIdVisitor().fromObject(value);
	}

	@SafeVarargs
	@Override
	public final List<?> formObjects(SessionFactory sessionFactory, T... values) {
		if (values == null || values.length <= 0) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableList(Arrays.stream(values)
				.map(value -> entityMetadata.getIdVisitor().fromObject(value))
				.filter(Objects::nonNull)
				.collect(Collectors.toList()));
	}

	@Override
	@SuppressWarnings("unchecked")
	public T ofObject(SessionFactory sessionFactory, Object value) {
		return (T) sessionFactory.fetch(entityMetadata, value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> ofObjects(SessionFactory sessionFactory, Object... values) {
		return (List<T>) sessionFactory.fetch(entityMetadata, values);
	}
}
