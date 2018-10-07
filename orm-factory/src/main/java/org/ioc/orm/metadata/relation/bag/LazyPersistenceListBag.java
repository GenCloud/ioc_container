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
import org.ioc.orm.metadata.relation.LazyBag;
import org.ioc.orm.metadata.visitors.container.DataContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class LazyPersistenceListBag<T> extends LazyPersistenceBag<T> implements List<T> {
	public LazyPersistenceListBag(SessionFactory session, DataContainer data) {
		super(session, data, ArrayList::new);
	}

	@Override
	public LazyBag<T> copy() {
		return new LazyPersistenceListBag<>(sessionFactory, dataContainer);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<T> getBag() {
		return (List) super.getBag();
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		return getBag().addAll(index, c);
	}

	@Override
	public T get(int index) {
		return getBag().get(index);
	}

	@Override
	public T set(int index, T element) {
		return getBag().set(index, element);
	}

	@Override
	public void add(int index, T element) {
		getBag().add(index, element);
	}

	@Override
	public T remove(int index) {
		return getBag().remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return getBag().indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return getBag().lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		return getBag().listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return getBag().listIterator(index);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return getBag().subList(fromIndex, toIndex);
	}
}
