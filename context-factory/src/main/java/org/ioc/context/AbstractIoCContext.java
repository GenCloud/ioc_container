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
package org.ioc.context;

import org.ioc.annotations.context.IoCComponent;
import org.ioc.aop.annotation.IoCAspect;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.type.IoCContext;
import org.ioc.context.type.IoCScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

import static org.ioc.utils.ReflectionUtils.*;

/**
 * @author GenCloud
 * @date 09/2018
 */
public abstract class AbstractIoCContext implements IoCContext, IoCScanner {
	final Logger log = LoggerFactory.getLogger(IoCContext.class);

	@Override
	public List<TypeMetadata> findMetadataInClassPathByAnnotations(List<Class<? extends Annotation>> annotations, String... packages) {
		final List<TypeMetadata> types = new LinkedList<>();

		findClassesByAnnotation(annotations, packages)
				.forEach(typeClass -> {
					if (checkType(typeClass)) {
						types.add(new TypeMetadata(resolveTypeName(typeClass), typeClass.getConstructors()[0], resolveLoadingMode(typeClass)));
					}
				});
		return types;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O> List<O> findInstancesInClassPathByInstance(Class<O> instance, String... packages) {
		final List<O> types = new LinkedList<>();

		findClassesByInstance(instance, packages)
				.forEach(typeClass -> {
					if (checkType(typeClass)) {
						types.add((O) instantiateClass(typeClass));
					}
				});
		return types;
	}

	@Override
	public List<TypeMetadata> findMetadataInClassPathByAnnotation(Class<? extends Annotation> annotation, String... packages) {
		final List<TypeMetadata> types = new LinkedList<>();

		findClassesByAnnotation(annotation, packages)
				.forEach(typeClass -> {
					if (checkType(typeClass)) {
						types.add(new TypeMetadata(resolveTypeName(typeClass), typeClass.getConstructors()[0], resolveLoadingMode(typeClass)));
					}
				});
		return types;
	}

	@Override
	public List<TypeMetadata> findMetadataInClassPath(String... packages) {
		final List<Class<? extends Annotation>> annotations = new LinkedList<>();
		final List<TypeMetadata> types = new LinkedList<>();
		annotations.add(IoCComponent.class);

		findClassesByAnnotation(annotations, packages)
				.forEach(typeClass -> {
					if (checkType(typeClass)) {
						types.add(new TypeMetadata(resolveTypeName(typeClass), typeClass.getConstructors()[0], resolveLoadingMode(typeClass)));
					}
				});
		return types;
	}

	@Override
	public List<Class<?>> findAspects(String... packages) {
		return findClassesByAnnotation(IoCAspect.class, packages);
	}
}
