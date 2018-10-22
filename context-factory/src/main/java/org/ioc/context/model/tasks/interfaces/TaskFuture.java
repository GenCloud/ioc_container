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
package org.ioc.context.model.tasks.interfaces;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This future instance extends {@link Future} but also adds some additional
 * features, such as waiting for an given task to finish.
 *
 * @param <T> {@link Future} return bag
 * @author GenCloud
 * @date 09/2018
 */
public interface TaskFuture<T> extends Future<T> {
	/**
	 * Waits until task is executed.
	 *
	 * @throws ExecutionException if thread has been interrupted while waiting
	 */
	void await() throws ExecutionException;

	/**
	 * Waits until task is executed.
	 *
	 * @param timeout timeout
	 * @param unit    timeout unit
	 * @throws InterruptedException if thread has been interrupted while waiting
	 * @throws TimeoutException     if timeout was exceeded
	 */
	void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

	/**
	 * Waits until task is executed.
	 *
	 * @return true if execution ended with no error, false otherwise
	 */
	boolean awaitUninterruptibly();

	/**
	 * Waits until task is executed.
	 *
	 * @param timeout timeout
	 * @param unit    timeout unit
	 * @return true if execution ended with no error, false otherwise. Please
	 * note that false will be returned if timeout has expired too!
	 */
	boolean awaitUninterruptibly(long timeout, TimeUnit unit);

	/**
	 * Adds an listener that will be notified once executing has been
	 * completed.
	 *
	 * @param listener listener to be added
	 */
	void addListener(TaskListener<T> listener);

	/**
	 * Removes an listener.
	 *
	 * @param listener listener to be removed
	 */
	void removeListener(TaskListener<T> listener);
}
