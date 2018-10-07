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
package org.ioc.context.listeners.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * And filter that accepts all values that are not in objects.
 *
 * @param <O> object bag
 * @author GenCloud
 * @date 09/2018
 */
public class ExcludeStrainer<O> implements Strainer<O> {
	/**
	 * The objects to be excluded.
	 */
	private Set<Object> objects = new HashSet<>();

	/**
	 * @param objects excluded objects
	 */
	public ExcludeStrainer(Object... objects) {
		Collections.addAll(this.objects, objects);
	}

	/**
	 * @param objects excluded objects
	 */
	public ExcludeStrainer(Collection<Object> objects) {
		this.objects.addAll(objects);
	}

	@Override
	public boolean accept(O object) {
		return !objects.contains(object);
	}
}
