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
package org.ioc.orm.metadata.relation.bag;

import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.relation.BagMapper;
import org.ioc.orm.metadata.type.EntityMetadata;
import org.ioc.orm.metadata.visitors.container.DataContainer;

import java.util.HashSet;
import java.util.Set;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class LazyEntitySetBag<T> extends LazyEntityBag<T> implements Set<T> {
	public LazyEntitySetBag(SessionFactory session, EntityMetadata meta, DataContainer data, BagMapper<T> factory) {
		super(session, meta, data, factory, HashSet::new);
	}

	@Override
	public LazyEntitySetBag<T> copy() {
		return new LazyEntitySetBag<>(sessionFactory, entityMetadata, dataContainer, tiBagMapper);
	}
}
