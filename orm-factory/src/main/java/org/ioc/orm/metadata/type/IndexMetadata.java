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

import org.ioc.utils.Assertion;

import java.util.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class IndexMetadata {
	private final List<ColumnMetadata> metadataList;
	private final boolean unique;
	private final String name;

	public IndexMetadata(String name, Collection<ColumnMetadata> metadataList, boolean unique) {
		Assertion.checkNotNull(name, "name null");
		Assertion.checkArgument(!name.isEmpty(), "name empty");

		this.name = name;
		this.metadataList = new ArrayList<>(metadataList);
		this.unique = unique;
	}

	public List<ColumnMetadata> getMetadataList() {
		return Collections.unmodifiableList(metadataList);
	}

	public boolean isUnique() {
		return unique;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IndexMetadata indexMetadata = (IndexMetadata) o;

		if (unique != indexMetadata.unique) {
			return false;
		}

		if (!Objects.equals(metadataList, indexMetadata.metadataList)) {
			return false;
		}

		return Objects.equals(name, indexMetadata.name);
	}

	@Override
	public int hashCode() {
		int result = metadataList != null ? metadataList.hashCode() : 0;
		result = 31 * result + (unique ? 1 : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}
}
