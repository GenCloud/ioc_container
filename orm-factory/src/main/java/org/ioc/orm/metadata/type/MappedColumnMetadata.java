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

/**
 * @author GenCloud
 * @date 10/2018
 */
public class MappedColumnMetadata extends ColumnMetadata {
	private final EntityMetadata entityMetadata;
	private final ColumnMetadata columnMetadata;

	public MappedColumnMetadata(EntityMetadata entityMetadata, ColumnMetadata columnMetadata,
								final String name, String property, Class<?> clazz,
								final boolean isLazyLoading) {
		super(name, property, clazz, false, isLazyLoading, false);
		this.entityMetadata = entityMetadata;
		this.columnMetadata = columnMetadata;
	}

	public EntityMetadata getEntityMetadata() {
		return entityMetadata;
	}

	public ColumnMetadata getColumnMetadata() {
		return columnMetadata;
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}
}
