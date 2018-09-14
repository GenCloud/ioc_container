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
package org.di.threads.factory;

import org.di.context.contexts.sensibles.EnvironmentSensible;
import org.di.context.excepton.IoCException;
import org.di.context.excepton.starter.IoCStopException;
import org.di.context.factories.config.ComponentDestroyable;
import org.di.context.factories.config.Factory;
import org.di.threads.configuration.ThreadingConfiguration;
import org.di.threads.configuration.ThreadingConfiguration.ThreadPoolPriority;
import org.di.threads.factory.model.impl.ThreadPoolImpl;
import org.di.threads.factory.model.interfaces.AsyncFuture;
import org.di.threads.factory.model.interfaces.ScheduledAsyncFuture;
import org.di.threads.factory.model.interfaces.Task;
import org.di.threads.factory.model.interfaces.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
 * @date 13.09.2018
 */
public class DefaultThreadingFactory implements Factory, ComponentDestroyable, EnvironmentSensible<ThreadingConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(DefaultThreadingFactory.class);

    private ThreadingConfiguration threadingConfiguration;

    /**
     * Public shared thread pool
     */
    private ThreadPool pool;

    private final Map<String, Future<?>> futures = new HashMap<>();
    /**
     * List of active thread pools
     */
    private Map<String, ThreadPool> threadPools = new HashMap<>();

    @Override
    public void environmentInform(ThreadingConfiguration threadingConfiguration) throws IoCException {
        this.threadingConfiguration = threadingConfiguration;
    }

    @Override
    public void initialize() {
        pool = createThreadPool(threadingConfiguration.getPoolName(),
                threadingConfiguration.getAvailableProcessors(),
                threadingConfiguration.getThreadTimeout(),
                threadingConfiguration.getThreadTimeoutUnit(),
                threadingConfiguration.getThreadPoolPriority());

        pool.async(50, TimeUnit.MILLISECONDS, 50, () -> threadPools.values().forEach(ThreadPool::notifyListeners));
    }

    /**
     * Instantiate working future in factory.
     *
     * @param name   method name
     * @param future running future
     */
    public void initFuture(String name, Future<?> future) {
        futures.put(name, future);
    }

    /**
     * Executes an asynchronous tasks. Tasks scheduled here will go to an default shared thread pool.
     *
     * @param <T>      task return type
     * @param callable callable instance
     * @return {@link AsyncFuture} notified once task has completed
     */
    public <T> AsyncFuture<T> async(Task<T> callable) {
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
     * @param <T>      task return type
     * @param callable callable instance
     * @param delay    initial delay to wait before task is executed
     * @param unit     time unit of delay
     * @return {@link AsyncFuture} notified once task has completed
     */
    public <T> AsyncFuture<T> async(long delay, TimeUnit unit, Task<T> callable) {
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
     * @return {@link AsyncFuture} notified once task has completed
     */
    public ScheduledAsyncFuture async(long delay, TimeUnit unit, long repeat, Runnable task) {
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
    private ThreadPool createThreadPool(String name, int threads, long threadTimeout,
                                        TimeUnit threadTimeoutUnit, ThreadPoolPriority priority) {
        if (log.isDebugEnabled()) {
            log.debug("Creating new {} priority ThreadPool {}; threads: {}, timeout:{}", priority, name, threads, threadTimeout);
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

        final ThreadPoolImpl pool = new ThreadPoolImpl(name, executor, this);
        threadPools.put(name, pool);
        return pool;
    }

    /**
     * Disposes an given thread pool. After disposing thread pool will no longer be usable.
     *
     * @param pool thread pool to be disposed
     */
    public void dispose(ThreadPool pool) {
        if (log.isDebugEnabled()) {
            log.debug("Disposing ThreadPool {}", pool);
        }

        pool.getExecutor().shutdown();
        threadPools.remove(pool.getName());
    }

    @Override
    public void destroy() throws IoCStopException {
        futures.values().forEach(f -> f.cancel(true));
        futures.clear();
        dispose(pool);
        pool = null;
        threadPools.clear();
        threadPools = null;
    }
}
