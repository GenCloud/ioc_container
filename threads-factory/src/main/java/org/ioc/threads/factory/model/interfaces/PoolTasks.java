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
package org.ioc.threads.factory.model.interfaces;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is an PoolTasks that you can use to asynchronously execute tasks.
 *
 * @author GenCloud
 * @date 09/2018
 */
public interface PoolTasks {
	String getName();

	ScheduledThreadPoolExecutor getExecutor();

	/**
	 * Executes an asynchronous tasks.
	 *
	 * @param <T>  task return bag
	 * @param task callable instance
	 * @return {@link TaskFuture} notified once task has completed
	 */
	<T> TaskFuture<T> async(Task<T> task);

	/**
	 * Executes an asynchronous tasks at an scheduled time.
	 *
	 * @param <T>   task return bag
	 * @param task  callable instance
	 * @param delay initial delay to wait before task is executed
	 * @param unit  time unit of delay
	 * @return {@link TaskFuture} notified once task has completed
	 */
	<T> TaskFuture<T> async(long delay, TimeUnit unit, Task<T> task);

	/**
	 * Executes an asynchronous tasks at an scheduled time.
	 *
	 * @param delay  initial delay to wait before task is executed
	 * @param unit   time unit of delay
	 * @param repeat repeating interval for this task
	 * @param task   task to be executed
	 * @return {@link TaskFuture} notified once task has completed
	 */
	ScheduledTaskFuture async(long delay, TimeUnit unit, long repeat, Runnable task);

	/**
	 * Notify all future listeners when task is complete.
	 */
	void notifyListeners();

	/**
	 * Disposes this thread pool. AfterInvocation disposing, it will no longer be able to execute tasks.
	 */
	void dispose();

	/**
	 * @return true if thread pool is no longer usable (i.e. was disposed)
	 */
	boolean isDisposed();
}
