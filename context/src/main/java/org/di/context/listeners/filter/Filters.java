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
package org.di.context.listeners.filter;

/**
 * Utility class for common filter types.
 *
 * @author GenCloud
 * @date 15.09.2018
 */
public final class Filters {
    /**
     * Performs an AND operation
     *
     * @param <O>     object type
     * @param filters filters
     * @return {@link AndFilter}
     */
    @SafeVarargs
    public static <O> Filter<O> and(Filter<O>... filters) {
        return new AndFilter<>(filters);
    }

    /**
     * Performs an OR operation
     *
     * @param <O>     object type
     * @param filters filters
     * @return {@link OrFilter}
     */
    @SafeVarargs
    public static <O> Filter<O> or(Filter<O>... filters) {
        return new OrFilter<>(filters);
    }

    /**
     * Performs an NOT operation.
     *
     * @param <O>    object type
     * @param filter filter
     * @return {@link NotFilter}
     */
    public static <O> Filter<O> not(Filter<O> filter) {
        return new NotFilter<>(filter);
    }
}
