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
package org.ioc.orm.property;

import org.ioc.orm.metadata.type.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class BasicPropertyFilter implements PropertyFilter {
	private final Set<String> ignored = new HashSet<>();

	public BasicPropertyFilter() {
		this(null);
	}

	public BasicPropertyFilter(Collection<String> c) {
		if (c != null && !c.isEmpty()) {
			ignored.addAll(c);
		}
	}

	@Override
	public boolean accept(EntityMetadata meta, ColumnMetadata column) {
		if (column == null) {
			return false;
		}

		if (ignored.contains(column.getName())) {
			return false;
		}

		if (ignored.contains(column.getProperty())) {
			return false;
		}

		if (column instanceof JoinBagMetadata) {
			return false;
		}

		if (column instanceof JoinColumnMetadata) {
			return false;
		}

		if (column instanceof MappedColumnMetadata) {
			return false;
		}

		return meta.hasColumn(column);
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 79 * hash + Objects.hashCode(ignored);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final BasicPropertyFilter other = (BasicPropertyFilter) obj;
		return Objects.equals(ignored, other.ignored);
	}
}
