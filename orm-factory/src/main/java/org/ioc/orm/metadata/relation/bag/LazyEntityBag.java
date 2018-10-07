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
import org.ioc.orm.metadata.relation.BagMapper;
import org.ioc.orm.metadata.relation.LazyBag;
import org.ioc.orm.metadata.type.EntityMetadata;
import org.ioc.orm.metadata.visitors.container.DataContainer;
import org.ioc.utils.Assertion;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author GenCloud
 * @date 10/2018
 */
public abstract class LazyEntityBag<T> implements LazyBag<T> {
	protected final DataContainer dataContainer;
	protected final EntityMetadata entityMetadata;
	protected final SessionFactory sessionFactory;
	final BagMapper<T> tiBagMapper;
	private final BagFactory<T> tiBagFactory;
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private Collection<T> entities;

	LazyEntityBag(SessionFactory sessionFactory, EntityMetadata entityMetadata,
				  final DataContainer dataContainer, BagMapper<T> tiBagMapper,
				  final BagFactory<T> tiBagFactory) {
		Assertion.checkNotNull(dataContainer, "dataContainer container");
		Assertion.checkNotNull(sessionFactory, "sessionFactory");
		Assertion.checkNotNull(entityMetadata, "metadata");
		Assertion.checkNotNull(tiBagMapper, "collection mapper");
		Assertion.checkNotNull(tiBagFactory, "collection factory");

		this.dataContainer = dataContainer;
		this.sessionFactory = sessionFactory;
		this.entityMetadata = entityMetadata;
		this.tiBagMapper = tiBagMapper;
		this.tiBagFactory = tiBagFactory;
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
		if (entities != null) {
			return entities;
		}

		try {
			final Object value = dataContainer.of();
			initialized.getAndSet(true);
			if (value == null) {
				entities = tiBagFactory.initialize(0);
				return entities;
			}

			if (!(value instanceof Collection)) {
				throw new OrmException("Expected type of collection but found [" + value + "] from dataContainer holder [" + dataContainer + "].");
			}

			if (value instanceof LazyEntityBag) {
				final LazyEntityBag lazy = (LazyEntityBag) value;
				entities = tiBagFactory.initialize(lazy.size());
				entities.addAll(lazy);
				return entities;
			}

			final Object[] keys = ((Collection) value).toArray();
			final List<T> results = tiBagMapper.ofObjects(sessionFactory, keys);
			entities = tiBagFactory.initialize(results.size());
			entities.addAll(results);
			return entities;
		} catch (Exception e) {
			throw new OrmException("Unable to query lazy entityMetadata collection with dataContainer [" + dataContainer + "].", e);
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
	public boolean addAll(Collection<? extends T> c) {
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
