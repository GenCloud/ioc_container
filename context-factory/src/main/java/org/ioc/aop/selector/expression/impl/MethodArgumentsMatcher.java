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
package org.ioc.aop.selector.expression.impl;

import org.ioc.aop.selector.expression.AbstractMatcher;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class MethodArgumentsMatcher extends AbstractMatcher {
	public MethodArgumentsMatcher(String exp) {
		super(exp);
	}

	@Override
	protected boolean isMatch(Method method) {
		final Parameter[] parameters = method.getParameters();
		if (parameters.length == 0 && (getValue() == null || getValue().equals(""))) {
			return true;
		}

		final String[] array = getValue().split(",");
		if (array.length != parameters.length) {
			return false;
		}

		boolean result = true;
		for (int i = 0; i < array.length; i++) {
			final Type type = parameters[i].getParameterizedType();
			final String typeFullName = type.getTypeName();
			if (type instanceof Class) {
				final int index = typeFullName.lastIndexOf('.');
				final String sub = typeFullName.substring(index + 1);
				result &= valueEquals(array[i], sub);
				continue;
			}

			result &= valueEquals(array[i], typeFullName);
		}
		return result;
	}
}
