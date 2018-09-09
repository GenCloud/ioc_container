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
package org.di.context;

import org.di.annotations.IoCComponent;
import org.di.annotations.property.Property;
import org.di.context.analyze.Analyzer;
import org.di.context.analyze.impl.ClassAnalyzer;
import org.di.context.analyze.impl.CyclicDependenciesAnalyzer;
import org.di.context.analyze.results.ClassAnalyzeResult;
import org.di.context.analyze.results.CyclicDependencyResult;
import org.di.enviroment.loader.PropertiesLoader;
import org.di.excepton.instantiate.IoCInstantiateException;
import org.di.factories.DependencyFactory;
import org.di.utils.factory.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.di.context.analyze.enums.CyclicDependencyState.FALSE;

/**
 * Central class to provide configuration for an application.
 *
 * @author GenCloud
 * @date 04.09.2018
 */
public class AppContext {
    private final static Logger log = LoggerFactory.getLogger(AppContext.class);

    /**
     * Factory initialized context components
     */
    private final DependencyFactory dependencyFactory = new DependencyFactory();

    /**
     * Context Analyzers
     */
    private final List<Analyzer<?, ?>> analyzers = new ArrayList<>();

    /**
     * @return dependencyFactory - factory initialized context components
     */
    public DependencyFactory getDependencyFactory() {
        return dependencyFactory;
    }

    /**
     * Initializing configurations in context
     */
    public void initEnvironment(Set<Class<?>> properties) {
        for (Class<?> type : properties) {
            final Property property = type.getAnnotation(Property.class);
            final Path path = Paths.get(property.path());
            try {
                final Object o = type.newInstance();
                PropertiesLoader.parse(o, path.toFile());
                dependencyFactory.addSingleton(o);
            } catch (Exception e) {
                throw new Error("Failed to Load " + path + " Properties File", e);
            }
        }
    }

    /**
     * Initializing analyzers in context
     *
     * @param analyzers - found analyzers in the classpath
     */
    public void initAnalyzers(Set<Class<? extends Analyzer>> analyzers) {
        final List<Analyzer<?, ?>> list = analyzers.stream().map(this::mapAnalyzer).collect(Collectors.toList());
        this.analyzers.addAll(list);
    }

    private Analyzer<?, ?> mapAnalyzer(Class<? extends Analyzer> cls) {
        try {
            return ReflectionUtils.instantiate(cls);
        } catch (IoCInstantiateException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * The main function for initializing component dependencies.
     * <p>
     * Starts the analyzers and, if properly executed, starts the initialization of components.
     *
     * @param components - collection of components found in the classpatch
     * @throws Exception - if component have grammar error, cyclic dependencies, etc.
     */
    public void initializeComponents(Set<Class<?>> components) throws Exception {
        final CyclicDependenciesAnalyzer analyzer = (CyclicDependenciesAnalyzer) getAnalyzer(CyclicDependenciesAnalyzer.class);
        final CyclicDependencyResult result = analyzer.analyze(new ArrayList<>(components));
        if (result.getCyclicDependencyState() == FALSE) {
            for (Class<?> component : components) {
                scanClass(component);
            }

            dependencyFactory.instantiateDefinitions(null);
            return;
        }

        throw new IoCInstantiateException(result.getThrowMessage());
    }

    /**
     * Function of scanning a component after detecting the dependencies
     * of their initialization.
     *
     * @param component - class for scan
     * @throws Exception - @throws Exception - if component have grammar error
     */
    private void scanClass(Class<?> component) throws Exception {
        final ClassAnalyzer classAnalyzer = (ClassAnalyzer) getAnalyzer(ClassAnalyzer.class);
        if (!classAnalyzer.supportFor(component)) {
            throw new IoCInstantiateException("It is impossible to test, check the class for type match!");
        }

        final ClassAnalyzeResult result = classAnalyzer.analyze(component);
        dependencyFactory.addDefinition(component, result);
    }

    /**
     * Returns the component from the factory.
     * Depending on its type, the initialized component or an existing one
     *
     * @param type - type for get
     * @return instantiated object from context
     */
    public Object getType(Class<?> type) {
        String name;
        if (type.isAnnotationPresent(IoCComponent.class)) {
            final IoCComponent ioCComponent = type.getAnnotation(IoCComponent.class);
            name = !ioCComponent.name().isEmpty() ? ioCComponent.name() : type.getSimpleName();
        } else {
            name = type.getSimpleName();
        }

        return getType(name);
    }

    /**
     * @param name - name of component for get
     * @return instantiated object from context factory
     */
    private Object getType(String name) {
        return dependencyFactory.getType(name);
    }

    /**
     * The analyzer search function by its class name.
     *
     * @param cls - class analyzer
     * @return found analyzer
     */
    public Analyzer<?, ?> getAnalyzer(Class<? extends Analyzer> cls) {
        return analyzers.stream().filter(a -> a.getClass().getSimpleName().equals(cls.getSimpleName())).findFirst().orElse(null);
    }
}
