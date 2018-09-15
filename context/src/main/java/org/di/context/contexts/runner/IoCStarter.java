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
package org.di.context.contexts.runner;

import org.apache.commons.lang3.time.StopWatch;
import org.di.context.annotations.IoCComponent;
import org.di.context.annotations.modules.ThreadingModule;
import org.di.context.annotations.property.Property;
import org.di.context.contexts.AppContext;
import org.di.context.contexts.resolvers.CommandLineArgumentResolver;
import org.di.context.factories.config.ComponentProcessor;
import org.di.context.factories.config.Factory;
import org.di.context.factories.config.Inspector;
import org.di.context.listeners.events.OnContextIsInitializedEvent;
import org.di.context.listeners.events.OnContextStartedEvent;
import org.di.context.utils.factory.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Classes that can be used to bootstrap and launch a application from a main method.
 * <p>
 * In most circumstances the static {@link #start(Class, String...)} method can be called
 * directly from your {@literal main} method to bootstrap your application:
 *
 * <pre class="code">
 * public class App {
 *     public static void main(String... args) throws Exception {
 *         IoCStarter.run(App.class, args);
 *     }
 * }
 * </pre>
 *
 * @author GenCloud
 * @date 04.09.2018
 * @see #start(Class, String...)
 * @see #start(Class[], String...)
 */
public class IoCStarter {
    private static final Logger log = LoggerFactory.getLogger(IoCStarter.class);

    private AppContext appContext;

    /**
     * Static helper that can be used to run a {@link IoCStarter} from the
     * specified source using default settings.
     *
     * @param mainClass the class to load
     * @param args      application resolvers
     * @return running {@link AppContext}
     */
    public static AppContext start(Class<?> mainClass, String... args) {
        return new IoCStarter().start(new Class[]{mainClass}, args);
    }

    /**
     * Run the application, creating {@link AppContext}.
     *
     * @param mainClasses the class to load
     * @param args        application resolvers
     * @return running {@link AppContext}
     */
    private AppContext start(Class<?>[] mainClasses, String... args) {
        final StopWatch watch = new StopWatch();
        watch.start();
        try {
            log.info("Start initialization of contexts app");
            appContext = initializeContext(mainClasses, args);
            appContext.getDispatcherFactory().fireEvent(new OnContextStartedEvent(appContext));
        } catch (Exception e) {
            final String msg = e.getMessage();
            log.error("Incorrect start: {}", msg, e);
            System.exit(0);
        }

        watch.stop();

        final long seconds = watch.getTime(TimeUnit.SECONDS);
        log.info("App contexts started in [{}] seconds", seconds);
        return appContext;
    }

    /**
     * Load components into contexts.
     *
     * @param mainClasses the classes to inspect
     * @param args        block startup resolvers
     * @return running {@link AppContext}
     */
    private AppContext initializeContext(Class<?>[] mainClasses, String... args) throws Exception {
        final AppContext context = new AppContext();
        for (Class<?> mainSource : mainClasses) {
            final List<String> modulePackages = getModulePaths(mainSource);
            final String[] packages = modulePackages.toArray(new String[0]);
            final Reflections reflections = ReflectionUtils.configureScanner(packages, mainSource);
            final ModuleInfo info = getModuleInfo(reflections);
            initializeModule(context, info, args);
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(context));
        context.getDispatcherFactory().fireEvent(new OnContextIsInitializedEvent(context));
        return context;
    }

    /**
     * Get installed module packages.
     * <p>
     * todo:13.09.18:rework me - come up with an idea for implementing auto-detection
     *
     * @param mainSource the classes to inspect
     * @return collection of packages
     */
    private List<String> getModulePaths(Class<?> mainSource) {
        final List<String> list = new ArrayList<>();
        if (mainSource.isAnnotationPresent(ThreadingModule.class)) {
            list.add("org.di.threads");
        }
        return list;
    }

    /**
     * Generate module info.
     *
     * @param reflections input configured instance of reflections
     * @return new instance of module info
     */
    private ModuleInfo getModuleInfo(Reflections reflections) {
        final ModuleInfo moduleInfo = new ModuleInfo();
        moduleInfo.setComponents(reflections.getTypesAnnotatedWith(IoCComponent.class));
        moduleInfo.setInspectors(reflections.getSubTypesOf(Inspector.class));
        moduleInfo.setProperties(reflections.getTypesAnnotatedWith(Property.class));
        moduleInfo.setProcessors(reflections.getSubTypesOf(ComponentProcessor.class));
        moduleInfo.setResolvers(reflections.getSubTypesOf(CommandLineArgumentResolver.class));
        moduleInfo.setFactories(reflections.getSubTypesOf(Factory.class));
        return moduleInfo;
    }

    /**
     * Load components into contexts.
     *
     * @param context instance of application contexts
     * @param info    created instance of module info
     * @param args    block startup resolvers
     * @throws Exception if contexts throw exception
     */
    private void initializeModule(AppContext context, ModuleInfo info, String... args) throws Exception {
        if (!info.getResolvers().isEmpty()) {
            context.initCommandLineResolvers(info.getResolvers(), args);
        }

        context.initInspectors(info.getInspectors());
        context.initEnvironment(info.getProperties());
        context.initFactories(info.getFactories());
        context.initProcessors(info.getProcessors());
        context.initComponents(info.getComponents());

        context.initPostConstructions();
    }

    /**
     * Class-utility for storing various module information.
     */
    private class ModuleInfo {
        private Set<Class<?>> components;
        private Set<Class<? extends Inspector>> inspectors;
        private Set<Class<?>> properties;
        private Set<Class<? extends ComponentProcessor>> processors;
        private Set<Class<? extends CommandLineArgumentResolver>> resolvers;
        private Set<Class<? extends Factory>> factories;

        Set<Class<?>> getComponents() {
            return components;
        }

        void setComponents(Set<Class<?>> components) {
            this.components = components;
        }

        Set<Class<? extends Inspector>> getInspectors() {
            return inspectors;
        }

        void setInspectors(Set<Class<? extends Inspector>> inspectors) {
            this.inspectors = inspectors;
        }

        Set<Class<?>> getProperties() {
            return properties;
        }

        Set<Class<? extends Factory>> getFactories() {
            return factories;
        }

        void setProperties(Set<Class<?>> properties) {
            this.properties = properties;
        }

        Set<Class<? extends ComponentProcessor>> getProcessors() {
            return processors;
        }

        void setProcessors(Set<Class<? extends ComponentProcessor>> processors) {
            this.processors = processors;
        }

        Set<Class<? extends CommandLineArgumentResolver>> getResolvers() {
            return resolvers;
        }

        void setResolvers(Set<Class<? extends CommandLineArgumentResolver>> resolvers) {
            this.resolvers = resolvers;
        }

        void setFactories(Set<Class<? extends Factory>> factories) {
            this.factories = factories;
        }
    }

    private class ShutdownHook extends Thread {
        private final AppContext context;

        ShutdownHook(AppContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            log.info("Start shutdown application");
            context.closeContext();
        }
    }
}
