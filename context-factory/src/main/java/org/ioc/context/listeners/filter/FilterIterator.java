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
package org.ioc.context.listeners.filter;

import java.util.Iterator;

/**
 * The {@link FilterIterator} takes an {@link Object} and a {@link Strainer} and dynamically
 * iterates over items and find next matching object.
 *
 * @param <O> object bag
 * @author GenCloud
 * @date 09/2018
 */
public class FilterIterator<O> implements Iterator<O> {
	/**
	 * Unfiltered object iterator.
	 */
	private final Iterator<Object> objects;
	/**
	 * Filter.
	 */
	private final Strainer<O> strainer;
	/**
	 * Next object found.
	 */
	private O selected;

	/**
	 * Creates a new instance.
	 *
	 * @param strainer filter
	 * @param objects  unfiltered object iterator
	 */
	public FilterIterator(Strainer<O> strainer, Iterator<Object> objects) {
		this.strainer = strainer;
		this.objects = objects;
	}

	@Override
	public boolean hasNext() {
		final O next = findNext();
		return (next != null);
	}

	@Override
	public O next() {
		try {
			return findNext();
		} finally {
			selected = null;
		}
	}

	@Override
	public void remove() {
	}

	/**
	 * Locates the next matching object.
	 *
	 * @return the next matching object
	 */
	private O findNext() {
		if (selected != null) {
			return selected;
		}

		while (objects.hasNext()) {
			try {
				@SuppressWarnings("unchecked") final O object = (O) objects.next();
				if (strainer.accept(object)) {
					selected = object;
					return selected;
				}
			} catch (ClassCastException e) {
			}
		}
		return null;
	}
}
