/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of DI (IoC) Container Project.
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
package org.ioc.web.model.mapping;

import io.netty.handler.codec.http.HttpMethod;
import org.ioc.context.model.TypeMetadata;

import java.lang.reflect.Method;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class Mapping {
	private final Method method;
	private final HttpMethod httpMethod;
	private final String path;

	private String produces;

	private String consumes;

	private Object instance;

	private boolean view = false;

	private Object[] parameters;

	private TypeMetadata metadata;

	public Mapping(Object instance, Method method, HttpMethod httpMethod, String path) {
		this.instance = instance;
		this.method = method;
		this.httpMethod = httpMethod;
		this.path = path;
	}

	public void setIsView() {
		this.view = true;
	}

	public boolean isView() {
		return view;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	public void setParameter(Object parameter, int pos) {
		parameters[pos] = parameter;
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public String getPath() {
		return path;
	}

	public String getProduces() {
		return produces;
	}

	public void setProduces(String produces) {
		this.produces = produces;
	}

	public String getConsumes() {
		return consumes;
	}

	public void setConsumes(String consumes) {
		this.consumes = consumes;
	}

	public Object getInstance() {
		return instance;
	}

	public void setInstance(Object instance) {
		this.instance = instance;
	}

	public Method getMethod() {
		try {
			return metadata.getType().getDeclaredMethod(method.getName(), method.getParameterTypes());
		} catch (NoSuchMethodException e) {
			return method;
		}
	}

	public TypeMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(TypeMetadata metadata) {
		this.metadata = metadata;
	}
}
