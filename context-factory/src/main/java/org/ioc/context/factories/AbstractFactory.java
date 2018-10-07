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
package org.ioc.context.factories;

import org.ioc.context.model.TypeMetadata;

import java.util.Map;

/**
 * Internal storage structure of the generic AbstractFactory.
 * IoCContext is inherited to implement different scoped AbstractFactory.
 *
 * @author GenCloud
 * @date 09/2018
 */
public abstract class AbstractFactory {
	protected InstanceFactory instanceFactory;

	AbstractFactory(InstanceFactory instanceFactory) {
		this.instanceFactory = instanceFactory;
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
