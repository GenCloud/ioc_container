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
package org.ioc.context.model;

import org.ioc.exceptions.IoCInstantiateException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Type constructor can make a constructor or a normal method.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class ConstructorMetadata {
	/**
	 * If it is a normal method, then specs must have a dependent instance object.
	 */
	private Object o;

	/**
	 * Required parameters.
	 */
	private Class<?>[] paramTypes;

	/**
	 * Storage constructor.
	 */
	private Function<Object[], Object> constructor;

	public ConstructorMetadata(Constructor constructor) {
		this.constructor = convertConstructorToNormalTypeConstructor(constructor);
		setParamTypes(constructor.getParameterTypes());
	}

	ConstructorMetadata(Object o, Method method) {
		this.o = o;
		this.constructor = convertMethodToNormalConstructor(method);
		setParamTypes(method.getParameterTypes());
	}

	/**
	 * Function of instantiate normal constructor bag with parameters.
	 *
	 * @param constructor for instantiation
	 * @return new instance
	 */
	private Function<Object[], Object> convertConstructorToNormalTypeConstructor(Constructor constructor) {
		return (args) -> {
			try {
				return constructor.newInstance(args);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new IoCInstantiateException("IoCError - Unavailable create instance of bag [" + constructor.getDeclaringClass() + "].", e);
			}
		};
	}

	/**
	 * Function of instantiate normal method bag with parameters.
	 *
	 * @param method for instantiation
	 * @return new instance
	 */
	private Function<Object[], Object> convertMethodToNormalConstructor(Method method) {
		return (args) -> {
			try {
				return method.invoke(getConfigObject(), args);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new IoCInstantiateException("IoCError - Unavailable create instance of bag [" + method.getDeclaringClass() + "].", e);
			}
		};
	}

	/**
	 * Applies arguments for function.
	 *
	 * @param args arguments
	 * @return new instance
	 */
	public Object construct(Object... args) {
		try {
			return constructor.apply(args);
		} catch (Exception e) {
			throw new IoCInstantiateException("IoCError - Unavailable create instance of bag [" + constructor.getClass() + "].", e);
		}
	}

	public Class<?>[] getParamTypes() {
		return paramTypes;
	}

	private void setParamTypes(Class<?>[] paramTypes) {
		this.paramTypes = paramTypes;
	}

	private Object getConfigObject() {
		return o;
	}
}
