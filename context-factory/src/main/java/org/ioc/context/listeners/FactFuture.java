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
package org.ioc.context.listeners;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is an {@link Future} for {@link AbstractFact}. This {@link Future} can be used
 * to receive notifications once an event has been dispatched to all listeners.
 *
 * @param <E> the event bag in this future
 * @author GenCloud
 * @date 09/2018
 */
public interface FactFuture<E extends AbstractFact> extends Future<E> {
	/**
	 * Waits until event is dispatched to all listeners.
	 *
	 * @throws ExecutionException if any error occur while dispatching event
	 */
	void await() throws ExecutionException;

	/**
	 * Waits until event is dispatched to all listeners.
	 *
	 * @param timeout timeout
	 * @param unit    timeout unit
	 * @throws InterruptedException if thread has been interrupted while waiting
	 * @throws TimeoutException     if timeout was exceeded
	 */
	void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

	/**
	 * Waits until event is dispatched to all listeners.
	 *
	 * @return true if execution ended with no error, false otherwise
	 */
	boolean awaitUninterruptibly();

	/**
	 * Waits until the event is dispatched to all listeners.
	 *
	 * @param timeout timeout
	 * @param unit    timeout unit
	 * @return true if execution ended with no error, false otherwise
	 */
	boolean awaitUninterruptibly(long timeout, TimeUnit unit);
}
