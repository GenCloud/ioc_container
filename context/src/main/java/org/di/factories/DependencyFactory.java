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
package org.di.factories;

import org.di.annotations.IoCComponent;
import org.di.annotations.IoCDependency;
import org.di.annotations.Lazy;
import org.di.annotations.LoadOpt;
import org.di.annotations.property.Property;
import org.di.context.analyze.enums.ClassStateInjection;
import org.di.context.analyze.results.ClassAnalyzeResult;
import org.di.excepton.instantiate.IoCInstantiateException;
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
 * a prototype object {@link LoadOpt.Opt}, depending on a flag.
 *
 * <p>If the "singleton" flag is true (the default), this class will create
 * the object that it creates exactly once on initialization and subsequently
 * return said singleton instance on all calls to the method.</p>
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

    /**
     * Return an instance, which may be shared or independent, of the specified component.
     *
     * @param name - name of the component to retrieve
     * @return - instance of component
     */
    public Object getType(String name) {
        Object o = singletons.get(name);
        if (o == null) {
            o = prototypes.get(name);
            if (o != null) {
                try {
                    o = instantiate(o.getClass());
                    return o;
                } catch (IoCInstantiateException e) {
                    log.error("", e);
                }
            }
        } else {
            return o;
        }

        return null;
    }

    /**
     * The function of enumerating the collection of information of uninitialized components by a filter.
     *
     * @param definitions - information classes collection of uninitialized components
     * @param predicate   - filter
     * @return filtered ClassDefinitions
     */
    private ClassDefinitions test(List<ClassDefinitions> definitions, Predicate<ClassDefinitions> predicate) {
        return definitions.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Component initialization function. Assembling an information class to identify dependencies
     * and initialization type.
     *
     * @param def - information class of component
     * @throws Exception
     */
    public void instantiateDefinitions(ClassDefinitions def) throws Exception {
        if (def != null) {
            forStateInjection(def);
        } else {
            for (ClassDefinitions definition : definitions) {
                forStateInjection(definition);
            }
        }
    }

    /**
     * Function of identify initialization type.
     *
     * @param def - information class of uninitialized component
     * @throws Exception
     */
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

    /**
     * Functional method for initializing component dependencies.
     *
     * @param definition - information class of uninitialized component
     * @throws Exception
     */
    private void instantiateDefMethods(ClassDefinitions definition) throws Exception {
        final List<Class<?>> deps = definition.getDependencies();
        if (!deps.isEmpty()) {
            final List<Object> objects = new ArrayList<>();
            forEachTypes(deps, objects);

            for (Method method : definition.getType().getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(IoCDependency.class)) {
                    final IoCDependency ioCDependency = method.getAnnotation(IoCDependency.class);
                    final Class<?> methodParameterType = method.getParameterTypes()[0];
                    final String name = !ioCDependency.name().isEmpty() ? ioCDependency.name() : methodParameterType.getSimpleName();
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

    /**
     * Initializing components by analyzing member fields.
     *
     * @param definition - information class of uninitialized component
     * @throws Exception
     */
    private void instantiateDefFields(ClassDefinitions definition) throws Exception {
        final List<Class<?>> deps = definition.getDependencies();
        if (!deps.isEmpty()) {
            final List<Object> objects = new ArrayList<>();
            forEachTypes(deps, objects);

            for (Field field : definition.getType().getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(IoCDependency.class)) {
                    final IoCDependency ioCDependency = field.getAnnotation(IoCDependency.class);
                    final Class<?> fieldType = field.getType();
                    final String name = !ioCDependency.name().isEmpty() ? ioCDependency.name() : fieldType.getSimpleName();

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

    /**
     * Initializing a component by analyzing a component constructor.
     *
     * @param definition - information class of uninitialized component
     * @throws Exception
     */
    private void instantiateDefConstructor(ClassDefinitions definition) throws Exception {
        final List<Class<?>> deps = definition.getDependencies();
        if (!deps.isEmpty()) {
            final List<Object> objects = new ArrayList<>();
            forEachTypes(deps, objects);

            if (definition.getType() instanceof Class) {
                final Constructor<?> constructor = ((Class) definition.getType()).getConstructors()[0];
                if (constructor.isAnnotationPresent(IoCDependency.class)) {
                    final Object o = constructor.newInstance(objects.toArray());
                    definition.setType(o);
                }
            }
        }

        initializeDefInMap(definition);
    }

    /**
     * The function of enumerating the dependencies of the component and initializing them with
     * the addition to the collection.
     *
     * @param deps    - dependencies of component
     * @param objects - empty collection for adding initialized components
     * @throws Exception
     */
    private void forEachTypes(List<Class<?>> deps, List<Object> objects) throws Exception {
        for (Class<?> type : deps) {
            final IoCComponent annotation = type.getAnnotation(IoCComponent.class);
            String typeName;
            if (!type.isAnnotationPresent(Property.class)) {
                typeName = !annotation.name().isEmpty() ? annotation.name() : type.getSimpleName();
            } else {
                typeName = type.getSimpleName();
            }

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

    /**
     * Function of initializing a component if it is not initialized in the factory.
     *
     * @param definition - information class of maybe uninitialized component
     * @throws Exception
     */
    private void initializeDefInMap(ClassDefinitions definition) throws Exception {
        if (!definition.isInitialized()) {
            definition.setInitialized(true);

            final IoCComponent annotation = definition.getType().getClass().getAnnotation(IoCComponent.class);
            final String typeName = !annotation.name().isEmpty() ? annotation.name() : definition.getType().getClass().getSimpleName();

            instantiateType(typeName, definition);
        }
    }

    /**
     * Function of initializing the insertion of a value in the component field.
     *
     * @param mainObject - main instantiated object who have field
     * @param field      - field for set value
     * @param typeToInit - value for set
     * @throws IllegalAccessException
     */
    private void initializeFields(Object mainObject, Field field, Object typeToInit) throws IllegalAccessException {
        final boolean access = field.isAccessible();
        field.setAccessible(true);
        field.set(mainObject, typeToInit);
        field.setAccessible(access);
    }

    /**
     * Function of initializing a component and adding it to boot parameters in a collection.
     *
     * @param typeName - name of component
     * @param def      - information class of uninitialized component
     * @throws Exception
     */
    private void instantiateType(String typeName, ClassDefinitions def) throws Exception {
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

    /**
     * Analog of the @instantiateType function with the addition of initialized components to the collection.
     *
     * @param objects  - empty collection for adding initialized components
     * @param typeName - name of head component
     * @param def      - information class of uninitialized component
     * @throws Exception
     * @see DependencyFactory#instantiateType(String, ClassDefinitions)
     */
    private void instantiateConstType(List<Object> objects, String typeName, ClassDefinitions def) throws Exception {
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

    /**
     * Custom function for adding singleton component in factory.
     *
     * @param o - instantiated object
     */
    public void addSingleton(Object o) {
        singletons.putIfAbsent(o.getClass().getSimpleName(), o);
    }

    /**
     * The function of creating an information class-component for its further analysis.
     *
     * @param type   - uninitialized component class
     * @param result - result of analyzer for detecting type initialization dependencies
     * @throws Exception
     */
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

        final IoCComponent intro = type.getAnnotation(IoCComponent.class);
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
            throw new IoCInstantiateException(type, result.getThrowableMessage());
        }
    }

    /**
     * The function of analyzing the information class for the presence of dependency components in the functions.
     *
     * @param o   - instantiated object of component
     * @param def - information class of component
     */
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

    /**
     * The function of analyzing the information class for the presence of component dependencies in the fields.
     *
     * @param o   - instantiated object of component
     * @param def - information class of component
     */
    private void fieldsDeps(Object o, ClassDefinitions def) {
        final Field[] fields = o.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(IoCDependency.class)) {
                final Class<?> fieldType = field.getType();
                if (checkClass(fieldType) && checkTypes(fieldType)) {
                    def.getDependencies().add(fieldType);
                }
            }
        }
    }

    /**
     * The function of analyzing the information class for the presence of dependency components in the constructor.
     *
     * @param type - class of component
     * @param def  - information class of component
     */
    private void constructorDeps(Class<?> type, ClassDefinitions def) {
        final Constructor<?> constructor = type.getConstructors()[0];
        if (constructor.isAnnotationPresent(IoCDependency.class)) {
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
