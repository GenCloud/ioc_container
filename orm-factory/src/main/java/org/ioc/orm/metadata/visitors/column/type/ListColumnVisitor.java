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
package org.ioc.orm.metadata.visitors.column.type;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.relation.bag.LazyPersistenceListBag;
import org.ioc.orm.metadata.visitors.column.BagColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class ListColumnVisitor extends BagColumnVisitor {
	public ListColumnVisitor(Field field, Class<?> clazz, boolean lazy) {
		super(field, clazz, lazy);
	}

	@Override
	public boolean setValue(Object o, DataContainer dataContainer, SessionFactory sessionFactory) throws OrmException {
		if (isLazyLoading()) {
			return setBag(o, new LazyPersistenceListBag(sessionFactory, dataContainer));
		} else {
			final Object value = dataContainer.of();
			return setBag(o, (Collection) value);
		}
	}

	@Override
	public List getValue(Object o, SessionFactory sessionFactory) throws OrmException {
		final Collection<?> list = getBag(o);
		return new ArrayList<>(list);
	}
}
