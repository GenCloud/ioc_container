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

import org.ioc.context.factories.AbstractFactory;
import org.ioc.context.model.TypeMetadata;
import org.ioc.exceptions.IoCInstantiateException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.ioc.utils.ReflectionUtils.resolveTypeName;

/**
 * Factory of the prototype domain.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class PrototypeFactory extends AbstractFactory {
	private Map<String, TypeMetadata> typeMap = new ConcurrentHashMap<>();

	public PrototypeFactory(InstanceFactory instanceFactory) {
		super(instanceFactory);
	}

	public void addType(TypeMetadata type) {
		if (typeMap.containsKey(type.getName())) {
			throw new IoCInstantiateException();
		}

		typeMap.put(type.getName(), type);
	}

	@Override
	public Object getType(Class<?> type) {
		final Optional<Object> any = Optional.ofNullable(getType(resolveTypeName(type)));
		return any.orElse(null);
	}

	@Override
	public Object getType(String name) {
		final TypeMetadata typeMetadata = getTypes().get(name);
		if (typeMetadata == null) {
			return null;
		}

		return instanceFactory.instantiate(typeMetadata.getConstructor());
	}

	@Override
	public Map<String, TypeMetadata> getTypes() {
		return Collections.unmodifiableMap(typeMap);
	}
}
