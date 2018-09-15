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
package org.di.context.listeners;

/**
 * Simple container that contains an event and a future.
 *
 * @author GenCloud
 * @date 15.09.2018
 */
public class EventContainer {
    /**
     * Event.
     */
    private final Event event;
    /**
     * Future.
     */
    private final EventFutureImpl<? extends Event> future;

    /**
     * Create new instance.
     *
     * @param event  event
     * @param future future
     */
    public EventContainer(Event event, EventFutureImpl<? extends Event> future) {
        this.event = event;
        this.future = future;
    }

    public Event getEvent() {
        return event;
    }

    public EventFutureImpl<? extends Event> getFuture() {
        return future;
    }
}