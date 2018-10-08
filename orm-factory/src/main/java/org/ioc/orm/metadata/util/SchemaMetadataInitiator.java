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
package org.ioc.orm.metadata.util;

import org.ioc.orm.metadata.inspectors.FacilityMetadataInspector;
import org.ioc.orm.metadata.type.SchemaMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitorFactory;
import org.ioc.orm.metadata.visitors.column.factory.BaseColumnVisitorFactory;
import org.ioc.utils.Assertion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SchemaMetadataInitiator {
	private final Set<Class<?>> classes = new HashSet<>();

	private ColumnVisitorFactory columnVisitorFactory = new BaseColumnVisitorFactory();

	public SchemaMetadata install() {
		final FacilityMetadataInspector analyzer = new FacilityMetadataInspector(columnVisitorFactory, classes);
		return new SchemaMetadata(analyzer.inspect());
	}

	public SchemaMetadataInitiator withColumnFactory(ColumnVisitorFactory factory) {
		this.columnVisitorFactory = factory;
		return this;
	}

	private SchemaMetadataInitiator withClass(Class clazz) {
		Assertion.checkNotNull(clazz, "class");

		classes.add(clazz);
		return this;
	}

	public SchemaMetadataInitiator withClasses(Class... classes) {
		Assertion.checkNotNull(classes, "classes");

		Arrays.stream(classes).forEach(this::withClass);
		return this;
	}

	public SchemaMetadataInitiator withClasses(Iterable<Class> iterable) {
		Assertion.checkNotNull(iterable, "iterator");

		for (Class<?> clazz : iterable) {
			withClass(clazz);
		}
		return this;
	}
}
