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

import org.di.annotations.IoCComponent;
import org.di.annotations.IoCDependency;
import org.di.context.analyze.Analyzer;
import org.di.context.analyze.results.CyclicDependencyResult;
import org.di.excepton.instantiate.IoCInstantiateException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import static org.di.context.analyze.enums.CyclicDependencyState.FALSE;
import static org.di.utils.factory.ReflectionUtils.checkClass;
import static org.di.utils.factory.ReflectionUtils.checkTypes;

/**
 * Analyzer for detecting the circular dependencies in components.
 *
 * @author GenCloud
 * @date 06.09.2018
 */
public class CyclicDependenciesAnalyzer implements Analyzer<CyclicDependencyResult, List<Class<?>>> {
    private final Map<String, DependencyInfo> dependencyInfos = new HashMap<>();

    @Override
    public CyclicDependencyResult analyze(List<Class<?>> tested) throws Exception {
        for (Class<?> type : tested) {
            checkType(type);
        }

        return checkCyclicDependencies();
    }

    @Override
    public boolean supportFor(List<Class<?>> tested) {
        return true;
    }

    /**
     * Checking the types for compliance to overdetermined components
     * and entering them for further verification.
     *
     * @param type - type for check
     * @throws IoCInstantiateException on find grammar errors
     */
    private void checkType(Class<?> type) throws IoCInstantiateException {
        final Constructor<?> constructor = type.getConstructors()[0];
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        final DependencyInfo definition = new DependencyInfo();
        if (parameterTypes.length > 0) {
            if (constructor.isAnnotationPresent(IoCDependency.class)) {
                for (Class<?> parameter : parameterTypes) {
                    if (parameter.isAnnotationPresent(IoCComponent.class)) {
                        if (!checkClass(parameter) && !checkTypes(parameter)) {
                            continue;
                        }

                        addDependencies(parameter, definition);
                    }
                }
            }
        }

        final Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(IoCDependency.class)) {
                final Class<?> fieldType = field.getType();
                if (!checkClass(fieldType) && !checkTypes(fieldType)) {
                    continue;
                }

                addDependencies(fieldType, definition);
            }
        }

        final Method[] methods = type.getDeclaredMethods();
        if (methods.length > 1) {
            for (Method method : methods) {
                if (method.isAnnotationPresent(IoCDependency.class)) {
                    if (method.getParameterCount() == 0) {
                        throw new IoCInstantiateException("Impossibility of injection into a function with fewer parameters than one");
                    }

                    if (method.getParameterCount() > 1) {
                        throw new IoCInstantiateException("Inability to inject a function with more than one parameter");
                    }

                    final Class<?> methodParameterType = method.getParameterTypes()[0];

                    if (!checkClass(methodParameterType) && !checkTypes(methodParameterType)) {
                        continue;
                    }

                    addDependencies(methodParameterType, definition);
                }
            }
        }

        addToMap(type.getSimpleName(), definition);
    }

    /**
     * The main method of checking components for cyclic dependencies among themselves
     *
     * @return - the result of checking cyclic dependencies
     * @see CyclicDependencyResult#getCyclicDependencyState()
     */
    private CyclicDependencyResult checkCyclicDependencies() {
        for (Entry<String, DependencyInfo> entry : dependencyInfos.entrySet()) {
            final String key = entry.getKey();
            final DependencyInfo value = entry.getValue();
            final CyclicDependencyResult result = checkEntry(key, value);
            if (result.getCyclicDependencyState() == FALSE) {
                continue;
            }

            return result;
        }

        return new CyclicDependencyResult(FALSE);
    }

    private CyclicDependencyResult checkEntry(String cur, DependencyInfo value) {
        final List<Class<?>> classes = value.getDependencies();
        if (classes != null && !classes.isEmpty()) {
            for (Class<?> type : classes) {
                final DependencyInfo dep = dependencyInfos.get(type.getSimpleName());
                if (dep != null) {
                    final List<Class<?>> classDeps = dep.getDependencies();
                    if (classDeps != null && !classDeps.isEmpty()) {
                        for (Class<?> type_dep : classDeps) {
                            if (type_dep.getSimpleName().equals(cur)) {
                                return new CyclicDependencyResult("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference in " + cur + "?");
                            }
                        }
                    }
                }
            }
        }

        return new CyclicDependencyResult(FALSE);
    }

    /**
     * Inserting dependencies of type to collection
     *
     * @param type           - class to get him dependencies
     * @param dependencyInfo - info instance of type dependencies
     */
    private void addDependencies(Class<?> type, DependencyInfo dependencyInfo) {
        if (dependencyInfo.getDependencies() == null) {
            dependencyInfo.setDependencies(new ArrayList<>());
        }

        final Optional<Class<?>> optional = dependencyInfo.getDependencies().stream().filter(c -> c.getSimpleName().equals(type.getSimpleName())).findFirst();
        if (!optional.isPresent()) {
            dependencyInfo.getDependencies().add(type);
        }
    }

    private void addToMap(String name, DependencyInfo definition) {
        if (!dependencyInfos.containsKey(name)) {
            dependencyInfos.put(name, definition);
        } else {
            final DependencyInfo old = dependencyInfos.get(name);
            dependencyInfos.replace(name, old, definition);
        }
    }

    private static class DependencyInfo {
        private List<Class<?>> dependencies;

        public List<Class<?>> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<Class<?>> dependencies) {
            this.dependencies = dependencies;
        }
    }
}