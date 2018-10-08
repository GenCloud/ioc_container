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
package org.ioc.orm.metadata.type;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class InheritMetadata {
	private final Object value;
	private final ColumnMetadata column;

	public InheritMetadata(ColumnMetadata columnMetadata, Serializable value) {
		this.column = columnMetadata;
		this.value = value;
	}

	public ColumnMetadata getColumn() {
		return column;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		InheritMetadata that = (InheritMetadata) o;

		if (!Objects.equals(column, that.column)) {
			return false;
		}

		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		int result = value != null ? value.hashCode() : 0;
		result = 31 * result + (column != null ? column.hashCode() : 0);
		return result;
	}
}
