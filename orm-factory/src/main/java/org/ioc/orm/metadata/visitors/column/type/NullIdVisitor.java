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
package org.ioc.orm.metadata.visitors.column.type;

import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.visitors.column.IdVisitor;

import java.util.Collections;
import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class NullIdVisitor implements IdVisitor {
	private static final NullIdVisitor instance = new NullIdVisitor();

	public static IdVisitor getInstance() {
		return NullIdVisitor.instance;
	}

	@Override
	public Object fromObject(Object entity) {
		return null;
	}

	@Override
	public Map<ColumnMetadata, Object> fromKey(Object key) {
		return Collections.emptyMap();
	}

	@Override
	public Object ofKey(Map<ColumnMetadata, Object> map) {
		return null;
	}
}
