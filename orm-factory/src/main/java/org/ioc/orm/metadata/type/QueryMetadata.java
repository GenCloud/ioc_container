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
public class QueryMetadata implements Comparable<QueryMetadata> {
	private final Class<?> entity;
	private final String name;
	private final String query;

	public QueryMetadata(Class<?> entity, String name, String query) {
		Assertion.checkNotNull(name, "name null");
		Assertion.checkNotNull(query, "query null");
		Assertion.checkArgument(!name.isEmpty(), "blank name");
		Assertion.checkArgument(!query.isEmpty(), "query blank");

		this.name = name;
		this.query = query;
		this.entity = entity;
	}

	public Class<?> getEntity() {
		return entity;
	}

	public String getName() {
		return name;
	}

	public String getQuery() {
		return query;
	}

	@Override
	public int compareTo(QueryMetadata o) {
		return name.compareTo(o.name);
	}

	@Override
	public String toString() {
		return "{" + name + "} " + query;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		QueryMetadata queryMetadata = (QueryMetadata) o;

		if (!Objects.equals(entity, queryMetadata.entity)) {
			return false;
		}

		if (!Objects.equals(name, queryMetadata.name)) {
			return false;
		}

		return Objects.equals(query, queryMetadata.query);
	}

	@Override
	public int hashCode() {
		int result = entity != null ? entity.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (query != null ? query.hashCode() : 0);
		return result;
	}
}
