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
package org.ioc.orm.factory.orient.query;

import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientQuery {
	private final String jpaQuery;
	private final String orientQuery;

	public OrientQuery(String jpaQuery, String orientQuery) {
		this.jpaQuery = jpaQuery;
		this.orientQuery = orientQuery;
	}

	public String getQuery() {
		return orientQuery;
	}

	@Override
	public String toString() {
		return jpaQuery;
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

		if (!Objects.equals(jpaQuery, that.jpaQuery)) {
			return false;
		}

		return Objects.equals(orientQuery, that.orientQuery);
	}

	@Override
	public int hashCode() {
		int result = jpaQuery != null ? jpaQuery.hashCode() : 0;
		result = 31 * result + (orientQuery != null ? orientQuery.hashCode() : 0);
		return result;
	}
}
