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
package org.ioc.aop.selector.expression;

import org.ioc.aop.selector.expression.impl.ClassMethodMatcher;
import org.ioc.aop.selector.expression.impl.FullClassNameMather;
import org.ioc.aop.selector.expression.impl.MethodArgumentsMatcher;
import org.ioc.aop.selector.expression.impl.ReturnTypeMatcher;
import org.ioc.aop.selector.expression.util.AnalyzeType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GenCloud
 * @date 09/2018
 */
public abstract class AbstractMatcher {
	private final String value;
	private int position, total;
	private boolean parsed;
	private AnalyzeType analyzeType;
	private List<AbstractMatcher> matchers;

	protected AbstractMatcher(String value) {
		this.value = value;
	}

	protected List<AbstractMatcher> getMatchers() {
		return matchers;
	}

	protected String getValue() {
		return value;
	}

	public boolean match(Method method) {
		if (value != null && value.equals("*")) {
			return true;
		}

		return isMatch(method);
	}

	protected boolean valueEquals(String value, String toEquals) {
		if (value == null || value.equals("*")) {
			return true;
		}

		String matchName;
		if (value.startsWith("*")) {
			matchName = value.substring(1, value.length() - 1);
			return toEquals.endsWith(matchName);
		} else if (value.endsWith("*")) {
			matchName = value.substring(0, value.length() - 1);
			return toEquals.startsWith(matchName);
		}

		return value.equalsIgnoreCase(toEquals);
	}

	protected void readValue() {
		if (parsed) {
			return;
		}

		analyzeType = AnalyzeType.RETURN_TYPE;
		parsed = true;
		matchers = new ArrayList<>();
		position = 0;
		total = value.length();

		parseValues();
	}

	private void parseValues() {
		AbstractMatcher matcher = readValues();
		while (matcher != null) {
			matchers.add(matcher);
			matcher = readValues();
		}
	}

	private AbstractMatcher readValues() {
		if (position >= total) {
			return null;
		}

		if (analyzeType == AnalyzeType.RETURN_TYPE) {
			analyzeType = AnalyzeType.PACKAGE_NAME;
			return readMethodReturn();
		} else if (analyzeType == AnalyzeType.PACKAGE_NAME) {
			analyzeType = AnalyzeType.METHOD_NAME;
			return readFullClassName();
		} else if (analyzeType == AnalyzeType.METHOD_NAME) {
			analyzeType = AnalyzeType.ARGUMENT;
			return readClassMethod();
		} else if (analyzeType == AnalyzeType.ARGUMENT) {
			analyzeType = AnalyzeType.END;
			return readMethodArguments();
		} else {
			return null;
		}
	}

	private AbstractMatcher readMethodReturn() {
		final StringBuilder builder = new StringBuilder();
		do {
			final char ch = value.charAt(position++);
			if (ch == ' ') {
				break;
			}

			builder.append(ch);
		} while (position < total);

		return new ReturnTypeMatcher(builder.toString());
	}

	private AbstractMatcher readFullClassName() {
		final StringBuilder builder = new StringBuilder();
		do {
			final char ch = value.charAt(position++);
			if (ch == '(') {
				break;
			}

			builder.append(ch);
		} while (position < total);

		int len = builder.length();
		while (len > 0) {
			final char ch = builder.charAt(--len);
			--position;
			if (ch == '.') {
				break;
			}
		}

		final String sub = builder.substring(0, len);
		return new FullClassNameMather(sub);
	}

	private AbstractMatcher readClassMethod() {
		final StringBuilder builder = new StringBuilder();
		do {
			final char ch = value.charAt(position++);
			if (ch == '(') {
				break;
			}

			builder.append(ch);
		} while (position < total);

		return new ClassMethodMatcher(builder.toString());
	}

	private AbstractMatcher readMethodArguments() {
		final StringBuilder builder = new StringBuilder();
		do {
			final char ch = value.charAt(position++);
			if (ch == ')') {
				break;
			}

			builder.append(ch);
		} while (position < total);

		return new MethodArgumentsMatcher(builder.toString());
	}

	protected abstract boolean isMatch(Method method);
}
