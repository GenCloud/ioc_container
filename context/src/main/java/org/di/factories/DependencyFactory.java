package org.di.factories;

import org.di.annotations.Component;
import org.di.annotations.Dependency;
import org.di.annotations.Lazy;
import org.di.annotations.LoadOpt;
import org.di.context.analyze.enums.ClassStateInjection;
import org.di.context.analyze.results.ClassAnalyzeResult;
import org.di.excepton.instantiate.IntroInstantiateException;
import org.di.factories.model.ClassDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

import static org.di.annotations.LoadOpt.Opt.PROTOTYPE;
import static org.di.annotations.LoadOpt.Opt.SINGLETON;
import static org.di.context.analyze.enums.ClassStateInjection.*;
import static org.di.utils.factory.ReflectionUtils.*;

/**
 * Simple template class for implementations that creates a singleton or
 * a prototype object {@link org.di.annotations.LoadOpt.Opt}, depending on a flag.
 * <p>
 * If the "singleton" flag is true (the default), this class will create
 * the object that it creates exactly once on initialization and subsequently
 * return said singleton instance on all calls to the method.
 *
 * @author GenCloud
 * @date 04.09.2018
 */
public class DependencyFactory {
    private static final Logger log = LoggerFactory.getLogger(DependencyFactory.class);

    private Map<String, Object> singletons = new HashMap<>();

    private Set<ClassDefinitions> definitions = new HashSet<>();

    private Map<String, Object> prototypes = new HashMap<>();

    public Map<String, Object> getSingletons() {
        return singletons;
    }

    public Map<String, Object> getPrototypes() {
        return prototypes;
    }

    public Object getType(String name) {
        Object o = singletons.get(name);
        if (o == null) {
            o = prototypes.get(name);
            if (o != null) {
                try {
                    o = instantiate(o.getClass());
                    return o;
                } catch (IntroInstantiateException e) {
                    log.error("", e);
                }
            }
        } else {
            return o;
        }

        return null;
    }

    private ClassDefinitions test(List<ClassDefinitions> definitions, Predicate<ClassDefinitions> predicate) {
        return definitions.stream().filter(predicate).findFirst().orElse(null);
    }

    public void instantiateLazyDefinitions(ClassDefinitions def) throws Exception {
        if (def != null && def.isLazy()) {
            forStateInjection(def);
        } else {
            for (ClassDefinitions definition : definitions) {
                if (definition.isLazy()) {
                    forStateInjection(definition);
                }
            }
        }
    }

    public void instantiateDefinitions(ClassDefinitions def) throws Exception {
        if (def != null) {
            forStateInjection(def);
        } else {
            for (ClassDefinitions definition : definitions) {
                forStateInjection(definition);
            }
        }
    }

    private void forStateInjection(ClassDefinitions def) throws Exception {
        final ClassStateInjection state = def.getStateInjection();
        if (state == INJECTED_CONSTRUCTOR) {
            instantiateDefConstructor(def);
        } else if (state == INJECTED_FIELDS) {
            instantiateDefFields(def);
        } else if (state == INJECTED_METHODS) {
            instantiateDefMethods(def);
        }
    }

    private void instantiateDefMethods(ClassDefinitions definition) throws Exception {
        final List<Class<?>> deps = definition.getDependencies();
        if (!deps.isEmpty()) {
            final List<Object> objects = new ArrayList<>();
            forEachTypes(deps, objects);

            for (Method method : definition.getType().getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Dependency.class)) {
                    final Dependency dependency = method.getAnnotation(Dependency.class);
                    final Class<?> methodParameterType = method.getParameterTypes()[0];
                    final String name = !dependency.name().isEmpty() ? dependency.name() : methodParameterType.getSimpleName();
                    if (methodParameterType.isAnnotationPresent(Lazy.class)) {
                        continue;
                    }

                    final Optional<Object> o = objects.stream().filter(obj -> obj.getClass().getSimpleName().equals(name)).findFirst();
                    if (o.isPresent()) {
                        method.invoke(definition.getType(), o.get());
                    }
                }
            }
        }

        initializeDefInMap(definition);
    }

    private void instantiateDefFields(ClassDefinitions definition) throws Exception {
        final List<Class<?>> deps = definition.getDependencies();
        if (!deps.isEmpty()) {
            final List<Object> objects = new ArrayList<>();
            forEachTypes(deps, objects);

            for (Field field : definition.getType().getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Dependency.class)) {
                    final Dependency dependency = field.getAnnotation(Dependency.class);
                    final Class<?> fieldType = field.getType();
                    final String name = !dependency.name().isEmpty() ? dependency.name() : fieldType.getSimpleName();

                    final Optional<Object> o = objects.stream().filter(obj -> obj.getClass().getSimpleName().equals(name)).findFirst();
                    if (o.isPresent()) {
                        final Object object = o.get();
                        initializeFields(definition.getType(), field, object);
                    }
                }
            }
        }

        initializeDefInMap(definition);
    }

    private void instantiateDefConstructor(ClassDefinitions definition) throws Exception {
        final List<Class<?>> deps = definition.getDependencies();
        if (!deps.isEmpty()) {
            final List<Object> objects = new ArrayList<>();
            forEachTypes(deps, objects);

            if (definition.getType() instanceof Class) {
                final Constructor<?> constructor = ((Class) definition.getType()).getConstructors()[0];
                if (constructor.isAnnotationPresent(Dependency.class)) {
                    final Object o = constructor.newInstance(objects.toArray());
                    definition.setType(o);
                }
            }
        }

        initializeDefInMap(definition);
    }

    private void forEachTypes(List<Class<?>> deps, List<Object> objects) throws Exception {
        for (Class<?> type : deps) {
            final Component annotation = type.getAnnotation(Component.class);
            final String typeName = !annotation.name().isEmpty() ? annotation.name() : type.getSimpleName();
            final Object object = getType(typeName);
            if (object != null) {
                objects.add(object);
                continue;
            }

            final ClassDefinitions tested = test(new ArrayList<>(definitions),
                    d -> d.getQualifiedName().equals(typeName));
            if (tested != null) {
                instantiateConstType(objects, typeName, tested);
            }
        }
    }

    private void initializeDefInMap(ClassDefinitions definition) throws Exception {
        if (!definition.isInitialized()) {
            definition.setInitialized(true);

            final Component annotation = definition.getType().getClass().getAnnotation(Component.class);
            final String typeName = !annotation.name().isEmpty() ? annotation.name() : definition.getType().getClass().getSimpleName();

            instantiateType(typeName, definition);
        }
    }

    private void initializeFields(Object mainObject, Field field, Object typeToInit) throws IllegalAccessException {
        final boolean access = field.isAccessible();
        field.setAccessible(true);
        field.set(mainObject, typeToInit);
        field.setAccessible(access);
    }

    private void instantiateType(String typeName, ClassDefinitions def) throws Exception {
        if (getType(typeName) != null) {
            return;
        }

        if (def.isSingleton()) {
            singletons.computeIfAbsent(typeName, k -> def.getType());
        } else if (def.isPrototype()) {
            Object o = prototypes.get(typeName);
            if (o == null) {
                instantiateDefinitions(def);
                prototypes.put(typeName, def.getType());
            }
        }
    }

    private void instantiateConstType(List<Object> objects, String typeName, ClassDefinitions def) throws Exception {
        if (getType(typeName) != null) {
            return;
        }

        if (def.getType() instanceof Class) {
            instantiateDefinitions(def);
        }

        if (def.isSingleton()) {

            final Object o = singletons.computeIfAbsent(typeName, k -> def.getType());
            objects.add(o);
        } else if (def.isPrototype()) {
            Object o = prototypes.get(typeName);
            if (o != null) {
                instantiateDefinitions(def);
                prototypes.put(typeName, o);
                objects.add(o);
                return;
            }


            if (def.getStateInjection() == INJECTED_CONSTRUCTOR) {
                o = def.getType();
            } else {
                o = instantiate(def.getType().getClass());
            }

            def.setType(o);

            instantiateDefinitions(def);
            objects.add(o);
        }
    }

    public void addDefinition(Class<?> type, ClassAnalyzeResult result) throws Exception {
        boolean flagSigleton = false, flagPrototype = false, flagLazy = type.isAnnotationPresent(Lazy.class);
        if (type.isAnnotationPresent(LoadOpt.class)) {
            final LoadOpt loadOpt = type.getAnnotation(LoadOpt.class);
            if (loadOpt.value() == SINGLETON) {
                flagSigleton = true;
            } else if (loadOpt.value() == PROTOTYPE) {
                flagPrototype = true;
            }
        } else {
            flagSigleton = true;
        }

        final Component intro = type.getAnnotation(Component.class);
        final String qualifiedName = !intro.name().isEmpty() ? intro.name() : type.getSimpleName();

        final ClassStateInjection state = result.getClassStateInjection();
        final ClassDefinitions def = new ClassDefinitions();

        if (state != GRAMMAR_THROW_EXCEPTION) {
            def.setLazy(flagLazy);
            def.setPrototype(flagPrototype);
            def.setSingleton(flagSigleton);
            def.setQualifiedName(qualifiedName);
            def.setStateInjection(state);

            if (state == INJECTED_CONSTRUCTOR) {
                def.setType(type);
                constructorDeps(type, def);
            } else if (state == INJECTED_FIELDS) {
                final Object o = type.newInstance();
                def.setType(o);
                fieldsDeps(o, def);
            } else if (state == INJECTED_METHODS) {
                final Object o = type.newInstance();
                def.setType(o);
                methodsDeps(o, def);
            } else if (state == INJECTED_NOTHING) {
                final Object o = type.newInstance();
                def.setType(o);
                initializeDefInMap(def);
            }

            definitions.add(def);
        } else {
            throw new IntroInstantiateException(type, result.getThrowableMessage());
        }
    }

    private void methodsDeps(Object o, ClassDefinitions def) {
        final Method[] methods = o.getClass().getDeclaredMethods();
        if (methods.length > 1) {
            for (Method method : methods) {
                final Class<?> methodParameterType = method.getParameterTypes()[0];
                if (checkClass(methodParameterType) && checkTypes(methodParameterType)) {
                    def.getDependencies().add(methodParameterType);
                }
            }
        }
    }

    private void fieldsDeps(Object o, ClassDefinitions def) {
        final Field[] fields = o.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Dependency.class)) {
                final Class<?> fieldType = field.getType();
                if (checkClass(fieldType) && checkTypes(fieldType)) {
                    def.getDependencies().add(fieldType);
                }
            }
        }
    }

    private void constructorDeps(Class<?> type, ClassDefinitions def) {
        final Constructor<?> constructor = type.getConstructors()[0];
        if (constructor.isAnnotationPresent(Dependency.class)) {
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length > 0) {
                for (Class<?> parameter : parameterTypes) {
                    if (checkClass(parameter) && checkTypes(parameter)) {
                        def.getDependencies().add(parameter);
                    }
                }
            }
        }
    }
}
