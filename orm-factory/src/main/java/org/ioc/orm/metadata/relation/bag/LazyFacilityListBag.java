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
package org.ioc.orm.metadata.relation.bag;

import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.relation.BagMapper;
import org.ioc.orm.metadata.visitors.container.DataContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class LazyFacilityListBag<T> extends LazyFacilityBag<T> implements List<T> {
	public LazyFacilityListBag(SessionFactory sessionFactory, DataContainer container, BagMapper<T> mapper) {
		super(sessionFactory, container, mapper, ArrayList::new);
	}

	@Override
	public LazyFacilityListBag<T> copy() {
		final LazyFacilityListBag<T> bag = new LazyFacilityListBag<>(sessionFactory, dataContainer, tiBagMapper);
		bag.clear();
		bag.addAll(this);
		return bag;
	}

	@Override
	@SuppressWarnings("unchecked")
	List<T> getBag() {
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
