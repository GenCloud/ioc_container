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

import java.util.Collection;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class ColumnMetadata implements Comparable<ColumnMetadata> {
	private final String name;
	private final String property;
	private final Class<?> type;

	private final boolean primaryKey;
	private final boolean isLazyLoading;
	private final boolean isJsonString;

	public ColumnMetadata(String name, String property, Class<?> type, boolean primaryKey,
						  final boolean isLazyLoading, boolean isJsonString) {
		Assertion.checkNotNull(name, "name");
		Assertion.checkNotNull(property, "property");
		Assertion.checkNotNull(type, "class");

		this.name = name;
		this.property = property;
		this.type = type;
		this.primaryKey = primaryKey;
		this.isLazyLoading = isLazyLoading;
		this.isJsonString = isJsonString;
	}

	/**
	 * @return the name of the column in the databaseDocument
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the name of the property (ie class field) within the entityMetadata
	 */
	public String getProperty() {
		return this.property;
	}

	public Class<?> getType() {
		return this.type;
	}

	public boolean isBag() {
		return Collection.class.isAssignableFrom(this.type);
	}

	public boolean isEmbedded() {
		return true;
	}

	public boolean isPrimaryKey() {
		return this.primaryKey;
	}

	public boolean isLazyLoaded() {
		return this.isLazyLoading;
	}

	public boolean isJsonString() {
		return this.isJsonString;
	}

	@Override
	public int compareTo(ColumnMetadata columnMetadata) {
		if (columnMetadata == null) {
			return 1;
		}

		return name.compareToIgnoreCase(columnMetadata.name);
	}

	@Override
	public String toString() {
		return name + " (" + type.getSimpleName() + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ColumnMetadata that = (ColumnMetadata) o;

		if (primaryKey != that.primaryKey) {
			return false;
		}
		if (isLazyLoading != that.isLazyLoading) {
			return false;
		}
		if (isJsonString != that.isJsonString) {
			return false;
		}
		if (!name.equals(that.name)) {
			return false;
		}
		if (!property.equals(that.property)) {
			return false;
		}
		return type.equals(that.type);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + property.hashCode();
		result = 31 * result + type.hashCode();
		result = 31 * result + (primaryKey ? 1 : 0);
		result = 31 * result + (isLazyLoading ? 1 : 0);
		result = 31 * result + (isJsonString ? 1 : 0);
		return result;
	}
}
