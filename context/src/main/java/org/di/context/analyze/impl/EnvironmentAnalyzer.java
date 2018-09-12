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
package org.di.context.analyze.impl;

import org.di.annotations.property.Property;
import org.di.annotations.property.PropertyFunction;
import org.di.factories.config.Analyzer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analyzer for detecting the methods of used annotation {@link PropertyFunction}
 * and check this methods for return type.
 *
 * @author GenCloud
 * @date 11.09.2018
 */
public class EnvironmentAnalyzer implements Analyzer<Boolean, Object> {
    @Override
    public Boolean analyze(Object tested) throws Exception {
        final Class<?> type = tested.getClass();
        final Method[] methods = type.getDeclaredMethods();
        if (methods.length > 0) {
            final List<Method> annotated = Arrays
                    .stream(methods)
                    .filter(m -> m.isAnnotationPresent(PropertyFunction.class))
                    .collect(Collectors.toList());
            if (!annotated.isEmpty()) {
                for (Method m : annotated) {
                    final Class<?> returnType = m.getReturnType();
                    if (returnType != Void.class && !returnType.isPrimitive()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean supportFor(Object tested) {
        return tested.getClass().isAnnotationPresent(Property.class);
    }
}
