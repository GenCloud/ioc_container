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
package org.ioc.orm.metadata.visitors.column.factory;

import org.ioc.orm.metadata.relation.mapper.BaseBagMapper;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.column.ColumnVisitorFactory;
import org.ioc.orm.metadata.visitors.column.type.BaseColumnVisitor;
import org.ioc.orm.metadata.visitors.column.type.ManyJoinColumnVisitor;
import org.ioc.orm.metadata.visitors.column.type.SingleJoinColumnVisitor;

import java.lang.reflect.Field;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class BaseColumnVisitorFactory implements ColumnVisitorFactory {
	@Override
	public ColumnVisitor of(Field field, Class<?> clazz) {
		return new BaseColumnVisitor(field, clazz);
	}

	@Override
	public ColumnVisitor manyVisit(Field field, FacilityMetadata facilityMetadata, boolean isLazyLoading) {
		return new ManyJoinColumnVisitor(field, facilityMetadata, isLazyLoading, new BaseBagMapper(facilityMetadata));
	}

	@Override
	public ColumnVisitor singleVisit(Field field, FacilityMetadata facilityMetadata, boolean isLazyLoading) {
		return new SingleJoinColumnVisitor(field, facilityMetadata, isLazyLoading, new BaseBagMapper(facilityMetadata));
	}
}
