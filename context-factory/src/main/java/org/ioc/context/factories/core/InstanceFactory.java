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
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.type.IoCContext;
import org.ioc.exceptions.IoCInstantiateException;

import java.util.*;
import java.util.stream.Collectors;

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
		Class<?> type = o.getClass();
		do {
			final Class<?> origin = getOrigin(type);
			if (origin != null) {
				Arrays.stream(origin.getDeclaredFields())
						.forEach(field -> {
							final IoCDependency ioCDependency = field.getAnnotation(IoCDependency.class);
							if (ioCDependency != null) {
								field.setAccessible(true);

								try {
									Object instance;
									if (!ioCDependency.value().isEmpty()) {
										instance = context.getType(ioCDependency.value());
										if (instance == null) {
											final List<TypeMetadata> types = context.getTypes();
											final Optional<TypeMetadata> finded = types.stream()
													.filter(t -> Objects.equals(t.getName(), ioCDependency.value()))
													.findFirst();
											if (finded.isPresent()) {
												instance = context.registerTypes(Collections.singletonList(finded.get())).get(0).getInstance();
											} else {
												throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + field.getType() + "]. Cant find type with specified qualified name [" + ioCDependency.value() + "].");
											}
										}
									} else {
										instance = context.getType(field.getType());
										if (instance == null) {
											final List<TypeMetadata> types = context.getTypes();
											final List<TypeMetadata> finded = types.stream()
													.filter(t -> field.getType().isAssignableFrom(t.getType()))
													.collect(Collectors.toList());
											if (finded.size() == 1) {
												instance = context.registerTypes(Collections.singletonList(finded.get(0))).get(0).getInstance();
											} else if (finded.size() > 1) {
												throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + field.getType() + "]. Found 2 or more instances in context! Use qualified name of type in @IoCDependency");
											} else {
												throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + field.getType() + "].");
											}
										}
									}

									field.set(o, instance);
								} catch (IllegalAccessException e) {
									throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + o.getClass() + "].", e);
								}
							}
						});
			}

			type = type.getSuperclass();
		}
		while (type != Object.class && !type.isInterface());
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
