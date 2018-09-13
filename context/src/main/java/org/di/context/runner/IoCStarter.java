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
package org.di.context.runner;

import org.apache.commons.lang3.time.StopWatch;
import org.di.annotations.IoCComponent;
import org.di.annotations.property.Property;
import org.di.context.AppContext;
import org.di.context.resolvers.CommandLineArgumentResolver;
import org.di.factories.config.Analyzer;
import org.di.factories.config.ComponentProcessor;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.di.utils.factory.ReflectionUtils.configureScanner;

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
        AppContext context = null;
        try {
            log.info("Start initialization of context app");
            context = initializeContext(mainClasses, args);
        } catch (Exception e) {
            final String msg = e.getMessage();
            log.error("Incorrect start: {}", msg, e);
            System.exit(0);
        }

        watch.stop();

        final long seconds = watch.getTime(TimeUnit.SECONDS);
        log.info("App context started in [{}] seconds", seconds);
        return context;
    }


    /**
     * Load components into context.
     *
     * @param mainClasses the classes to analyze
     * @param args        block startup resolvers
     * @return running {@link AppContext}
     */
    private AppContext initializeContext(Class<?>[] mainClasses, String... args) throws Exception {
        final AppContext context = new AppContext();
        for (Class<?> mainSource : mainClasses) {
            final Reflections reflections = configureScanner(mainSource);
            final Set<Class<?>> components = reflections.getTypesAnnotatedWith(IoCComponent.class);
            final Set<Class<? extends Analyzer>> analyzers = reflections.getSubTypesOf(Analyzer.class);
            final Set<Class<?>> properties = reflections.getTypesAnnotatedWith(Property.class);
            final Set<Class<? extends ComponentProcessor>> processors = reflections.getSubTypesOf(ComponentProcessor.class);
            final Set<Class<? extends CommandLineArgumentResolver>> resolvers = reflections.getSubTypesOf(CommandLineArgumentResolver.class);
            if (!resolvers.isEmpty()) {
                context.initCommandLineResolvers(resolvers, args);
            }

            context.initAnalyzers(analyzers);
            context.initProcessors(processors);
            context.initEnvironment(properties);
            context.initializeComponents(components);

            context.initializePostConstructs();
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(context));
        return context;
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
