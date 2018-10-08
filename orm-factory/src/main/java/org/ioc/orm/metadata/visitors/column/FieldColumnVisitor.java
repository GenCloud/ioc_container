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
package org.ioc.orm.metadata.visitors.column;

import org.ioc.utils.Assertion;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public abstract class FieldColumnVisitor {
	private final Field field;

	protected FieldColumnVisitor(Field field) {
		Assertion.checkNotNull(field, "field");

		this.field = field;
		field.setAccessible(true);
	}

	protected Field getRawField() {
		return field;
	}

	protected final boolean setValue(Object entity, Object value) throws IllegalAccessException {
		if (entity == null) {
			return false;
		}

		if (!field.getDeclaringClass().isInstance(entity)) {
			return false;
		}

		field.set(entity, value);
		return true;
	}

	protected final Object getValue(Object entity) throws IllegalAccessException {
		if (entity == null) {
			return null;
		}

		if (!field.getDeclaringClass().isInstance(entity)) {
			return null;
		}

		return field.get(entity);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		FieldColumnVisitor that = (FieldColumnVisitor) o;

		return Objects.equals(field, that.field);
	}

	@Override
	public int hashCode() {
		return field != null ? field.hashCode() : 0;
	}
}
