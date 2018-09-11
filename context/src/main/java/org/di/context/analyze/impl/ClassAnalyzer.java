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

import org.di.annotations.IoCDependency;
import org.di.context.analyze.Analyzer;
import org.di.context.analyze.results.ClassAnalyzeResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.di.context.analyze.enums.ClassStateInjection.*;
import static org.di.utils.factory.ReflectionUtils.findConstructor;


/**
 * Analyzer for detecting the method of injection used.
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public class ClassAnalyzer implements Analyzer<ClassAnalyzeResult, Class<?>> {
    @Override
    public ClassAnalyzeResult analyze(Class<?> tested) {
        final Constructor<?> constructor = findConstructor(tested);
        if (constructor != null) {
            return new ClassAnalyzeResult(INJECTED_CONSTRUCTOR);
        }

        final Field[] fields = tested.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(IoCDependency.class)) {
                return new ClassAnalyzeResult(INJECTED_FIELDS);
            }
        }

        final Method[] methods = tested.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(IoCDependency.class)) {
                if (method.getParameterCount() == 0) {
                    return new ClassAnalyzeResult("Impossibility of injection into a function with fewer parameters than one");
                }

                if (method.getParameterCount() > 1) {
                    return new ClassAnalyzeResult("Inability to inject a function with more than one parameter");
                }

                return new ClassAnalyzeResult(INJECTED_METHODS);
            }
        }
        return new ClassAnalyzeResult(INJECTED_NOTHING);
    }

    @Override
    public boolean supportFor(Class<?> tested) {
        return !tested.isAnnotation() & !tested.isArray() && !tested.isEnum();
    }
}