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
package org.di.threads.pocessors;

import org.di.context.annotations.Processor;
import org.di.context.contexts.AppContext;
import org.di.context.contexts.sensibles.ContextSensible;
import org.di.context.contexts.sensibles.ThreadFactorySensible;
import org.di.context.excepton.IoCException;
import org.di.context.excepton.instantiate.IoCInstantiateException;
import org.di.context.factories.config.ComponentProcessor;
import org.di.threads.annotation.SimpleTask;
import org.di.threads.factory.DefaultThreadingFactory;
import org.di.threads.utils.GeneralTask;
import org.di.threads.utils.TaskProperties;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Head task configurer processor in context.
 *
 * @author GenCloud
 * @date 13.09.2018
 */
@Processor
public class ThreadConfigureProcessor implements ComponentProcessor, ContextSensible, ThreadFactorySensible {
    private AppContext appContext;
    private DefaultThreadingFactory factory;

    public AppContext getAppContext() {
        return appContext;
    }

    public DefaultThreadingFactory getFactory() {
        return factory;
    }

    @Override
    public void contextInform(AppContext appContext) throws IoCException {
        this.appContext = appContext;
    }

    @Override
    public void threadFactoryInform(Object factory) throws IoCException {
        this.factory = (DefaultThreadingFactory) factory;
    }

    @Override
    public Object afterComponentInitialization(String componentName, Object component) {
        final List<Method> methods = findMethodsAnnotatedWithThreading(component.getClass());
        if (!methods.isEmpty()) {
            for (Method method : methods) {
                if (method.getParameterTypes().length > 0) {
                    throw new IoCInstantiateException("IoCError - Unavailable create instance of task-method [" + method + "]." +
                            "Can't instantiate task with method parameters length > 0");
                }
                instantiateTask(method, component);
            }
        }
        return component;
    }

    @Override
    public Object beforeComponentInitialization(String componentName, Object component) {
        return component;
    }

    /**
     * Initializing task method.
     *
     * @param method    method to invoke in task
     * @param component type for invoke method
     */
    private void instantiateTask(Method method, Object component) {
        final SimpleTask annotation = method.getAnnotation(SimpleTask.class);
        long startingDelay = annotation.startingDelay();
        final long fixedInterval = annotation.fixedInterval();
        final TimeUnit timeUnit = annotation.unit();
        final TaskProperties properties = new TaskProperties(startingDelay, fixedInterval, timeUnit);
        final GeneralTask task = new GeneralTask(component, method, properties);

        factory.addTask(task);
    }

    /**
     * Function of find methods annotated with SimpleTask and don't have arguments.
     *
     * @param type type for scan
     * @return collection of methods
     */
    private List<Method> findMethodsAnnotatedWithThreading(Class<?> type) {
        final Method[] methods = type.getDeclaredMethods();
        return Arrays
                .stream(methods)
                .filter(f -> f.isAnnotationPresent(SimpleTask.class))
                .filter(f -> f.getParameterTypes().length == 0)
                .collect(Collectors.toList());
    }
}
