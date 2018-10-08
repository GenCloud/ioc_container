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
package org.ioc.aop;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.type.IoCContext;

import java.util.Collections;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class LazyProxy implements LazyLoader {
	private final TypeMetadata metadata;
	private final IoCContext context;

	private LazyProxy(TypeMetadata metadata, IoCContext context) {
		this.metadata = metadata;
		this.context = context;
	}

	public static Object newProxyInstance(TypeMetadata metadata, IoCContext context) {
		final Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(metadata.getType());
		enhancer.setCallback(new LazyProxy(metadata, context));
		enhancer.setCallbackFilter(m -> m.isBridge() ? 1 : 0);
		return enhancer.create();
	}

	@Override
	public Object loadObject() {
		context.registerLazy(Collections.singletonList(metadata));
		return metadata.getInstance();
	}
}
