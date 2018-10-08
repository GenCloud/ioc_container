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
package org.ioc.context.factories;

import org.ioc.annotations.context.Order;
import org.ioc.context.listeners.*;
import org.ioc.context.processors.DestroyProcessor;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.enviroment.configurations.FactDispatcherAutoConfiguration;
import org.ioc.exceptions.IoCException;
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
 * @date 09/2018
 */
@Order(999)
public class FactDispatcherFactory implements Factory, DestroyProcessor, EnvironmentSensible<FactDispatcherAutoConfiguration> {
	private static final Logger log = LoggerFactory.getLogger(FactDispatcherFactory.class);

	private Set<IListener> globalIListeners = new HashSet<>();

	private Queue<FactContainer> events = new ConcurrentLinkedQueue<>();

	private FactDispatcherAutoConfiguration factDispatcherAutoConfiguration;

	private ScheduledFuture<?> future;

	@Override
	public void initialize() throws IoCException {
		final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(factDispatcherAutoConfiguration.getAvailableDescriptors());
		future = executor.scheduleAtFixedRate(new QueueScheduling(), 0, 10, TimeUnit.MILLISECONDS);
	}

	/**
	 * Notify listeners of the event. Note that not all implementation
	 * need to invoke listeners immediately. Dispatching can occur concurrently.
	 *
	 * @param <E>   the event bag
	 * @param event the event
	 * @return the future. The future can be used to be notified once the event has
	 * been dispatched to all listeners
	 */
	public <E extends AbstractFact> FactFuture<E> fireEvent(E event) {
		if (log.isDebugEnabled()) {
			log.debug("Queuing dispatch for event {}", event);
		}

		final FactFutureImpl<E> future = new FactFutureImpl<>();
		events.add(new FactContainer(event, future));
		return future;
	}

	/**
	 * Do the dispatching.
	 *
	 * @param abstractFact event
	 */
	private synchronized void dispatch(AbstractFact abstractFact) {
		final Iterator<IListener> iter = globalIListeners.iterator();
		while (iter.hasNext()) {
			try {
				final IListener listener = iter.next();
				if (!listener.dispatch(abstractFact)) {
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
	public synchronized void addListener(IListener listener) {
		log.debug("Adding new listener global {}", listener);
		globalIListeners.add(listener);
	}

	/**
	 * Removes an existing global listener.
	 *
	 * @param listener listener
	 */
	public synchronized void removeListener(IListener listener) {
		globalIListeners.remove(listener);
	}

	@Override
	public void environmentInform(FactDispatcherAutoConfiguration factDispatcherAutoConfiguration) throws IoCException {
		this.factDispatcherAutoConfiguration = factDispatcherAutoConfiguration;
	}

	@Override
	public void destroy() {
		future.cancel(true);
		future = null;
		events.clear();
		events = null;
		globalIListeners.clear();
		globalIListeners = null;
	}

	private class QueueScheduling implements Runnable {
		@Override
		public void run() {
			FactContainer event;
			while ((event = events.poll()) != null) {
				synchronized (event) {
					try {
						if (event.getFuture().isCancelled()) {
							continue;
						}

						if (log.isDebugEnabled()) {
							log.debug("Dispatching event {}", event.getAbstractFact());
						}

						event.getFuture().setRunning(true);
						event.getFuture().setComplete(false);

						dispatch(event.getAbstractFact());
						event.getFuture().complete(event.getAbstractFact());
					} catch (Throwable t) {
						event.getFuture().completeExceptionally(t);
						log.warn("IoCError - Exception in EventDispatcherFactory thread", t);
					}
				}
			}
		}
	}
}
