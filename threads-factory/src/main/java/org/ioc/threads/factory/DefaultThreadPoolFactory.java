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
package org.ioc.threads.factory;

import org.ioc.annotations.context.Order;
import org.ioc.context.factories.Factory;
import org.ioc.context.processors.DestroyProcessor;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.enviroment.configurations.ThreadingAutoConfiguration;
import org.ioc.enviroment.configurations.ThreadingAutoConfiguration.ThreadPoolPriority;
import org.ioc.exceptions.IoCException;
import org.ioc.threads.factory.model.impl.PoolTasksImpl;
import org.ioc.threads.factory.model.interfaces.PoolTasks;
import org.ioc.threads.factory.model.interfaces.ScheduledTaskFuture;
import org.ioc.threads.factory.model.interfaces.Task;
import org.ioc.threads.factory.model.interfaces.TaskFuture;
import org.ioc.threads.utils.GeneralTask;
import org.ioc.threads.utils.TaskProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Component is responsible for scheduling tasks and executing them in parallel.
 *
 * @author GenCloud
 * @date 09/2018
 */
@Order(9999)
public class DefaultThreadPoolFactory implements Factory, EnvironmentSensible<ThreadingAutoConfiguration>, DestroyProcessor {
	private static final Logger log = LoggerFactory.getLogger(DefaultThreadPoolFactory.class);
	private final Map<String, Future<?>> futures = new HashMap<>();
	private final List<GeneralTask> tasks = new ArrayList<>();
	private ThreadingAutoConfiguration threadingAutoConfiguration;
	/**
	 * Public shared thread pool
	 */
	private PoolTasks pool;
	/**
	 * List of active thread pools
	 */
	private Map<String, PoolTasks> threadPools = new HashMap<>();

	@Override
	public void environmentInform(ThreadingAutoConfiguration threadingAutoConfiguration) throws IoCException {
		this.threadingAutoConfiguration = threadingAutoConfiguration;
	}

	@Override
	public void initialize() {
		pool = createThreadPool(threadingAutoConfiguration.getPoolName(),
				threadingAutoConfiguration.getAvailableProcessors(),
				threadingAutoConfiguration.getThreadTimeout(),
				threadingAutoConfiguration.getThreadTimeoutUnit(),
				threadingAutoConfiguration.getThreadPoolPriority());

		pool.async(50, TimeUnit.MILLISECONDS, 50, () -> threadPools.values().forEach(PoolTasks::notifyListeners));
	}

	/**
	 * Add found task to factories.
	 *
	 * @param task metadata of task
	 */
	public void addTask(GeneralTask task) {
		tasks.add(task);
	}

	/**
	 * Instantiate tasks in factories and start it.
	 */
	public void initTasks() {
		tasks.forEach(t -> {
			final TaskProperties properties = t.getTaskProperties();
			final TimeUnit timeUnit = properties.getTimeUnit();
			if (properties.getStartingDelay() < 0) {
				properties.setStartingDelay(0);
			}

			if (properties.getStartingDelay() == 0 && properties.getFixedInterval() != -1) {
				final Future<?> future = async(0, timeUnit, properties.getFixedInterval(), t);
				futures.put(t.getMethod().getName(), future);
				return;
			}

			if (properties.getStartingDelay() > 0 && properties.getFixedInterval() > 0) {
				final Future<?> future = async(properties.getStartingDelay(), timeUnit, properties.getFixedInterval(), t);
				futures.put(t.getMethod().getName(), future);
				return;
			}

			if (properties.getFixedInterval() == -1) {
				final Future<?> future = async(properties.getStartingDelay(), timeUnit, t);
				futures.put(t.getMethod().getName(), future);
			}
		});
	}

	/**
	 * Executes an asynchronous tasks. Tasks scheduled here will go to an default shared thread pool.
	 *
	 * @param <T>      task return bag
	 * @param callable callable instance
	 * @return {@link TaskFuture} notified once task has completed
	 */
	public <T> TaskFuture<T> async(Task<T> callable) {
		if (log.isDebugEnabled()) {
			log.debug("Scheduling async task: {}", callable);
		}
		return pool.async(callable);
	}

	/**
	 * Executes an asynchronous tasks at an scheduled time. Please note that resources in scheduled
	 * thread pool are limited and tasks should be performed fast.
	 * <p>
	 * Tasks scheduled here will go to an default shared thread pool.
	 *
	 * @param <T>      task return bag
	 * @param callable callable instance
	 * @param delay    initial delay to wait before task is executed
	 * @param unit     time unit of delay
	 * @return {@link TaskFuture} notified once task has completed
	 */
	public <T> TaskFuture<T> async(long delay, TimeUnit unit, Task<T> callable) {
		if (log.isDebugEnabled()) {
			log.debug("Scheduling async task in {}ms: {}", unit.toMillis(delay), callable);
		}
		return pool.async(delay, unit, callable);
	}

	/**
	 * Executes an asynchronous tasks at an scheduled time. Please note that resources in scheduled
	 * thread pool are limited and tasks should be performed fast.
	 * <p>
	 * Tasks scheduled here will go to an default shared thread pool.
	 *
	 * @param delay  initial delay to wait before task is executed
	 * @param unit   time unit of delay
	 * @param repeat repeating interval for this task
	 * @param task   task to be executed
	 * @return {@link TaskFuture} notified once task has completed
	 */
	public ScheduledTaskFuture async(long delay, TimeUnit unit, long repeat, Runnable task) {
		if (log.isDebugEnabled()) {
			log.debug("Scheduling repeating async task in {}ms each {}ms: {}", unit.toMillis(delay), unit.toMillis(repeat),
					task);
		}
		return pool.async(delay, unit, repeat, task);
	}

	/**
	 * Creates a new thread pool.
	 *
	 * @param name              pool name
	 * @param threads           maximum amount of active threads
	 * @param threadTimeout     time it takes to expire an inactive thread
	 * @param threadTimeoutUnit {@link TimeUnit} for {@param threadTimeout}
	 * @param priority          processor scheduling priority
	 * @return new thread pool
	 */
	private PoolTasks createThreadPool(String name, int threads, long threadTimeout,
									   TimeUnit threadTimeoutUnit, ThreadPoolPriority priority) {
		if (log.isDebugEnabled()) {
			log.debug("Creating new {} priority PoolTasks {}; threads: {}, timeout:{}", priority, name, threads, threadTimeout);
		}

		final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threads);

		if (threadTimeout >= 1) {
			executor.setKeepAliveTime(threadTimeout, threadTimeoutUnit);
			executor.allowCoreThreadTimeOut(true);
		}

		executor.setThreadFactory(new ThreadFactory() {
			private final AtomicInteger threadNumber = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				final Thread thread = new Thread(r, name + "-" + threadNumber.getAndIncrement());
				thread.setPriority(priority.get());
				return thread;
			}
		});

		final PoolTasksImpl pool = new PoolTasksImpl(name, executor, this);
		threadPools.put(name, pool);
		return pool;
	}

	/**
	 * Disposes an given thread pool. AfterInvocation disposing thread pool will no longer be usable.
	 *
	 * @param pool thread pool to be disposed
	 */
	public void dispose(PoolTasks pool) {
		if (log.isDebugEnabled()) {
			log.debug("Disposing PoolTasks {}", pool);
		}

		pool.getExecutor().shutdown();
		threadPools.remove(pool.getName());
	}

	@Override
	public void destroy() {
		futures.values().forEach(f -> f.cancel(true));
		futures.clear();
		dispose(pool);
		pool = null;
		threadPools.clear();
		threadPools = null;
	}

	@Override
	public String toString() {
		return "DefaultThreadPoolFactory{pool=" + pool +
				", futures=" + futures +
				", tasks=" + tasks +
				", threadPools=" + threadPools +
				'}';
	}
}
