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
 * Utility class for common filter types.
 *
 * @author GenCloud
 * @date 09/2018
 */
public final class Filters {
	/**
	 * Performs an AND operation
	 *
	 * @param <O>       object bag
	 * @param strainers filters
	 * @return {@link AndStrainer}
	 */
	@SafeVarargs
	public static <O> Strainer<O> and(Strainer<O>... strainers) {
		return new AndStrainer<>(strainers);
	}

	/**
	 * Performs an OR operation
	 *
	 * @param <O>       object bag
	 * @param strainers filters
	 * @return {@link OrStrainer}
	 */
	@SafeVarargs
	public static <O> Strainer<O> or(Strainer<O>... strainers) {
		return new OrStrainer<>(strainers);
	}

	/**
	 * Performs an NOT operation.
	 *
	 * @param <O>      object bag
	 * @param strainer filter
	 * @return {@link NotStrainer}
	 */
	public static <O> Strainer<O> not(Strainer<O> strainer) {
		return new NotStrainer<>(strainer);
	}
}
