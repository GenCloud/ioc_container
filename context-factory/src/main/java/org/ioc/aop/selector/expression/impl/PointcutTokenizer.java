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
import org.ioc.aop.selector.expression.util.ExecutionPointcut;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class PointcutTokenizer {
	private final int total;
	private final String expression;
	private int position;

	public PointcutTokenizer(String expression) {
		this.expression = expression;
		this.total = expression.length();
		position = 0;
	}

	public AbstractMatcher parseValues() {
		if (position >= total) {
			return null;
		}

		final boolean annotated = expression.charAt(0) == '@';
		if (annotated) {
			position++;
		}

		final StringBuilder builder = new StringBuilder();
		do {
			final char ch = expression.charAt(position++);
			if (ch == '(') {
				break;
			}

			builder.append(ch);
		} while (position < total);

		int end = total;
		do {
			final char ch = expression.charAt(--end);
			if (ch == ')') {
				break;
			}
		}
		while (end > position);

		if (end == position) {
			return new NoneMatchPointcut();
		}

		final String expName = builder.toString().toLowerCase();
		final String exp = expression.substring(position, end);
		switch (expName) {
			case "*":
				return new AnyMatcher();
			case "this":
				return new DeclaringClassPointcut(exp);
			case "args":
				return new ArgumentMatcher(annotated, exp);
			case "target":
				return new TargetMatcher(annotated, exp);
			case "within":
				return new WithinMatcher(annotated, exp);
			case "annotation":
				return new AnnotationMatcher(annotated, exp);
			case "exec":
				return new ExecutionPointcut(exp);
			default:
				return new NoneMatchPointcut();
		}
	}
}
