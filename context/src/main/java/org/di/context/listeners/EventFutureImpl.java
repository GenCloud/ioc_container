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
package org.di.context.listeners;

import com.google.common.util.concurrent.AbstractFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author GenCloud
 * @date 15.09.2018
 */
public class EventFutureImpl<E extends Event> extends AbstractFuture<E> implements EventFuture<E> {
    private boolean running = false;
    private boolean complete = false;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean set(Event value) {
        running = false;
        complete = true;
        return super.set((E) value);
    }

    @Override
    public boolean setException(Throwable throwable) {
        return super.setException(throwable);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return !(!mayInterruptIfRunning && running) && !complete && super.cancel(mayInterruptIfRunning);
    }

    @Override
    public void await() throws ExecutionException {
        try {
            super.get();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            super.get(timeout, unit);
        } catch (ExecutionException ignored) {
        }
    }

    @Override
    public boolean awaitUninterruptibly() {
        try {
            super.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        try {
            super.get(timeout, unit);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }
}