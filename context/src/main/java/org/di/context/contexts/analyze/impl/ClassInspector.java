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
package org.di.context.contexts.analyze.impl;

import org.di.context.annotations.IoCDependency;
import org.di.context.annotations.Lazy;
import org.di.context.contexts.analyze.enums.ClassStateInjection;
import org.di.context.contexts.analyze.results.ClassInspectionResult;
import org.di.context.factories.config.Inspector;
import org.di.context.utils.factory.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


/**
 * Inspector for detecting the method of injection used.
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public class ClassInspector implements Inspector<ClassInspectionResult, Class<?>> {
    @Override
    public ClassInspectionResult inspect(Class<?> tested) {
        return inspect(tested, false);
    }

    public ClassInspectionResult inspect(Class<?> tested, boolean ignoreLazy) {
        if (!ignoreLazy) {
            if (tested.isAnnotationPresent(Lazy.class)) {
                return new ClassInspectionResult(ClassStateInjection.LAZY_INITIALIZATION);
            }
        }

        final Constructor<?> constructor = ReflectionUtils.findConstructor(tested);
        if (constructor != null) {
            return new ClassInspectionResult(ClassStateInjection.INJECTED_CONSTRUCTOR);
        }

        final Field[] fields = tested.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(IoCDependency.class)) {
                return new ClassInspectionResult(ClassStateInjection.INJECTED_FIELDS);
            }
        }

        final Method[] methods = tested.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(IoCDependency.class)) {
                if (method.getParameterCount() == 0) {
                    return new ClassInspectionResult("Impossibility of injection into a function with fewer parameters than one");
                }

                if (method.getParameterCount() > 1) {
                    return new ClassInspectionResult("Inability to inject a function with more than one parameter");
                }

                return new ClassInspectionResult(ClassStateInjection.INJECTED_METHODS);
            }
        }
        return new ClassInspectionResult(ClassStateInjection.INJECTED_NOTHING);
    }

    @Override
    public boolean supportFor(Class<?> tested) {
        return !tested.isArray() && !tested.isEnum();
    }
}