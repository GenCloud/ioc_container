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
package org.ioc.orm.repositories.proxy;

import net.sf.cglib.proxy.InvocationHandler;
import org.ioc.orm.annotations.Query;
import org.ioc.orm.exceptions.RepositoryInvocationException;
import org.ioc.orm.exceptions.RepositoryMappingException;
import org.ioc.orm.factory.SchemaQuery;
import org.ioc.orm.factory.facility.FacilityManager;
import org.ioc.orm.interpretator.MethodsAnalyzer;
import org.ioc.orm.metadata.transaction.Tx;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.type.QueryMetadata;
import org.ioc.orm.repositories.ProxyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transactional;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class RepositoryHandler implements InvocationHandler {
	private static final Logger log = LoggerFactory.getLogger(RepositoryHandler.class);
	private static final Collection<String> ignoredMethods = Arrays.asList("equals", "hashCode", "toString");
	private final FacilityManager facilityManager;
	private final Class<?> entityClass;
	private final FacilityMetadata facilityMetadata;
	private final ProxyRepository repository;
	private final MethodsAnalyzer methodsAnalyzer;
	private final boolean logQueries;

	public RepositoryHandler(FacilityManager facilityManager, Class<?> entityClass, FacilityMetadata facilityMetadata,
							 ProxyRepository repository, boolean logQueries) {
		this.facilityManager = facilityManager;
		this.entityClass = entityClass;
		this.facilityMetadata = facilityMetadata;
		this.repository = repository;

		methodsAnalyzer = new MethodsAnalyzer(facilityMetadata);
		this.logQueries = logQueries;
	}

	@Override
	public Object invoke(Object o, Method method, Object[] args) {
		if (method.isAnnotationPresent(Transactional.class)) {
			if (log.isDebugEnabled()) {
				log.debug("Intercepting proxy method invocation under transaction for repository type [{}].", repository.getClass().getSimpleName());
			}

			try (Tx tx = facilityManager.openTx()) {
				final Object invoked = analyzeAndInvoke(method, args);
				tx.success();
				return invoked;
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Intercepting proxy method invocation for repository type [{}].", repository.getClass().getSimpleName());
			}

			return analyzeAndInvoke(method, args);
		}
	}

	private Object analyzeAndInvoke(Method method, Object[] args) {
		final boolean skip = ignoredMethods.stream().anyMatch(ignore -> method.getName().equals(ignore));
		final Method fastMethod;
		try {
			fastMethod = repository.getClass().getMethod(method.getName(), method.getParameterTypes());
			if (!skip && fastMethod != null) {
				try {
					return fastMethod.invoke(repository, args);
				} catch (ReflectiveOperationException e) {
					throw new RepositoryInvocationException(e);
				}
			}
		} catch (NoSuchMethodException e) {
			boolean isList = false;

			final Type type = method.getGenericReturnType();
			if (type instanceof ParameterizedType) {
				isList = true;
				final ParameterizedType paramType = (ParameterizedType) type;
				final Type[] argTypes = paramType.getActualTypeArguments();
				if (argTypes.length > 0) {
					if (!entityClass.equals(argTypes[0])) {
						throw new RepositoryMappingException("Not compatible types: " + entityClass.toGenericString() + " and " + argTypes[0]);
					}
				}
			} else {
				if (!entityClass.equals(type)) {
					throw new RepositoryMappingException("Not compatible types: " + entityClass.toGenericString() + " and " + type);
				}
			}

			if (method.isAnnotationPresent(Query.class)) {
				final Query namedQuery = method.getAnnotation(Query.class);
				final String query = namedQuery.name();
				if (!query.isEmpty()) {
					final String[] hints = namedQuery.params();

					SchemaQuery schemaQuery;
					final Optional<QueryMetadata> opt = facilityMetadata.getQueryMetadataCollection()
							.stream()
							.filter(q -> q.getName().equalsIgnoreCase(query))
							.findFirst();
					if (opt.isPresent()) {
						if (hints.length > 0) {
							final Map<String, Object> params = new HashMap<>();
							final String name = opt.get().getName();
							for (int i = 0; i < hints.length; i++) {
								params.put(hints[i], args[i]);
							}

							schemaQuery = repository.executePreparedQuery(name, params);
						} else {
							schemaQuery = repository.executePreparedQueryWithoutParams(query);
						}
					} else {
						throw new RepositoryMappingException("Could not get NamedQuery in Schema - " + query);
					}

					return resolveResult(isList, schemaQuery);
				}
			}

			final String query = methodsAnalyzer.toQuery(method, args);
			if (query != null) {
				if (logQueries) {
					log.info(query);
				}

				final SchemaQuery schemaQuery = repository.executePreparedQueryWithoutParams(query);
				return resolveResult(isList, schemaQuery);
			}
		}

		return null;
	}

	private Object resolveResult(boolean isList, SchemaQuery schemaQuery) {
		if (!isList) {
			final Object o = schemaQuery.first();
			if (o != null) {
				facilityMetadata.invokePostLoad(o);
			}
			return o;
		} else {
			final Collection<?> list = schemaQuery.list();
			if (list != null) {
				list.forEach(facilityMetadata::invokePostLoad);
			}
			return list;
		}
	}
}
