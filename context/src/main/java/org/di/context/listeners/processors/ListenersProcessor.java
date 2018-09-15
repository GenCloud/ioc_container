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
package org.di.context.listeners.processors;

import org.di.context.annotations.Processor;
import org.di.context.annotations.listeners.Listener;
import org.di.context.contexts.AppContext;
import org.di.context.contexts.sensibles.ContextSensible;
import org.di.context.excepton.IoCException;
import org.di.context.factories.config.ComponentProcessor;

/**
 * @author GenCloud
 * @date 15.09.2018
 */
@Processor
public class ListenersProcessor implements ComponentProcessor, ContextSensible {
    private AppContext appContext;

    @Override
    public Object afterComponentInitialization(String componentName, Object component) {
        final Class<?> type = component.getClass();
        if (type.isAnnotationPresent(Listener.class)) {
            appContext.initListener(component);
        }
        return component;
    }

    @Override
    public Object beforeComponentInitialization(String componentName, Object component) {
        return component;
    }

    @Override
    public void contextInform(AppContext appContext) throws IoCException {
        this.appContext = appContext;
    }
}
