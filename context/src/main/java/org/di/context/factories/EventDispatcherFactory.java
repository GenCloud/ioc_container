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
package org.di.context.factories;

import org.di.context.contexts.sensibles.EnvironmentSensible;
import org.di.context.excepton.IoCException;
import org.di.context.excepton.starter.IoCStopException;
import org.di.context.factories.config.ComponentDestroyable;
import org.di.context.factories.config.Factory;
import org.di.context.listeners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This event dispatcher notify listeners that an certain event occurred in their objects.
 *
 * @author GenCloud
 * @date 15.09.2018
 */
public class EventDispatcherFactory implements Factory, ComponentDestroyable,
        EnvironmentSensible<EventDispatcherConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(EventDispatcherFactory.class);

    private Set<Listener> globalListeners = new HashSet<>();

    private Queue<EventContainer> events = new ConcurrentLinkedQueue<>();

    private EventDispatcherConfiguration eventDispatcherConfiguration;

    private ScheduledFuture<?> future;

    @Override
    public void initialize() throws IoCException {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(eventDispatcherConfiguration.getAvailableDescriptors());
        future = executor.scheduleAtFixedRate(new QueueScheduling(), 0, 10, TimeUnit.MILLISECONDS);
    }

    /**
     * Notify listeners of the event. Note that not all implementation
     * need to invoke listeners immediately. Dispatching can occur concurrently.
     *
     * @param <E>   the event type
     * @param event the event
     * @return the future. The future can be used to be notified once the event has
     * been dispatched to all listeners
     */
    public <E extends Event> EventFuture<E> fireEvent(E event) {
        if (log.isDebugEnabled()) {
            log.debug("Queuing dispatch for event {}", event);
        }

        final EventFutureImpl<E> future = new EventFutureImpl<>();
        events.add(new EventContainer(event, future));
        return future;
    }

    /**
     * Do the dispatching.
     *
     * @param event event
     */
    private synchronized void dispatch(Event event) {
        final Iterator<Listener> iter = globalListeners.iterator();
        while (iter.hasNext()) {
            try {
                final Listener listener = iter.next();
                if (!listener.dispatch(event)) {
                    iter.remove();
                }
            } catch (Throwable t) {
                log.warn("IoCError - Exception in listener", t);
                iter.remove();
            }
        }
    }

    /**
     * Adds a new global listener.
     *
     * @param listener listener
     */
    public synchronized void addListener(Listener listener) {
        log.debug("Adding new listener global {}", listener);
        globalListeners.add(listener);
    }

    /**
     * Removes an existing global listener.
     *
     * @param listener listener
     */
    public synchronized void removeListener(Listener listener) {
        globalListeners.remove(listener);
    }

    @Override
    public void environmentInform(EventDispatcherConfiguration eventDispatcherConfiguration) throws IoCException {
        this.eventDispatcherConfiguration = eventDispatcherConfiguration;
    }

    @Override
    public void destroy() throws IoCStopException {
        future.cancel(true);
        future = null;
        events.clear();
        events = null;
        globalListeners.clear();
        globalListeners = null;
    }

    private class QueueScheduling implements Runnable {
        @Override
        public void run() {
            EventContainer event;
            while ((event = events.poll()) != null) {
                synchronized (event) {
                    try {
                        if (event.getFuture().isCancelled()) {
                            continue;
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Dispatching event {}", event.getEvent());
                        }

                        // set state
                        event.getFuture().setRunning(true);
                        event.getFuture().setComplete(false);

                        // dispatch
                        dispatch(event.getEvent());
                        // the set will update state
                        event.getFuture().set(event.getEvent());
                    } catch (Throwable t) {
                        event.getFuture().setException(t);
                        log.warn("IoCError - Exception in EventDispatcherFactory thread", t);
                    }
                }
            }
        }
    }
}
