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
package org.di.threads.listeners;

import org.di.context.annotations.listeners.Listener;
import org.di.context.contexts.sensibles.ThreadFactorySensible;
import org.di.context.excepton.IoCException;
import org.di.context.listeners.events.OnContextIsInitializedEvent;
import org.di.context.listeners.impl.TypedListener;
import org.di.threads.factory.DefaultThreadingFactory;

/**
 * @author GenCloud
 * @date 15.09.2018
 */
@Listener
public class ContextInitListener extends TypedListener<OnContextIsInitializedEvent> implements ThreadFactorySensible {
    private DefaultThreadingFactory defaultThreadingFactory;

    /**
     * @param type type of accepted events
     */
    public ContextInitListener(Class<OnContextIsInitializedEvent> type) {
        super(type);
    }

    @Override
    protected boolean dispatch(OnContextIsInitializedEvent e) {
        defaultThreadingFactory.initTasks();
        return true;
    }

    @Override
    public void threadFactoryInform(Object defaultThreadingFactory) throws IoCException {
        this.defaultThreadingFactory = (DefaultThreadingFactory) defaultThreadingFactory;
    }
}
