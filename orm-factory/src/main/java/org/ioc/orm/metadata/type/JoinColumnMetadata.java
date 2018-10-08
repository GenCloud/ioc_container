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

/**
 * @author GenCloud
 * @date 10/2018
 */
public class JoinColumnMetadata extends ColumnMetadata {
	private final FacilityMetadata facilityMetadata;

	public JoinColumnMetadata(String name, String property, Class<?> clazz,
							  final FacilityMetadata facilityMetadata, boolean primaryKey) {
		super(name, property, clazz, primaryKey, false, false);
		Assertion.checkNotNull(facilityMetadata, "entity metadata");

		this.facilityMetadata = facilityMetadata;
	}

	public FacilityMetadata getFacilityMetadata() {
		return facilityMetadata;
	}
}
