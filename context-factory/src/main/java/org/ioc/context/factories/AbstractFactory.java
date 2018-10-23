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
package org.ioc.context.factories;

import org.ioc.context.factories.core.InstanceFactory;
import org.ioc.context.model.TypeMetadata;
import org.ioc.exceptions.IoCInstantiateException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal storage structure of the generic AbstractFactory.
 * IoCContext is inherited to implement different scoped AbstractFactory.
 *
 * @author GenCloud
 * @date 09/2018
 */
public abstract class AbstractFactory {
	protected InstanceFactory instanceFactory;

	public AbstractFactory(InstanceFactory instanceFactory) {
		this.instanceFactory = instanceFactory;
	}

	protected TypeMetadata getMetadata(Class<?> type) {
		final List<TypeMetadata> collect = getTypes().values()
				.stream()
				.filter(t -> type.isAssignableFrom(t.getType()))
				.collect(Collectors.toList());

		if (collect.size() == 1) {
			return collect.get(0);
		} else if (collect.size() > 1) {
			throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "]. Found 2 or more instances in context!");
		}

		return null;
	}

	/**
	 * Return instantiated bag if founded.
	 *
	 * @param type bag for find
	 * @return instantiated bag
	 */
	public abstract Object getType(Class<?> type);

	/**
	 * Return instantiated bag if founded.
	 *
	 * @param name name of bag for find
	 * @return instantiated bag
	 */
	public abstract Object getType(String name);

	/**
	 * Return all metadata's of instantiated bag's.
	 *
	 * @return map of metadata's
	 */
	public abstract Map<String, TypeMetadata> getTypes();
}
