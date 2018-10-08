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
package org.ioc.context.model;

import org.ioc.annotations.context.Mode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * {@link org.ioc.context.type.IoCContext} internal bag storage structure.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class TypeMetadata {
	/**
	 * Name of bag.
	 */
	private String name;

	/**
	 * Types of bag.
	 */
	private Class<?> type;

	/**
	 * Unified constructor metadata.
	 */
	private ConstructorMetadata constructor;

	/**
	 * Instance can be cached.
	 */
	private Object instance;

	/**
	 * Loading mode
	 */
	private Mode mode;

	private boolean initialized = false;

	public TypeMetadata(String name, Constructor constructor, Mode mode) {
		this.name = name;
		this.constructor = new ConstructorMetadata(constructor);
		this.type = constructor.getDeclaringClass();
		this.mode = mode;
	}

	public TypeMetadata(String name, Object o, Method method, Mode mode) {
		this.name = name;
		this.constructor = new ConstructorMetadata(o, method);
		this.type = method.getReturnType();
		this.mode = mode;
	}

	public TypeMetadata(String name, Object instance, Mode mode) {
		this.name = name;
		this.instance = instance;
		this.type = instance.getClass();
		this.mode = mode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public ConstructorMetadata getConstructor() {
		return constructor;
	}

	public void setConstructor(ConstructorMetadata constructor) {
		this.constructor = constructor;
	}

	public Object getInstance() {
		return instance;
	}

	public void setInstance(Object instance) {
		this.instance = instance;
	}

	public Mode getMode() {
		return mode;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	@Override
	public String toString() {
		return "TypeMetadata{" +
				"name='" + name + '\'' +
				", bag=" + type +
				", constructor=" + constructor +
				", instance=" + instance +
				", mode=" + mode + super.toString() +
				'}';
	}
}
