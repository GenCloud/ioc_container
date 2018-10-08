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
package org.ioc.aop.advice;

import org.ioc.aop.JunctionDot;
import org.ioc.aop.interceptor.Interceptor;
import org.ioc.context.DefaultIoCContext;
import org.ioc.context.type.IoCContext;
import org.ioc.exceptions.IoCInstantiateException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author GenCloud
 * @date 09/2018
 */
public abstract class Execution implements Interceptor {
	private static final Map<Class, Object> aspects = new ConcurrentHashMap<>();

	protected Class<?> classType;
	protected Method method;
	private IoCContext context;

	public Execution(IoCContext context, Class classType, Method method) {
		this.classType = classType;
		this.context = context;
		this.method = method;
	}

	@Override
	@SuppressWarnings("deprecation")
	public final void intercept(JunctionDot junctionDot) {
		Object instance = aspects.get(classType);
		if (instance == null) {
			try {
				instance = classType.newInstance();

				((DefaultIoCContext) context).instantiateSensibles(instance);

				aspects.put(classType, instance);
			} catch (Exception e) {
				throw new IoCInstantiateException("IoCError - Unavailable create instance for IoCAspect type - " + classType);
			}
		}

		invoke(instance, junctionDot);
	}

	protected void invoke(Object aspect, JunctionDot junctionDot) {
		try {
			method.setAccessible(true);
			method.invoke(aspect, junctionDot);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
