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
package org.ioc.utils.collections;

import org.ioc.utils.Assertion;

import java.util.*;

/**
 * ArrayList implementation of Set
 *
 * @author GenCloud
 * @date 09/2018
 */
public class ArrayListSet<E> extends AbstractSet<E> {
	private final List<E> items;

	public ArrayListSet() {
		this(10);
	}

	public ArrayListSet(final Collection<? extends E> collection) {
		Assertion.checkNotNull(collection);

		items = new ArrayList<>(collection.size());
		if (Set.class.isAssignableFrom(collection.getClass())) {
			items.addAll(collection);
		} else {
			addAll(collection);
		}
	}

	public ArrayListSet(final int initialCapacity) {
		items = new ArrayList<>(initialCapacity);
	}

	@Override
	public boolean add(final E item) {
		if (item == null) {
			return false;
		}

		if (items.contains(item)) {
			return false;
		} else {
			return items.add(item);
		}
	}

	public E get(final int index) throws IndexOutOfBoundsException {
		return items.get(index);
	}

	@Override
	public Iterator<E> iterator() {
		return items.iterator();
	}

	@Override
	public int size() {
		return items.size();
	}
}
