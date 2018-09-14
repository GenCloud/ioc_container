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
package org.di.threads.factory.model.impl;

import org.di.threads.factory.DefaultThreadingFactory;
import org.di.threads.factory.model.interfaces.AsyncFuture;
import org.di.threads.factory.model.interfaces.ScheduledAsyncFuture;
import org.di.threads.factory.model.interfaces.Task;
import org.di.threads.factory.model.interfaces.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author GenCloud
 * @date 14.09.2018
 */
public class ThreadPoolImpl implements ThreadPool {
    private static final Logger log = LoggerFactory.getLogger(ThreadPool.class);

    /**
     * Thread pool name
     */
    private final String name;
    /**
     * Backing executor
     */
    private final ScheduledThreadPoolExecutor executor;

    private final DefaultThreadingFactory defaultThreadingFactory;
    /**
     * List of active and pending futures
     */
    private final List<AsyncFutureImpl<?>> activeFutures = new ArrayList<>();

    /**
     * @param name     pool name
     * @param executor backing {@link ScheduledThreadPoolExecutor}
     */
    public ThreadPoolImpl(String name, ScheduledThreadPoolExecutor executor, DefaultThreadingFactory defaultThreadingFactory) {
        this.name = name;
        this.executor = executor;
        this.defaultThreadingFactory = defaultThreadingFactory;
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
    public <T> AsyncFuture<T> async(Task<T> callable) {
        if (log.isDebugEnabled()) {
            log.debug("Task [{}] submit to [{}]", callable, name);
        }
        return new AsyncFutureImpl<>(executor.submit(callable));
    }

    @Override
    public <T> AsyncFuture<T> async(long delay, TimeUnit unit, Task<T> callable) {
        if (log.isDebugEnabled()) {
            log.debug("Task [{}] scheduled in [{}] [{}] to [{}]", callable, delay, unit, name);
        }
        return new AsyncFutureImpl<>(executor.schedule(callable, delay, unit));
    }

    @Override
    public ScheduledAsyncFuture async(long delay, TimeUnit unit, long repeat, Runnable task) {
        if (log.isDebugEnabled()) {
            log.debug("Task {} scheduled every {} {} to {}, starting in {}", task, repeat, unit, name, delay);
        }
        return new ScheduledAsyncFutureImpl(executor.scheduleAtFixedRate(task, delay, repeat, unit));
    }

    @Override
    public void dispose() {
        defaultThreadingFactory.dispose(this);
    }

    @Override
    public void notifyListeners() {
        for (final AsyncFutureImpl<?> future : activeFutures) {
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
        return "ThreadPoolImpl{" +
                "name='" + name + '\'' +
                '}';
    }
}

