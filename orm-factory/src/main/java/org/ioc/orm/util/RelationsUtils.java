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
package org.ioc.orm.util;

import net.sf.cglib.proxy.Enhancer;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.SessionFactory;
import org.ioc.orm.metadata.relation.BagMapper;
import org.ioc.orm.metadata.relation.proxy.LazyRelationHandler;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.relation.RelationVisitor;
import org.ioc.orm.metadata.visitors.relation.type.ManyToOneVisitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class RelationsUtils {
	private final static Map<Class<?>, HandlerContainer> hadlers = new ConcurrentHashMap<>();

	public static boolean isInitialized(Object o) {
		if (!isNotProxy(o)) {
			throw new OrmException("Type [" + o + "] is not a proxy!");
		}

		final HandlerContainer handlerContainer = hadlers.get(o.getClass());
		if (handlerContainer.getHandler() != null) {
			final LazyRelationHandler handler = handlerContainer.getHandler();
			return handler.isInitialized();
		}

		return false;
	}

	public static boolean isNotProxy(Object o) {
		if (o == null) {
			return false;
		}

		final HandlerContainer handlerContainer = hadlers.get(o.getClass());
		if (handlerContainer == null) {
			return false;
		}

		return handlerContainer.getHandler() != null;
	}

	public static Object createProxy(FacilityMetadata facilityMetadata, Object key,
									 SessionFactory sessionFactory, BagMapper bagMapper) {
		if (facilityMetadata == null) {
			return null;
		}

		final Class<?> clazz = facilityMetadata.getType(facilityMetadata.getIdVisitor().fromKey(key));
		HandlerContainer handlerContainer = hadlers.get(clazz);
		Object instance = null;
		if (handlerContainer == null) {
			final RelationVisitor visitor = new ManyToOneVisitor(key, sessionFactory, bagMapper);
			final LazyRelationHandler handler = new LazyRelationHandler(facilityMetadata, visitor, sessionFactory, clazz);
			final Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(clazz);
			enhancer.setUseCache(false);
			enhancer.setCallback(handler);
			enhancer.setCallbackFilter(m -> m.isBridge() ? 1 : 0);
			instance = enhancer.create();
			handlerContainer = new HandlerContainer(handler, instance);
			hadlers.put(clazz, handlerContainer);
		}

		if (instance == null) {
			instance = handlerContainer.getInstance();
		}

		return clazz.cast(instance);
	}

	private static class HandlerContainer {
		private final LazyRelationHandler handler;
		private final Object instance;

		private HandlerContainer(LazyRelationHandler handler, Object instance) {
			this.handler = handler;
			this.instance = instance;
		}

		Object getInstance() {
			return instance;
		}

		LazyRelationHandler getHandler() {
			return handler;
		}
	}
}
