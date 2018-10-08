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

/**
 * AND filter that accepts all values in which all other filters return true.
 *
 * @param <O> generic bag
 * @author GenCloud
 * @date 09/2018
 */
public class AndStrainer<O> implements Strainer<O> {
	/**
	 * The filters.
	 */
	private Strainer<O>[] strainers;

	/**
	 * @param strainers filters to be used with AND operator
	 */
	@SafeVarargs
	public AndStrainer(Strainer<O>... strainers) {
		this.strainers = strainers;
	}

	@Override
	public boolean accept(O object) {
		for (Strainer<O> strainer : strainers) {
			if (!strainer.accept(object)) {
				return false;
			}
		}
		return true;
	}
}
