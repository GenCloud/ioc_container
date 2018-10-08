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
package org.ioc.orm.factory.orient.query;

import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientQuery {
	private final String javaPersistentQuery;
	private final String orientPersistentQuery;

	public OrientQuery(String javaPersistentQuery, String orientPersistentQuery) {
		this.javaPersistentQuery = javaPersistentQuery;
		this.orientPersistentQuery = orientPersistentQuery;
	}

	public String getQuery() {
		return orientPersistentQuery;
	}

	@Override
	public String toString() {
		return javaPersistentQuery;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		OrientQuery that = (OrientQuery) o;

		if (!Objects.equals(javaPersistentQuery, that.javaPersistentQuery)) {
			return false;
		}

		return Objects.equals(orientPersistentQuery, that.orientPersistentQuery);
	}

	@Override
	public int hashCode() {
		int result = javaPersistentQuery != null ? javaPersistentQuery.hashCode() : 0;
		result = 31 * result + (orientPersistentQuery != null ? orientPersistentQuery.hashCode() : 0);
		return result;
	}
}
