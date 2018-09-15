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
package org.di.context.listeners.impl;

import org.di.context.listeners.Event;
import org.di.context.listeners.Listener;
import org.di.context.listeners.filter.Filter;

/**
 * This listener will filter to only dispatch events on which object matches an given {@link Filter}.
 *
 * @param <T> type of objects filtered in this filter
 * @author GenCloud
 * @date 15.09.2018
 */
public abstract class FilteredListener<T> implements Listener {
    /**
     * The filter that will be used to filter incoming events.
     */
    private final Filter<T> filter;

    /**
     * @param filter filter used to filter events
     */
    public FilteredListener(Filter<T> filter) {
        this.filter = filter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean dispatch(Event e) {
        if (!filter.accept((T) e.getSource())) {
            return true;
        }
        return dispatch(e, (T) e.getSource());
    }

    /**
     * @param e      event
     * @param object object represented in the event
     * @return true to keep listener alive
     * @see Listener#dispatch(Event)
     */
    protected abstract boolean dispatch(Event e, T object);
}
