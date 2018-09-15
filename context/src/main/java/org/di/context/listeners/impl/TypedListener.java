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

/**
 * This listener will filter to only dispatch an certain type events.
 *
 * @param <T> type filtered by this {@link Listener}
 * @author GenCloud
 * @date 15.09.2018
 */
public abstract class TypedListener<T> implements Listener {
    /**
     * Type this listener will accept.
     */
    private final Class<T> type;

    /**
     * @param type type of accepted events
     * @notice recommend insert full class type of {@param <T>}
     */
    public TypedListener(Class<T> type) {
        this.type = type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean dispatch(Event e) {
        if (!type.isInstance(e)) {
            return true;
        }
        return dispatch((T) e);
    }

    /**
     * @param e event
     * @return true to keep listener alive
     * @see Listener#dispatch(Event)
     */
    protected abstract boolean dispatch(T e);
}
