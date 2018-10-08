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

import org.ioc.annotations.web.MappingMethod;

import java.lang.reflect.Constructor;

import static org.ioc.annotations.context.Mode.REQUEST;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class ControllerMetadata extends TypeMetadata {
	private final MappingMethod mappingMethod;
	private final String mappingPath;

	public ControllerMetadata(String name, Constructor constructor) {
		super(name, constructor, REQUEST);
		mappingMethod = MappingMethod.GET;
		mappingPath = "";
	}

	public ControllerMetadata(String name, Constructor constructor, MappingMethod mappingMethod, String mappingPath) {
		super(name, constructor, REQUEST);
		this.mappingMethod = mappingMethod;
		this.mappingPath = mappingPath;
	}

	public MappingMethod getMappingMethod() {
		return mappingMethod;
	}

	public String getMappingPath() {
		return mappingPath;
	}

	@Override
	public String toString() {
		return "ControllerMetadata{" + super.toString() +
				", mappingPath=" + mappingPath +
				", mappingMethod=" + mappingMethod +
				'}';
	}
}
