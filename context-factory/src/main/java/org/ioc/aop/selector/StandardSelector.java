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
package org.ioc.aop.selector;

import org.ioc.aop.selector.expression.AbstractMatcher;
import org.ioc.aop.selector.expression.impl.PointcutTokenizer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class StandardSelector {
	private static final String EXEC = "exec(* *.*(*))";
	private static final Map<Integer, Boolean> matchMap = new ConcurrentHashMap<>();
	private final String value;

	public StandardSelector(String value) {
		this.value = value;
	}

	public boolean isValidForAdvisor(Method method) {
		if (method == null || value == null || value.isEmpty()) {
			return false;
		}

		final int hash = calcHash(method, value);
		Boolean result = matchMap.get(hash);
		if (result == null) {
			final AbstractMatcher exp = new PointcutTokenizer(value).parseValues();
			result = exp.match(method);
			matchMap.put(hash, result);
		}
		return result;
	}

	private int calcHash(Method method, String exp) {
		return exp.hashCode() ^ method.hashCode();
	}
}
