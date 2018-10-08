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
package org.ioc.threads.factory.model.impl;

import org.ioc.threads.factory.DefaultThreadPoolFactory;
import org.ioc.threads.factory.model.interfaces.PoolTasks;
import org.ioc.threads.factory.model.interfaces.ScheduledTaskFuture;
import org.ioc.threads.factory.model.interfaces.Task;
import org.ioc.threads.factory.model.interfaces.TaskFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class PoolTasksImpl implements PoolTasks {
	private static final Logger log = LoggerFactory.getLogger(PoolTasks.class);

	/**
	 * Thread pool name
	 */
	private final String name;
	/**
	 * Backing executor
	 */
	private final ScheduledThreadPoolExecutor executor;

	private final DefaultThreadPoolFactory defaultThreadPoolFactory;
	/**
	 * List of active and pending futures
	 */
	private final List<TaskFutureImpl<?>> activeFutures = new ArrayList<>();

	/**
	 * @param name     pool name
	 * @param executor backing {@link ScheduledThreadPoolExecutor}
	 */
	public PoolTasksImpl(String name, ScheduledThreadPoolExecutor executor, DefaultThreadPoolFactory defaultThreadPoolFactory) {
		this.name = name;
		this.executor = executor;
		this.defaultThreadPoolFactory = defaultThreadPoolFactory;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}

	@Override
	public <T> TaskFuture<T> async(Task<T> callable) {
		if (log.isDebugEnabled()) {
			log.debug("Task [{}] submit to [{}]", callable, name);
		}
		return new TaskFutureImpl<>(executor.submit(callable));
	}

	@Override
	public <T> TaskFuture<T> async(long delay, TimeUnit unit, Task<T> callable) {
		if (log.isDebugEnabled()) {
			log.debug("Task [{}] scheduled in [{}] [{}] to [{}]", callable, delay, unit, name);
		}
		return new TaskFutureImpl<>(executor.schedule(callable, delay, unit));
	}

	@Override
	public ScheduledTaskFuture async(long delay, TimeUnit unit, long repeat, Runnable task) {
		if (log.isDebugEnabled()) {
			log.debug("Task {} scheduled every {} {} to {}, starting in {}", task, repeat, unit, name, delay);
		}
		return new ScheduledTaskFutureImpl(executor.scheduleAtFixedRate(task, delay, repeat, unit));
	}

	@Override
	public void dispose() {
		defaultThreadPoolFactory.dispose(this);
	}

	@Override
	public void notifyListeners() {
		for (final TaskFutureImpl<?> future : activeFutures) {
			if (future.isDone()) {
				future.notifyListeners();
				activeFutures.remove(future);
			}
		}
	}

	@Override
	public boolean isDisposed() {
		return executor.isShutdown();
	}

	@Override
	public String toString() {
		return "PoolTasksImpl{" +
				"name='" + name + '\'' +
				'}';
	}
}

