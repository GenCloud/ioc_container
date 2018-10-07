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
package org.ioc.threads.factory.model.impl;

import org.ioc.threads.factory.model.interfaces.ScheduledTaskFuture;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Future implementation for asynchronous tasks.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class ScheduledTaskFutureImpl implements ScheduledTaskFuture {
	/**
	 * {@link ExecutorService} {@link ScheduledFuture}
	 */
	private final ScheduledFuture<?> future;

	/**
	 * @param future {@link ExecutorService} {@link ScheduledFuture}
	 */
	public ScheduledTaskFutureImpl(ScheduledFuture<?> future) {
		this.future = future;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return future.getDelay(unit);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	@Override
	public int compareTo(Delayed o) {
		return future.compareTo(o);
	}

	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public Object get() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(long timeout, TimeUnit unit) {
		throw new UnsupportedOperationException();
	}
}
