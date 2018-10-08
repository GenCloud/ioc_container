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
package org.ioc.context.factories.core;

import org.ioc.annotations.context.IoCDependency;
import org.ioc.context.model.ConstructorMetadata;
import org.ioc.context.type.IoCContext;
import org.ioc.exceptions.IoCInstantiateException;

import java.util.Arrays;

import static org.ioc.utils.ReflectionUtils.getOrigin;

/**
 * Class-tool for instantiate bag and initializing dependencies.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class InstanceFactory {
	private IoCContext context;

	public InstanceFactory(IoCContext context) {
		this.context = context;
	}

	public IoCContext getContext() {
		return context;
	}

	/**
	 * Head function for instantiate bag and inject dependencies.
	 *
	 * @param constructor metadata of bag
	 * @return instantiated object
	 */
	public Object instantiate(ConstructorMetadata constructor) {
		return instantiateFields(constructor.construct(getArgsFromContext(constructor.getParamTypes())));
	}

	/**
	 * Inject dependencies in a field's of bag.
	 *
	 * @param o bag for injection
	 * @return instantiated object
	 */
	private Object instantiateFields(Object o) {
		Arrays.stream(getOrigin(o.getClass()).getDeclaredFields())
				.forEach(field -> {
					if (field.isAnnotationPresent(IoCDependency.class)) {
						field.setAccessible(true);
						try {
							field.set(o, context.getType(field.getType()));
						} catch (IllegalAccessException e) {
							throw new IoCInstantiateException("IoCError - Unavailable create instance of bag [" + o.getClass() + "].", e);
						}
					}
				});
		return o;
	}

	/**
	 * Function of getting instantiated bag's in context.
	 *
	 * @param paramTypes types for find
	 * @return array of instantiated bag's
	 */
	private Object[] getArgsFromContext(Class<?>[] paramTypes) {
		return Arrays.stream(paramTypes).map(context::getType).toArray();
	}
}
