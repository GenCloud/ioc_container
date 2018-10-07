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
package org.ioc.orm.metadata.relation.proxy;

import net.sf.cglib.proxy.InvocationHandler;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.orm.metadata.visitors.container.type.BaseDataContainer;
import org.ioc.orm.metadata.visitors.relation.RelationVisitor;
import org.ioc.utils.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class LazyRelationHandler implements InvocationHandler {
	private static final Logger log = LoggerFactory.getLogger(LazyRelationHandler.class);

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private final FacilityMetadata facilityMetadata;
	private final RelationVisitor relationVisitor;
	private final SessionFactory sessionFactory;
	private final Class<?> entityClass;

	public LazyRelationHandler(FacilityMetadata facilityMetadata, RelationVisitor relationVisitor,
							   SessionFactory sessionFactory, Class<?> entityClass) {
		this.entityClass = entityClass;
		Assertion.checkNotNull(facilityMetadata, "entity metadata");
		Assertion.checkNotNull(relationVisitor, "visitor");
		Assertion.checkNotNull(sessionFactory, "sessionFactory");

		this.facilityMetadata = facilityMetadata;
		this.relationVisitor = relationVisitor;
		this.sessionFactory = sessionFactory;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public boolean isInitialized() {
		return initialized.get();
	}

	private boolean load(Object self) throws OrmException {
		if (initialized.get()) {
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug("Intercepting proxy method invocation, fetching lazy relation for [{}].", facilityMetadata);
		}

		final Object value = relationVisitor.visit();
		if (value == null) {
			log.info("Lazy relation fetched, null/empty value found.");
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug("Lazy relation fetched, copying facilityMetadata values from instance [{}].", value);
		}

		for (Map.Entry<ColumnMetadata, ColumnVisitor> entry : facilityMetadata.getColumnVisitorMap()) {
			final ColumnVisitor visitor = entry.getValue();
			final Object columnValue = visitor.getValue(value, sessionFactory);
			final DataContainer data = new BaseDataContainer(columnValue);
			visitor.setValue(self, data, sessionFactory);
		}

		initialized.getAndSet(true);
		return true;
	}

	@Override
	public Object invoke(Object o, Method method, Object[] args) throws Throwable {
		if (!initialized.get()) {
			load(o);
		}

		return method.invoke(o, args);
	}
}
