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

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.relation.BagFactory;
import org.ioc.orm.metadata.relation.LazyBag;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.utils.Assertion;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author GenCloud
 * @date 10/2018
 */
public abstract class LazyPersistenceBag<T> implements LazyBag<T> {
	protected final DataContainer dataContainer;
	protected final SessionFactory sessionFactory;
	private final BagFactory<T> bagFactory;
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private Collection<T> entities;

	public LazyPersistenceBag(SessionFactory sessionFactory, DataContainer dataContainer,
							  final BagFactory<T> bagFactory) {
		Assertion.checkNotNull(dataContainer, "container");
		Assertion.checkNotNull(sessionFactory, "sessionFactory");
		Assertion.checkNotNull(bagFactory, "collection factory");

		this.dataContainer = dataContainer;
		this.sessionFactory = sessionFactory;
		this.bagFactory = bagFactory;
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

	protected Collection<T> getBag() {
		return installEntities();
	}

	@SuppressWarnings("unchecked")
	private Collection<T> installEntities() {
		if (initialized.get()) {
			return entities;
		}

		try {
			final Object value = dataContainer.of();
			if (value == null) {
				entities = bagFactory.initialize(0);
			} else if (value instanceof Collection) {
				final Collection<T> collection = (Collection) value;
				entities = bagFactory.initialize(collection.size());
				entities.addAll(collection);
			} else {
				throw new OrmException("Expected vertex collection but found value [" + value + "].");
			}
			initialized.getAndSet(true);
			return entities;
		} catch (Exception e) {
			throw new OrmException("Unable to query lazy vertex collection with container [" + dataContainer + "].", e);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return installEntities().equals(obj);
	}

	@Override
	public int hashCode() {
		return installEntities().hashCode();
	}

	@Override
	public int size() {
		return installEntities().size();
	}

	@Override
	public boolean isEmpty() {
		return installEntities().isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		if (o == null) {
			return false;
		}
		return installEntities().contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return installEntities().iterator();
	}

	@Override
	public Object[] toArray() {
		return installEntities().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return installEntities().toArray(a);
	}

	@Override
	public boolean add(T o) {
		return installEntities().add(o);
	}

	@Override
	public boolean remove(Object o) {
		return installEntities().remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return installEntities().containsAll(c);
	}

	@Override
	public boolean addAll(Collection c) {
		return installEntities().addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return installEntities().removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return installEntities().retainAll(c);
	}

	@Override
	public void clear() {
		installEntities().clear();
	}
}
