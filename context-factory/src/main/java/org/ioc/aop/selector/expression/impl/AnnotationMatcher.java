/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of DI (IoC) Container Project.
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
package org.ioc.aop.selector.expression.impl;

import org.ioc.aop.selector.expression.AbstractMatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class AnnotationMatcher extends AbstractMatcher {
	private final boolean annotated;

	AnnotationMatcher(boolean annotated, String value) {
		super(value);
		this.annotated = annotated;
	}

	public boolean isAnnotated() {
		return annotated;
	}

	@SuppressWarnings("unchecked")
	Class<? extends Annotation> annotationType() {
		if (getValue() == null || getValue().isEmpty()) {
			return null;
		}

		try {
			return (Class<? extends Annotation>) getClass().getClassLoader().loadClass(getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected boolean isMatch(Method method) {
		final Class<? extends Annotation> annotationType = annotationType();
		if (annotationType != null && annotationType.isAnnotation()) {
			return method.isAnnotationPresent(annotationType);
		}
		return false;
	}
}
