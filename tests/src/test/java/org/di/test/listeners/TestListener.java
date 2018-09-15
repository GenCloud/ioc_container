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
package org.di.test.listeners;

import org.di.context.listeners.Event;
import org.di.context.listeners.Listener;
import org.di.context.listeners.events.OnComponentInitEvent;
import org.di.context.listeners.events.OnContextIsInitializedEvent;
import org.di.context.listeners.events.OnContextStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 15.09.2018
 */
@org.di.context.annotations.listeners.Listener
public class TestListener implements Listener {
    private final Logger log = LoggerFactory.getLogger(TestListener.class);

    @Override
    public boolean dispatch(Event event) {
        if (OnContextStartedEvent.class.isAssignableFrom(event.getClass())) {
            log.info("ListenerInform - Context is started! [{}]", event.getSource());
        } else if (OnContextIsInitializedEvent.class.isAssignableFrom(event.getClass())) {
            log.info("ListenerInform - Context is initialized! [{}]", event.getSource());
        } else if (OnComponentInitEvent.class.isAssignableFrom(event.getClass())) {
            final OnComponentInitEvent ev = (OnComponentInitEvent) event;
            log.info("ListenerInform - Component [{}] in instance [{}] is initialized!", ev.getComponentName(), ev.getSource());
        }
        return true;
    }
}
