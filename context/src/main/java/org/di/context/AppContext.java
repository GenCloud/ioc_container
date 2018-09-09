package org.di.context;

import org.di.annotations.Component;
import org.di.annotations.Dependency;
import org.di.annotations.property.Property;
import org.di.context.analyze.Analyzer;
import org.di.context.analyze.impl.ClassAnalyzer;
import org.di.context.analyze.results.ClassAnalyzeResult;
import org.di.enviroment.PropertiesLoader;
import org.di.excepton.instantiate.IntroInstantiateException;
import org.di.factories.DependencyFactory;
import org.di.utils.factory.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.di.utils.factory.ReflectionUtils.checkClass;

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
            final String path = property.path();
            try {
                PropertiesLoader.parse(type, path);
                final Object o = type.newInstance();
                scanClass(o.getClass());
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
        } catch (IntroInstantiateException e) {
            log.error("", e);
        }
        return null;
    }

    public void initializeComponents(Set<Class<?>> components) throws Exception {
        for (Class<?> component : components) {
            if (checkClass(component)) {
                scanClass(component);
            }
        }

        dependencyFactory.instantiateDefinitions(null);
        dependencyFactory.instantiateLazyDefinitions(null);
    }

    private void scanClass(Class<?> component) throws Exception {
        final ClassAnalyzer classAnalyzer = (ClassAnalyzer) getAnalyzer(ClassAnalyzer.class);
        if (!classAnalyzer.supportFor(component)) {
            throw new IntroInstantiateException("It is impossible to test, check the class for type match!");
        }

        final ClassAnalyzeResult result = classAnalyzer.analyze(component);
        dependencyFactory.addDefinition(component, result);
    }

    private Object getType(Class<?> type, Annotation annotation) {
        String name = null;
        if (annotation != null) {
            if (annotation instanceof Component) {
                final Component component = (Component) annotation;
                name = !component.name().isEmpty() ? component.name() : type.getSimpleName();
            } else if (annotation instanceof Dependency) {
                final Dependency dependency = (Dependency) annotation;
                name = !dependency.name().isEmpty() ? dependency.name() : type.getSimpleName();
            }
        } else {
            name = type.getSimpleName();
        }

        return dependencyFactory.getType(name);
    }

    public Object getType(Class<?> type) {
        String name;
        if (type.isAnnotationPresent(Component.class)) {
            final Component component = type.getAnnotation(Component.class);
            name = !component.name().isEmpty() ? component.name() : type.getSimpleName();
        } else {
            name = type.getSimpleName();
        }

        return getType(name);
    }

    private Object getType(String name) {
        return dependencyFactory.getType(name);
    }

    public Analyzer<?, ?> getAnalyzer(Class<? extends Analyzer> cls) {
        return analyzers.stream().filter(a -> a.getClass().getSimpleName().equals(cls.getSimpleName())).findFirst().orElse(null);
    }
}
