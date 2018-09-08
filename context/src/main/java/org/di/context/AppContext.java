package org.di.context;

import org.di.annotations.Component;
import org.di.annotations.Dependency;
import org.di.annotations.LoadOpt;
import org.di.annotations.property.Property;
import org.di.context.analyze.Analyzer;
import org.di.context.analyze.enums.ClassStateInjection;
import org.di.context.analyze.impl.ClassAnalyzer;
import org.di.context.analyze.impl.CyclicDependenciesAnalyzer;
import org.di.context.analyze.results.ClassAnalyzeResult;
import org.di.context.analyze.results.CyclicDependencyResult;
import org.di.enviroment.PropertiesLoader;
import org.di.excepton.instantiate.IntroInstantiateException;
import org.di.factories.DependencyFactory;
import org.di.utils.factory.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.di.annotations.LoadOpt.Opt.PROTOTYPE;
import static org.di.annotations.LoadOpt.Opt.SINGLETON;
import static org.di.context.analyze.enums.ClassStateInjection.*;
import static org.di.context.analyze.enums.CyclicDependencyState.TRUE;
import static org.di.utils.factory.ReflectionUtils.checkClass;
import static org.di.utils.factory.ReflectionUtils.checkTypes;

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

    public void initEnvironment(Set<Class<?>> properties) {
        for (Class<?> type : properties) {
            final Property property = type.getAnnotation(Property.class);
            final String path = property.path();
            try {
                PropertiesLoader.parse(type, path);
                final Object o = type.newInstance();
                addToFactory(o, o.getClass().getSimpleName(), null);
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
        final CyclicDependenciesAnalyzer analyzer = (CyclicDependenciesAnalyzer) getAnalyzer(CyclicDependenciesAnalyzer.class);
        final CyclicDependencyResult result = analyzer.analyze(new ArrayList<>(components));
        if (result.getCyclicDependencyState() == TRUE) {
            for (Class<?> component : components) {
                if (checkClass(component)) {
                    scanClass(component);
                }
            }
            return;
        }

        throw new IntroInstantiateException(result.getThrowMessage());
    }

    @Nullable
    private Object injectConstructorDeps(Class<?> component) throws IntroInstantiateException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object inComponent;
        final Constructor<?> constructor = component.getConstructors()[0];
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        if (parameterTypes.length > 0) {
            final Set<Object> objects = new HashSet<>();
            for (Class<?> parameter : parameterTypes) {
                if (checkClass(parameter) && checkTypes(parameter)) {
                    if (parameter.isAnnotationPresent(Component.class)) {
                        final Component intro = parameter.getAnnotation(Component.class);
                        Object object;
                        if (parameter.isAnnotationPresent(LoadOpt.class)) {
                            final LoadOpt loadOpt = parameter.getAnnotation(LoadOpt.class);
                            if (loadOpt.value() == PROTOTYPE) {
                                object = instantiateComponent(parameter);
                                if (object != null) {
                                    objects.add(object);
                                    continue;
                                }

                                try {
                                    scanClass(parameter);
                                } catch (StackOverflowError e) {
                                    throw new IntroInstantiateException("Component: " + parameter.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                                }
                            } else if (loadOpt.value() == SINGLETON) {
                                object = getSingleton(parameter, intro);
                                if (object != null) {
                                    objects.add(object);
                                    continue;
                                }

                                try {
                                    scanClass(parameter);
                                } catch (StackOverflowError e) {
                                    throw new IntroInstantiateException("Component: " + parameter.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                                }
                            }
                        } else {
                            object = getSingleton(parameter, intro);

                            if (object != null) {
                                objects.add(object);
                                continue;
                            }

                            try {
                                scanClass(parameter);
                            } catch (StackOverflowError e) {
                                throw new IntroInstantiateException("Component: " + parameter.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                            }
                        }
                    }
                }
            }

            inComponent = constructor.newInstance(objects.toArray());
            final Component intro = component.getAnnotation(Component.class);
            final String componentName = !intro.name().isEmpty() ? intro.name() : component.getSimpleName();
            final LoadOpt loadOpt = component.getAnnotation(LoadOpt.class);
            if (addToFactory(inComponent, componentName, loadOpt)) {
                return inComponent;
            }
        }

        return null;
    }

    @Nullable
    private Object injectFieldsDeps(Class<?> component) throws IllegalAccessException, InstantiationException, IntroInstantiateException, InvocationTargetException {
        final Object inComponent = component.newInstance();
        final Field[] fields = component.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Dependency.class)) {
                final Class<?> type = field.getType();
                if (checkClass(type) && checkTypes(type)) {
                    final Dependency dependency = field.getAnnotation(Dependency.class);
                    Object object;
                    if (type.isAnnotationPresent(LoadOpt.class)) {
                        final LoadOpt loadOpt = type.getAnnotation(LoadOpt.class);
                        if (loadOpt.value() == PROTOTYPE) {
                            object = instantiateComponent(type);
                            if (instantiateFields(inComponent, field, object)) {
                                continue;
                            }

                            try {
                                scanClass(type);
                            } catch (StackOverflowError e) {
                                throw new IntroInstantiateException("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                            }
                        } else if (loadOpt.value() == SINGLETON) {
                            object = getSingleton(type, dependency);
                            if (instantiateFields(inComponent, field, object)) {
                                continue;
                            }

                            try {
                                scanClass(type);
                            } catch (StackOverflowError e) {
                                throw new IntroInstantiateException("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                            }
                        }
                    } else {
                        object = getSingleton(type, dependency);
                        if (instantiateFields(inComponent, field, object)) {
                            continue;
                        }

                        try {
                            scanClass(type);
                        } catch (StackOverflowError e) {
                            throw new IntroInstantiateException("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                        }
                    }
                }
            }
        }

        final Component intro = component.getAnnotation(Component.class);
        final String componentName = !intro.name().isEmpty() ? intro.name() : component.getSimpleName();
        final LoadOpt loadOpt = component.getAnnotation(LoadOpt.class);
        if (addToFactory(inComponent, componentName, loadOpt)) {
            return inComponent;
        }

        return null;
    }

    @Nullable
    private Object injectMethodsDeps(Class<?> component) throws IllegalAccessException, InstantiationException, IntroInstantiateException, InvocationTargetException {
        final Object inComponent = component.newInstance();
        final Method[] methods = inComponent.getClass().getDeclaredMethods();
        if (methods.length > 1) {
            for (Method method : methods) {
                final Class<?> type = method.getParameterTypes()[0];
                if (checkClass(type) && checkTypes(type)) {
                    final Dependency dependency = method.getAnnotation(Dependency.class);
                    Object object;
                    if (type.isAnnotationPresent(LoadOpt.class)) {
                        final LoadOpt loadOpt = type.getAnnotation(LoadOpt.class);
                        if (loadOpt.value() == PROTOTYPE) {
                            object = instantiateComponent(type);
                            if (object != null) {
                                method.invoke(inComponent, object);
                                continue;
                            }

                            try {
                                scanClass(type);
                            } catch (StackOverflowError e) {
                                throw new IntroInstantiateException("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                            }
                        } else if (loadOpt.value() == SINGLETON) {
                            object = getSingleton(type, dependency);
                            if (object != null) {
                                method.invoke(inComponent, object);
                                continue;
                            }

                            try {
                                scanClass(type);
                            } catch (StackOverflowError e) {
                                throw new IntroInstantiateException("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                            }
                        }
                    } else {
                        object = getSingleton(type, dependency);
                        if (object != null) {
                            method.invoke(inComponent, object);
                            continue;
                        }

                        try {
                            scanClass(type);
                        } catch (StackOverflowError e) {
                            throw new IntroInstantiateException("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                        }
                    }
                }
            }
        }

        final Component intro = component.getAnnotation(Component.class);
        final String componentName = !intro.name().isEmpty() ? intro.name() : component.getSimpleName();
        final LoadOpt loadOpt = component.getAnnotation(LoadOpt.class);
        if (addToFactory(inComponent, componentName, loadOpt)) {
            return inComponent;
        }

        return null;
    }

    @Nullable
    private Object scanClass(Class<?> component) throws IntroInstantiateException, IllegalAccessException, InstantiationException, InvocationTargetException {
        final ClassAnalyzer classAnalyzer = (ClassAnalyzer) getAnalyzer(ClassAnalyzer.class);
        if (!classAnalyzer.supportFor(component)) {
            throw new IntroInstantiateException("It is impossible to test, check the class for type match!");
        }

        final ClassAnalyzeResult result = classAnalyzer.analyze(component);
        final ClassStateInjection state = result.getClassStateInjection();
        if (state == GRAMMAR_THROW_EXCEPTION) {
            throw new IntroInstantiateException(component, result.getThrowableMessage());
        } else if (state == INJECTED_CONSTRUCTOR) {
            return injectConstructorDeps(component);
        } else if (state == INJECTED_FIELDS) {
            return injectFieldsDeps(component);
        } else if (state == INJECTED_METHODS) {
            return injectMethodsDeps(component);
        }

        return null;
    }

    private boolean addToFactory(Object inComponent, String componentName, LoadOpt loadOpt) {
        if (loadOpt != null) {
            if (loadOpt.value() == PROTOTYPE) {
                return true;
            } else if (loadOpt.value() == SINGLETON) {
                toFactorySingleton(inComponent, componentName);
                return true;
            }
        } else {
            toFactorySingleton(inComponent, componentName);
            return true;
        }
        return false;
    }

    private Object instantiateComponent(Class<?> type) throws InvocationTargetException, IntroInstantiateException, InstantiationException, IllegalAccessException {
        return scanClass(type);
    }

    private boolean instantiateFields(Object inComponent, Field field, Object object) throws IllegalAccessException {
        if (object != null) {
            final boolean access = field.isAccessible();
            field.setAccessible(true);
            field.set(inComponent, object);
            field.setAccessible(access);
            return true;
        }
        return false;
    }

    private void toFactorySingleton(Object inComponent, String name) {
        dependencyFactory.addSingleton(name, inComponent);
    }

    private Object getSingleton(Class<?> type, Annotation annotation) {
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

        return dependencyFactory.getSingleton(name);
    }

    public Object getSingleton(Class<?> type) {
        String name;
        if (type.isAnnotationPresent(Component.class)) {
            final Component component = type.getAnnotation(Component.class);
            name = !component.name().isEmpty() ? component.name() : type.getSimpleName();
        } else {
            name = type.getSimpleName();
        }

        return getSingleton(name);
    }

    private Object getSingleton(String name) {
        return dependencyFactory.getSingleton(name);
    }

    public Analyzer<?, ?> getAnalyzer(Class<? extends Analyzer> cls) {
        return analyzers.stream().filter(a -> a.getClass().getSimpleName().equals(cls.getSimpleName())).findFirst().orElse(null);
    }
}
