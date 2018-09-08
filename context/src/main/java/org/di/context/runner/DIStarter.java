package org.di.context.runner;

import org.apache.commons.lang3.time.StopWatch;
import org.di.annotations.Component;
import org.di.annotations.ScanPackage;
import org.di.annotations.property.Property;
import org.di.context.AppContext;
import org.di.context.analyze.Analyzer;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.di.utils.factory.ReflectionUtils.configureReflection;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
public class DIStarter {
    private static final Logger log = LoggerFactory.getLogger(DIStarter.class);

    public static AppContext start(Class<?> mainClass, String... args) {
        return new DIStarter().start(new Class[]{mainClass}, args);
    }

    private AppContext start(Class<?>[] mainClasses, String... args) {
        final StopWatch watch = new StopWatch();
        watch.start();
        AppContext context = null;
        try {
            log.info("Start initialization of context app");
            context = initializeContext(mainClasses);
        } catch (Exception e) {
            log.error("Incorrect start", e);
        }

        watch.stop();

        final long seconds = watch.getTime(TimeUnit.SECONDS);
        log.info("App context started in [{}] seconds", seconds);
        return context;
    }

    private AppContext initializeContext(Class<?>... mainClasses) throws Exception {
        final AppContext context = new AppContext();
        for (Class<?> mainSource : mainClasses) {
            final ScanPackage scanPackage = mainSource.getAnnotation(ScanPackage.class);
            if (scanPackage != null) {
                final String[] packages = scanPackage.packages();
                final Class<?>[] classes = scanPackage.classes();
                if (packages.length > 0) {
                    final Reflections reflections = configureReflection(packages);
                    final Set<Class<?>> components = reflections.getTypesAnnotatedWith(Component.class);
                    final Set<Class<? extends Analyzer>> analyzers = reflections.getSubTypesOf(Analyzer.class);
                    final Set<Class<?>> properties = reflections.getTypesAnnotatedWith(Property.class);
                    context.initEnvironment(properties);
                    context.initAnalyzers(analyzers);
                    context.initializeComponents(components);
                } else if (classes.length > 0) {
                    final Reflections reflections = configureReflection(classes);
                    final Set<Class<?>> components = reflections.getTypesAnnotatedWith(Component.class);
                    final Set<Class<? extends Analyzer>> analyzers = reflections.getSubTypesOf(Analyzer.class);
                    final Set<Class<?>> properties = reflections.getTypesAnnotatedWith(Property.class);
                    context.initEnvironment(properties);
                    context.initAnalyzers(analyzers);
                    context.initializeComponents(components);
                } else {
                    final Reflections reflections = configureReflection(new Class[]{mainSource});
                    final Set<Class<?>> components = reflections.getTypesAnnotatedWith(Component.class);
                    final Set<Class<? extends Analyzer>> analyzers = reflections.getSubTypesOf(Analyzer.class);
                    final Set<Class<?>> configurations = reflections.getTypesAnnotatedWith(Property.class);
                    context.initEnvironment(configurations);
                    context.initAnalyzers(analyzers);
                    context.initializeComponents(components);
                }
            }
        }

        return context;
    }
}
