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
package org.ioc.utils;

import org.ioc.context.model.tasks.AbstractTask;

import java.lang.reflect.Method;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class GeneralTask extends AbstractTask<Void> implements Runnable {
	private final Object o;
	private final Method method;
	private final TaskProperties taskProperties;

	public GeneralTask(Object o, Method method, TaskProperties taskProperties) {
		this.o = o;
		this.method = method;
		this.taskProperties = taskProperties;
	}

	public TaskProperties getTaskProperties() {
		return taskProperties;
	}

	public Object getObject() {
		return o;
	}

	public Method getMethod() {
		return method;
	}

	@Override
	public Void call() throws Exception {
		method.setAccessible(true);
		method.invoke(o);
		method.setAccessible(false);
		return null;
	}

	@Override
	public void run() {
		try {
			call();
		} catch (Exception ignored) {
		}
	}
}
