/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of IoC Starter Project.
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
package org.ioc.web.model.mapping;

import io.netty.handler.codec.http.HttpMethod;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.type.IoCContext;
import org.ioc.web.annotations.PathVariable;
import org.ioc.web.model.http.RequestEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class MappingContainer {
	private final Logger log = LoggerFactory.getLogger(MappingContainer.class);

	private final Map<Pattern, Mapping> mappingMap = new HashMap<>();

	private IoCContext context;

	public void addMapping(IoCContext context, String path, Method method, Mapping mapping) {
		if (this.context == null) {
			this.context = context;
		}

		final Parameter[] parameters = method.getParameters();
		for (Parameter parameter : parameters) {
			final PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
			if (pathVariable != null) {
				if (parameter.getType() == String.class) {
					path = path.replace("{" + pathVariable.value() + "}", "[0-9\\d\\D]*");
				} else if (parameter.getType() == Integer.class
						|| parameter.getType() == Long.class) {
					path = path.replace("{" + pathVariable.value() + "}", "[0-9]*");
				}
			}
		}

		final Pattern pattern = Pattern.compile(path);

		final Mapping map = mappingMap.get(pattern);
		if (map != null) {
			log.info("Found duplicate method [{}] -> [{}]", map.getMethod(), mapping.getMethod());
		} else {
			mappingMap.put(pattern, mapping);
			log.info("Mapped method [{}], method=[{}], to [{}]", mapping.getPath(), mapping.getHttpMethod(), mapping.getMethod());
		}
	}

	public Mapping findMapping(RequestEntry requestEntry) {
		final String path = requestEntry.getPath();
		final HttpMethod method = requestEntry.getHttpMethod();

		int splitIndex = path.indexOf('?');
		if (splitIndex == -1) {
			splitIndex = path.length();
		}

		if (path.length() != 1) {
			if (path.charAt(splitIndex - 1) == '/') {
				splitIndex--;
			}
		}

		for (Pattern pattern : mappingMap.keySet()) {
			if (pattern.matcher(path.substring(0, splitIndex)).matches()) {
				final Mapping mapping = mappingMap.get(pattern);
				if (mapping.getHttpMethod().equals(method)) {
					final TypeMetadata metadata = mapping.getMetadata();
					Object instance = context.getType(metadata.getType());
					mapping.setInstance(instance);
					return mapping;
				}
			}
		}
		return null;
	}
}
