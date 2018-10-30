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

import org.ioc.annotations.context.Lazy;
import org.ioc.aop.LazyProxy;
import org.ioc.context.factories.AbstractFactory;
import org.ioc.context.model.TypeMetadata;
import org.ioc.exceptions.IoCInstantiateException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Factory for singleton domain.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class SingletonFactory extends AbstractFactory {
	private Map<String, TypeMetadata> typeMap = new ConcurrentHashMap<>();

	private Map<String, TypeMetadata> lazyMap = new ConcurrentHashMap<>();

	public SingletonFactory(InstanceFactory instanceFactory) {
		super(instanceFactory);
	}

	public void addType(TypeMetadata type) {
		final String typeName = type.getName();
		if (!type.isInitialized() && type.getType().isAnnotationPresent(Lazy.class)) {
			type.setInstance(LazyProxy.newProxyInstance(type, instanceFactory.getContext()));
			lazyMap.put(typeName, type);
			return;
		}

		if (!typeMap.containsKey(typeName)) {
			if (type.getInstance() == null) {
				type.setInstance(instanceFactory.instantiate(type.getConstructor()));
			}

			typeMap.put(typeName, type);
		}
	}

	@Override
	public Object getType(Class<?> type) {
		final TypeMetadata metadata = getMetadata(type);
		if (metadata != null) {
			return metadata.getInstance();
		} else {
			final List<TypeMetadata> lazy = lazyMap.values()
					.stream()
					.filter(t -> type.isAssignableFrom(t.getType()))
					.collect(Collectors.toList());
			if (lazy.size() == 1) {
				final TypeMetadata component = lazy.get(0);
				lazyMap.remove(component.getName());
				return component.getInstance();
			} else if (lazy.size() > 1) {
				throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "]. " +
						"Found 2 or more instances in context! Use type-name on component.");
			}
		}

		return null;
	}

	@Override
	public Object getType(String name) {
		final TypeMetadata typeMetadata = getTypes().get(name);
		if (typeMetadata == null) {
			return null;
		}

		return typeMetadata.getInstance();
	}

	@Override
	public Map<String, TypeMetadata> getTypes() {
		return Collections.unmodifiableMap(typeMap);
	}
}
