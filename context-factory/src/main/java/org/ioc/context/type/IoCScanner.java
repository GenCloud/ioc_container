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
package org.ioc.context.type;

import org.ioc.context.model.TypeMetadata;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Sample context classpath scanner.
 *
 * @author GenCloud
 * @date 09/2018
 */
public interface IoCScanner {
	/**
	 * Scan classpath for finding generated metadata's annotated with {@code annotations}.
	 *
	 * @param annotations annotation's for filter
	 * @param packages    for scanning
	 * @return filtered metadata's
	 */
	List<TypeMetadata> findMetadataInClassPathByAnnotations(List<Class<? extends Annotation>> annotations, String... packages);

	/**
	 * Scan classpath for finding types assignable {@code instance}.
	 *
	 * @param instance instance for filter
	 * @param packages for scanning
	 * @return filtered instances
	 */
	<O> List<O> findInstancesInClassPathByInstance(Class<O> instance, String... packages);

	/**
	 * Scan classpath for finding types annotated with {@code annotation}.
	 *
	 * @param annotation annotation for filter
	 * @param packages   for scanning
	 * @return filtered metadata's
	 */
	List<TypeMetadata> findMetadataInClassPathByAnnotation(Class<? extends Annotation> annotation, String... packages);

	/**
	 * Scan classpath for finding all generated metadata's.
	 *
	 * @param packages for scanning
	 * @return filtered metadata's
	 */
	List<TypeMetadata> findMetadataInClassPath(String... packages);

	/**
	 * Scan classpath for finding all classes annotated with {@link org.ioc.aop.annotation.IoCAspect}.
	 *
	 * @param packages for scanning
	 * @return filtered classes
	 */
	List<Class<?>> findAspects(String... packages);
}
