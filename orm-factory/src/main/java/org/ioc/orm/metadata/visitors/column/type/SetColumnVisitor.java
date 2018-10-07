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
package org.ioc.orm.metadata.visitors.column.type;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.relation.bag.LazyPersistenceSetBag;
import org.ioc.orm.metadata.visitors.column.BagColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SetColumnVisitor extends BagColumnVisitor {
	public SetColumnVisitor(Field field, Class<?> clazz, boolean lazy) {
		super(field, clazz, lazy);
	}

	@Override
	public boolean setValue(Object o, DataContainer dataContainer, SessionFactory sessionFactory) throws OrmException {
		if (isLazyLoading()) {
			return setBag(o, new LazyPersistenceSetBag(sessionFactory, dataContainer));
		} else {
			final Object of = dataContainer.of();
			return setBag(o, (Collection) of);
		}
	}

	@Override
	public Set getValue(Object o, SessionFactory sessionFactory) throws OrmException {
		return new HashSet<>(getBag(o));
	}
}
