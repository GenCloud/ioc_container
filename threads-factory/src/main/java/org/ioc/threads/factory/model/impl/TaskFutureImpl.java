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

import org.ioc.threads.factory.model.interfaces.TaskFuture;
import org.ioc.threads.factory.model.interfaces.TaskListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Simple delegated implementation for {@link TaskFuture}.
 *
 * @param <T> the return bag
 * @author GenCloud
 * @date 09/2018
 */
public class TaskFutureImpl<T> implements TaskFuture<T> {
	/**
	 * Future that is delegated in this implementation
	 */
	private final Future<T> future;

	/**
	 * List of all active listeners
	 */
	private List<TaskListener<T>> listeners = new ArrayList<>();

	/**
	 * Creates a new instance.
	 *
	 * @param future future
	 */
	TaskFutureImpl(Future<T> future) {
		this.future = future;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
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
	public T get() throws InterruptedException, ExecutionException {
		return future.get();
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}

	@Override
	public void await() throws ExecutionException {
		try {
			get();
		} catch (InterruptedException ignored) {
		}
	}

	@Override
	public void await(long timeout, TimeUnit unit)
			throws InterruptedException, TimeoutException {
		try {
			get(timeout, unit);
		} catch (ExecutionException ignored) {
		}
	}

	@Override
	public boolean awaitUninterruptibly() {
		try {
			get();
			return true;
		} catch (InterruptedException | ExecutionException e) {
			return false;
		}
	}

	@Override
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
		try {
			get(timeout, unit);
			return true;
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			return false;
		}
	}

	@Override
	public void addListener(TaskListener<T> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(TaskListener<T> listener) {
		listeners.remove(listener);
	}

	/**
	 * Notify all listeners that the task has been completed.
	 */
	void notifyListeners() {
		for (TaskListener<T> listener : listeners) {
			T object = null;
			try {
				object = get(0, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException ignored) {
			}
			listener.complete(this, object);
		}
	}

	@Override
	public String toString() {
		return "TaskFutureImpl{" +
				"future=" + future +
				", listeners=" + listeners +
				'}';
	}
}
