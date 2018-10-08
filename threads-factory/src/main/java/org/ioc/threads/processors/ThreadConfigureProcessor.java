/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
 *
 * IoC Starter Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IoC Starter Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IoC Starter Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.threads.processors;

import org.ioc.context.factories.Factory;
import org.ioc.context.processors.TypeProcessor;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.factories.ThreadFactorySensible;
import org.ioc.context.type.IoCContext;
import org.ioc.exceptions.IoCException;
import org.ioc.exceptions.IoCInstantiateException;
import org.ioc.threads.annotation.SimpleTask;
import org.ioc.threads.factory.DefaultThreadPoolFactory;
import org.ioc.threads.utils.GeneralTask;
import org.ioc.threads.utils.TaskProperties;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Head task configurer processor in context.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class ThreadConfigureProcessor implements TypeProcessor, ContextSensible, ThreadFactorySensible {
	private IoCContext context;
	private DefaultThreadPoolFactory factory;

	public IoCContext getContext() {
		return context;
	}

	public DefaultThreadPoolFactory getFactory() {
		return factory;
	}

	@Override
	public void contextInform(IoCContext ioCContext) throws IoCException {
		this.context = ioCContext;
	}

	@Override
	public void factoryInform(Factory factory) throws IoCException {
		this.factory = (DefaultThreadPoolFactory) factory;
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
	 * @param component bag for invoke method
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
	 * @param type bag for scan
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
