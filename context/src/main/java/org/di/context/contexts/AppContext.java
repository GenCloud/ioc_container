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
package org.di.context.contexts;

import org.di.context.annotations.Processor;
import org.di.context.annotations.property.Property;
import org.di.context.contexts.analyze.impl.ClassInspector;
import org.di.context.contexts.analyze.results.ClassInspectionResult;
import org.di.context.contexts.resolvers.CommandLineArgumentResolver;
import org.di.context.enviroment.loader.PropertiesLoader;
import org.di.context.excepton.instantiate.IoCInstantiateException;
import org.di.context.factories.DependencyInitiator;
import org.di.context.factories.EventDispatcherFactory;
import org.di.context.factories.config.ComponentProcessor;
import org.di.context.factories.config.Factory;
import org.di.context.factories.config.Inspector;
import org.di.context.listeners.Listener;
import org.di.context.listeners.events.OnContextDestroyEvent;
import org.di.context.listeners.impl.FilteredListener;
import org.di.context.listeners.impl.TypedListener;
import org.di.context.utils.factory.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Central class to provide configuration for an application.
 *
 * @author GenCloud
 * @date 04.09.2018
 */
public class AppContext {
    private final static Logger log = LoggerFactory.getLogger(AppContext.class);

    /**
     * Factory initialized contexts components.
     */
    private final DependencyInitiator dependencyInitiator = new DependencyInitiator(this);

    /**
     * Factory for dispatching events to listeners.
     */
    private EventDispatcherFactory dispatcherFactory;

    public EventDispatcherFactory getDispatcherFactory() {
        if (dispatcherFactory == null) {
            final EventDispatcherFactory dispatcher = dependencyInitiator.findFactory(EventDispatcherFactory.class);
            setDispatcherFactory(dispatcher);
        }
        return dispatcherFactory;
    }

    private void setDispatcherFactory(EventDispatcherFactory dispatcherFactory) {
        if (this.dispatcherFactory == null) {
            this.dispatcherFactory = dispatcherFactory;
        }
    }

    /**
     * @return dependencyFactory - factory initialized contexts components
     */
    public DependencyInitiator getDependencyInitiator() {
        return dependencyInitiator;
    }

    /**
     * Initializing configurations in contexts
     */
    public void initEnvironment(Set<Class<?>> properties) {
        for (Class<?> type : properties) {
            final Property property = type.getAnnotation(Property.class);
            if (property.ignore()) {
                continue;
            }

            final Path path = Paths.get(property.path());
            try {
                final Object o = type.newInstance();
                PropertiesLoader.parse(o, path.toFile());
                dependencyInitiator.instantiatePropertyMethods(o);
                dependencyInitiator.addInstalledConfiguration(o);
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
        final List<CommandLineArgumentResolver> list = resolvers
                .stream()
                .map(this::mapResolver)
                .collect(Collectors.toList());

        list.forEach(r -> r.resolve(args));
    }

    private CommandLineArgumentResolver mapResolver(Class<? extends CommandLineArgumentResolver> cls) {
        try {
            return ReflectionUtils.instantiate(cls);
        } catch (IoCInstantiateException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * Initializing inspectors in contexts
     *
     * @param inspectors found analyzers in the classpath
     */
    public void initInspectors(Set<Class<? extends Inspector>> inspectors) {
        final List<Inspector<?, ?>> list = inspectors
                .stream()
                .map(this::mapInspector)
                .collect(Collectors.toList());

        dependencyInitiator.addInspectors(list);
    }

    private Inspector<?, ?> mapInspector(Class<? extends Inspector> cls) {
        try {
            return ReflectionUtils.instantiate(cls);
        } catch (IoCInstantiateException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * Initialize factories {@link Factory} in context.
     *
     * @param factories collection of classes inherited {@link Factory}
     */
    public void initFactories(Set<Class<? extends Factory>> factories) {
        dependencyInitiator.addFactories(factories);
    }

    /**
     * Initializing processors in contexts
     *
     * @param analyzers found processors in the classpath
     */
    public void initProcessors(Set<Class<? extends ComponentProcessor>> analyzers) {
        final List<ComponentProcessor> list = analyzers
                .stream()
                .filter(p -> p.isAnnotationPresent(Processor.class) && !p.getAnnotation(Processor.class).ignore())
                .map(this::mapProcessor)
                .collect(Collectors.toList());

        dependencyInitiator.addProcessors(list);
    }

    private ComponentProcessor mapProcessor(Class<? extends ComponentProcessor> cls) {
        try {
            return ReflectionUtils.instantiate(cls);
        } catch (IoCInstantiateException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * Function of register class annotated {@link org.di.context.annotations.listeners.Listener}.
     *
     * @param listeners collection listeners
     */
    @SuppressWarnings("unchecked")
    public void initListener(Set<Class<?>> listeners) throws Exception {
        if (getDispatcherFactory() == null) {
            final EventDispatcherFactory dispatcher = dependencyInitiator.findFactory(EventDispatcherFactory.class);
            setDispatcherFactory(dispatcher);
        }

        if (!listeners.isEmpty()) {
            for (Class<?> type : listeners) {
                if (Listener.class.isAssignableFrom(type)) {
                    Listener instance;
                    if (TypedListener.class.isAssignableFrom(type)
                            || FilteredListener.class.isAssignableFrom(type)) {
                        final Constructor<? extends Listener> constructor =
                                (Constructor<? extends Listener>) ReflectionUtils.findSampleConstructor(type);
                        assert constructor != null;
                        instance = constructor.newInstance(Arrays
                                .stream(constructor.getParameters())
                                .map(this::mapConstType).toArray());
                    } else {
                        instance = (Listener) ReflectionUtils.instantiate(type);
                    }
                    dependencyInitiator.instantiateSensibles(instance);
                    getDispatcherFactory().addListener(instance);
                }
            }
        }
    }

    private Object mapConstType(Parameter p) {
        final Class<?> type = p.getType();
        if (type == Class.class || type.isPrimitive()) {
            return type;
        } else {
            return ReflectionUtils.instantiate(type);
        }
    }

    /**
     * The main function for initializing component dependencies.
     * <p>
     * Starts the analyzers and, if properly executed, starts the initialization of components.
     *
     * @param components - collection of components found in the classpatch
     */
    public void initComponents(Set<Class<?>> components) {
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
        final ClassInspector classAnalyzer = getAnalyzer(ClassInspector.class);
        if (!classAnalyzer.supportFor(component)) {
            throw new IoCInstantiateException("It is impossible to org.di.test, check the class for type match!");
        }

        final ClassInspectionResult result = classAnalyzer.inspect(component);
        dependencyInitiator.instantiate(component, result);
    }

    /**
     * Function of invoke class method annotated {@link javax.annotation.PostConstruct}.
     *
     * @throws Exception if class methods not invoked
     */
    public void initPostConstructions() throws Exception {
        dependencyInitiator.initializePostConstructions(null);
    }


    /**
     * Returns the component from the factory.
     * Depending on its type, the initialized component or an existing one.
     *
     * @param type - type for get
     * @return instantiated object from contexts factory
     */
    @SuppressWarnings("unchecked")
    public <O> O getType(Class<O> type) {
        return (O) dependencyInitiator.getType(type);
    }

    private <O extends Inspector<?, ?>> O getAnalyzer(Class<O> cls) {
        return dependencyInitiator.getInspetor(cls);
    }

    /**
     * Function of calling shutdown hook
     */
    public void closeContext() {
        getDispatcherFactory().fireEvent(new OnContextDestroyEvent(this));
        dependencyInitiator.clear();
        log.info("Context is closed");
    }
}