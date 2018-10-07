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
package org.ioc.orm.metadata.visitors.relation.type;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.relation.BagMapper;
import org.ioc.orm.metadata.visitors.relation.RelationVisitor;
import org.ioc.utils.Assertion;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class ManyToOneVisitor implements RelationVisitor {
	private final BagMapper bagMapper;
	private final Object key;
	private final SessionFactory sessionFactory;

	public ManyToOneVisitor(Object key, SessionFactory sessionFactory, BagMapper bagMapper) {
		Assertion.checkNotNull(key, "key");
		Assertion.checkNotNull(sessionFactory, "session factory");
		Assertion.checkNotNull(bagMapper, "bag mapper");

		this.key = key;
		this.sessionFactory = sessionFactory;
		this.bagMapper = bagMapper;
	}

	@Override
	public Object visit() throws OrmException {
		return bagMapper.ofObject(sessionFactory, key);
	}
}
