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
package org.ioc.orm.metadata.type;

import org.ioc.utils.Assertion;

import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class EmbeddedBagMetadata extends ColumnMetadata {
	private final Class<?> type;

	public EmbeddedBagMetadata(String name, String property, Class<?> clazz, Class<?> type,
							   boolean primary, boolean isLazyLoading, boolean isJson) {
		super(name, property, clazz, primary, isLazyLoading, isJson);
		Assertion.checkNotNull(clazz, "class");

		this.type = type;
	}

	@Override
	public boolean isBag() {
		return true;
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		if (!super.equals(o)) {
			return false;
		}

		EmbeddedBagMetadata that = (EmbeddedBagMetadata) o;

		return Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}
}
