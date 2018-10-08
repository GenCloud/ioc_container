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
package org.ioc.orm.metadata.visitors.container.type;

import org.ioc.orm.metadata.visitors.container.DataContainer;

import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class BaseDataContainer implements DataContainer {
	private final Object value;

	public BaseDataContainer(Object value) {
		this.value = value;
	}

	@Override
	public boolean empty() {
		return value == null;
	}

	@Override
	public Object of() {
		return value;
	}

	@Override
	public String toString() {
		if (value != null) {
			return "Data{" + value + "}";
		} else {
			return "Data{null}";
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		BaseDataContainer that = (BaseDataContainer) o;

		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return value != null ? value.hashCode() : 0;
	}
}
