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

import org.di.annotations.property.Property;
import org.di.context.analyze.impl.ClassAnalyzer;
import org.di.context.analyze.results.ClassAnalyzeResult;
import org.di.context.resolvers.CommandLineArgumentResolver;
import org.di.enviroment.loader.PropertiesLoader;
import org.di.excepton.instantiate.IoCInstantiateException;
import org.di.factories.DependencyFactory;
import org.di.factories.config.Analyzer;
import org.di.factories.config.ComponentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.di.utils.factory.ReflectionUtils.instantiate;

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
                dependencyFactory.instantiatePropertyMethods(o);
                dependencyFactory.addInstalledConfiguration(o);
            } catch (Exception e) {
                throw new Error("Failed to Load " + path + " Config File", e);
            }
        }
    }

    /**
     * Initializing command line resolvers, binded of user.
     *
     * @param resolvers found analyzers in the classpath
     * @param args      block resolvers
     */
    public void initCommandLineResolvers(Set<Class<? extends CommandLineArgumentResolver>> resolvers, String... args) {
        final List<CommandLineArgumentResolver> list = resolvers.stream().map(this::mapResolver).collect(Collectors.toList());
        list.forEach(r -> r.resolve(args));
    }

    private CommandLineArgumentResolver mapResolver(Class<? extends CommandLineArgumentResolver> cls) {
        try {
            return instantiate(cls);
        } catch (IoCInstantiateException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * Initializing analyzers in context
     *
     * @param analyzers found analyzers in the classpath
     */
    public void initAnalyzers(Set<Class<? extends Analyzer>> analyzers) {
        final List<Analyzer<?, ?>> list = analyzers.stream().map(this::mapAnalyzer).collect(Collectors.toList());
        dependencyFactory.addAnalyzers(list);
    }

    private Analyzer<?, ?> mapAnalyzer(Class<? extends Analyzer> cls) {
        try {
            return instantiate(cls);
        } catch (IoCInstantiateException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * Initializing processors in context
     *
     * @param analyzers found processors in the classpath
     */
    public void initProcessors(Set<Class<? extends ComponentProcessor>> analyzers) {
        final List<ComponentProcessor> list = analyzers.stream().map(this::mapProcessor).collect(Collectors.toList());
        dependencyFactory.addProcessors(list);
    }

    private ComponentProcessor mapProcessor(Class<? extends ComponentProcessor> cls) {
        try {
            return instantiate(cls);
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
     */
    public void initializeComponents(Set<Class<?>> components) {
        for (Class<?> component : components) {
            scanClass(component);
        }
    }

    /**
     * Function of scanning a component after detecting the dependencies
     * of their initialization.
     *
     * @param component - class for scan
     */
    private void scanClass(Class<?> component) {
        final ClassAnalyzer classAnalyzer = getAnalyzer(ClassAnalyzer.class);
        if (!classAnalyzer.supportFor(component)) {
            throw new IoCInstantiateException("It is impossible to test, check the class for type match!");
        }

        final ClassAnalyzeResult result = classAnalyzer.analyze(component);
        dependencyFactory.instantiate(component, result);
    }

    /**
     * Function of invoke class method annotated {@link javax.annotation.PostConstruct}.
     *
     * @throws Exception if class methods not invoked
     */
    public void initializePostConstructs() throws Exception {
        dependencyFactory.initializePostConstruct(null);
    }

    /**
     * Returns the component from the factory.
     * Depending on its type, the initialized component or an existing one.
     *
     * @param type - type for get
     * @return instantiated object from context factory
     */
    @SuppressWarnings("unchecked")
    public <O> O getType(Class<O> type) {
        return (O) dependencyFactory.getType(type);
    }

    private <O extends Analyzer<?, ?>> O getAnalyzer(Class<O> cls) {
        return dependencyFactory.getAnalyzer(cls);
    }
}
