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
package org.di.threads.factory.model.interfaces;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is an ThreadPool that you can use to asynchronously execute tasks.
 *
 * @author GenCloud
 * @date 13.09.2018
 */
public interface ThreadPool {
    String getName();

    ScheduledThreadPoolExecutor getExecutor();

    /**
     * Executes an asynchronous tasks.
     *
     * @param <T>  task return type
     * @param task callable instance
     * @return {@link AsyncFuture} notified once task has completed
     */
    <T> AsyncFuture<T> async(Task<T> task);

    /**
     * Executes an asynchronous tasks at an scheduled time.
     *
     * @param <T>   task return type
     * @param task  callable instance
     * @param delay initial delay to wait before task is executed
     * @param unit  time unit of delay
     * @return {@link AsyncFuture} notified once task has completed
     */
    <T> AsyncFuture<T> async(long delay, TimeUnit unit, Task<T> task);

    /**
     * Executes an asynchronous tasks at an scheduled time.
     *
     * @param delay  initial delay to wait before task is executed
     * @param unit   time unit of delay
     * @param repeat repeating interval for this task
     * @param task   task to be executed
     * @return {@link AsyncFuture} notified once task has completed
     */
    ScheduledAsyncFuture async(long delay, TimeUnit unit, long repeat, Runnable task);

    /**
     * Notify all future listeners when task is complete.
     */
    void notifyListeners();

    /**
     * Disposes this thread pool. After disposing, it will no longer be able to execute tasks.
     */
    void dispose();

    /**
     * @return true if thread pool is no longer usable (i.e. was disposed)
     */
    boolean isDisposed();
}
