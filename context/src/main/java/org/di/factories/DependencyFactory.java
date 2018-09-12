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

import org.di.annotations.LoadOpt;
import org.di.annotations.property.PropertyFunction;
import org.di.context.analyze.enums.ClassStateInjection;
import org.di.context.analyze.impl.ClassAnalyzer;
import org.di.context.analyze.impl.EnvironmentAnalyzer;
import org.di.context.analyze.impl.PostConstructAnalyzer;
import org.di.context.analyze.results.ClassAnalyzeResult;
import org.di.excepton.instantiate.IoCInstantiateException;
import org.di.factories.config.Analyzer;
import org.di.factories.config.ComponentProcessor;
import org.di.factories.config.IoCProvider;
import org.di.utils.factory.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

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
 * @date 10.09.2018
 */
public class DependencyFactory {
    /**
     * Context Analyzers
     */
    private final List<Analyzer<?, ?>> analyzers = new ArrayList<>();

    /**
     * Factory registered component processor
     */
    private final List<ComponentProcessor> componentProcessors = new ArrayList<>();

    private final Set<Class<?>> classRequests = new HashSet<>();
    /**
     * Factory instantiatable classes
     */
    private final Set<Class<?>> classesInstantiatable = new HashSet<>();
    /**
     * Factory instantiatable classes (singletons)
     */
    private final Set<Class<?>> classSingletons = new HashSet<>();
    /**
     * Factory instantiatable classes (proto)
     */
    private final Set<Class<?>> classPrototypes = new HashSet<>();
    /**
     * Factory instantiatable classes (lazy)
     */
    private final Set<Class<?>> lazys = new HashSet<>();
    /**
     * Factory of classes who have annotated method with PostConstruct
     */
    private final Set<Class<?>> postConstruction = new HashSet<>();
    /**
     * Factory providers
     */
    private final Map<String, IoCProvider> providers = new HashMap<>();
    /**
     * Factory singletons
     */
    private final Map<String, Object> singletons = new HashMap<>();
    /**
     * Factory prototypes
     */
    private final Map<String, Object> prototypes = new HashMap<>();
    /**
     * Factory interfaces
     */
    private final Map<String, Class> interfaces = new HashMap<>();

    /**
     * Return an instance, which may be shared or independent, of the specified component.
     *
     * @param type             type of the component to retrieve
     * @param postConstruction not initialize prototype type if param is true
     * @return instance of object from factory
     */
    private Object getType(Class<?> type, boolean postConstruction) {
        final ClassAnalyzeResult result = classAnalyze(type);

        Object o = null;
        final String typeName = getComponentName(type);
        if (prototypes.containsKey(typeName)) {
            if (postConstruction) {
                return prototypes.get(typeName);
            }
            o = instantiateType(prototypes.get(typeName).getClass(), null, result);
        }

        if (singletons.containsKey(typeName)) {
            o = singletons.get(typeName);
        }

        if (o == null) {
            if (lazys.contains(type)) {
                o = instantiateType(type, null, result);
                assert o != null;
                try {
                    initializePostConstruct(o);
                } catch (Exception e) {
                    throw new IoCInstantiateException("IoCError - Unavailable invoke method annotated with PostConstruct of type [" + type + "]. ");
                }
            }
        }

        return o;
    }

    /**
     * Return an instance, which may be shared or independent, of the specified component.
     *
     * @param type type of the component to retrieve
     * @return instance of object from factory
     */
    public Object getType(Class<?> type) {
        return getType(type, false);
    }

    /**
     * Function of invoke post initialization method of class annotated with {@link PostConstruct}.
     *
     * @param type maybe null
     * @throws Exception if method can't invoked
     */
    public void initializePostConstruct(Object type) throws Exception {
        if (type != null) {
            final Optional<Class<?>> opt = postConstruction
                    .stream()
                    .filter(c -> c.getSimpleName().equals(type.getClass().getSimpleName()))
                    .findFirst();
            if (opt.isPresent()) {
                invokePostConstruction(type);
            }
        } else {
            for (Class<?> c : postConstruction) {
                final Object o = getType(c, true);
                if (o != null) {
                    invokePostConstruction(o);
                }
            }
        }
    }

    /**
     * Function of invoking method annotated with {@link PostConstruct}.
     *
     * @param type type for get method annotated with {@link PostConstruct}
     * @throws Exception if method can't invoked
     */
    private void invokePostConstruction(Object type) throws Exception {
        for (Method method : type.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                method.invoke(type);
            }
        }
    }

    /**
     * Function object instantiation in the factory.
     *
     * @param request type to init instance
     * @param result  result of class analyzer
     * @param <O>     the generic type of the interface.
     * @return instantiated object in factory
     */
    @SuppressWarnings("unchecked")
    public <O> O instantiate(Class<O> request, ClassAnalyzeResult result) {
        final O instance = instantiate(request, null, result);
        final Class subclass = request.getSuperclass();
        if (subclass != Object.class) {
            if (isAbstract(subclass)) {
                installIoCInstance(subclass, instance);
            }
        }

        return instance;
    }

    /**
     * Function object instantiation in the factory.
     *
     * @param request type to init instance
     * @param parent  type-owner of {@param request}
     * @param result  result of class analyzer
     * @param <O>     the generic type of the interface.
     * @return instantiated object in factory
     */
    @SuppressWarnings("unchecked")
    private <O> O instantiate(Class<O> request, Class<?> parent, ClassAnalyzeResult result) {
        try {
            final O instance = findInstalledObject(request, parent, result);
            if (instance != null) {
                return instance;
            }

            return instantiateType(request, parent, result);
        } catch (Exception e) {
            String msg = "IoCError - Unavailable create your class collection. ";

            if (parent != null) {
                msg += "\nCannot instantiate the type [" + parent.getName() + "]. "
                        + "At least one of parameters type [" + request + "] can't be instantiated. ";
            }

            throw new IoCInstantiateException(msg, e);
        }
    }

    /**
     * Search function for an instantiated object in the factory.
     *
     * @param requestedType type to get its initialized instance already
     * @param result        result of class analyzer
     * @param <O>           the generic type of the interface.
     * @return {@code null} object if type not found
     */
    @SuppressWarnings("unchecked")
    private <O> O findInstalledObject(Class<O> requestedType, Class<?> parent, ClassAnalyzeResult result) {
        Class<O> type = requestedType;
        final String typeName = getComponentName(type);
        if (Number.class.isAssignableFrom(requestedType) || Boolean.class.isAssignableFrom(requestedType)) {
            if (singletons.containsKey(typeName)) {
                return (O) singletons.get(typeName);
            }
        }

        if (requestedType.isInterface()) {
            if (interfaces.containsKey(requestedType.getSimpleName())) {
                type = interfaces.get(requestedType.getSimpleName());
            } else if (providers.containsKey(requestedType.getSimpleName())) {
                return getInstanceFromProvider(requestedType);
            } else {
                throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + requestedType + "]. " +
                        "It is an interface and there was no implementation class mapping defined for this type. " +
                        "Please use the 'installInterface' method to define what implementing class should be used for a given interface.");
            }
        }

        if (isAbstract(requestedType)) {
            if (providers.containsKey(requestedType.getSimpleName())) {
                return getInstanceFromProvider(requestedType);
            } else {
                throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "]. " +
                        "It is an abstract class and there is no subclass for this class available. " +
                        "Please define a provider with the `installIoCProvider` method for this abstract class type.");
            }
        }

        if (classRequests.contains(type)) {
            if (!classesInstantiatable.contains(type)) {
                throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "]. " +
                        "Requested component is currently in creation: Is there an unresolvable circular reference?");
            }
        } else {
            classRequests.add(type);
        }

        if (prototypes.containsKey(typeName)) {
            return (O) instantiateType(prototypes.get(typeName).getClass(), parent, result);
        }

        if (singletons.containsKey(typeName)) {
            return (O) singletons.get(typeName);
        }

        if (providers.containsKey(typeName)) {
            final O instanceFromProvider = getInstanceFromProvider(type);
            addInstantiable(type);

            if (isSingleton(type)) {
                singletons.put(typeName, instanceFromProvider);
            } else if (isPrototype(type)) {
                prototypes.put(typeName, instanceFromProvider);
            }

            return instanceFromProvider;
        }

        return null;
    }

    /**
     * Scanner-function for detecting configuration methods in an instantiated configuration object.
     *
     * @param o instantiated environment object
     * @throws IoCInstantiateException if method have grammar error
     */
    public void instantiatePropertyMethods(Object o) throws Exception {
        final EnvironmentAnalyzer analyzer = getAnalyzer(EnvironmentAnalyzer.class);
        final boolean result = analyzer.analyze(o);
        if (result) {
            final Method[] methods = o.getClass().getDeclaredMethods();
            for (Method m : methods) {
                if (m.isAnnotationPresent(PropertyFunction.class)) {
                    final Class<?> returnType = m.getReturnType();
                    if (returnType != Void.class && !returnType.isPrimitive()) {
                        final Object returned = m.invoke(o);
                        if (m.isAnnotationPresent(Named.class)) {
                            final Named named = m.getAnnotation(Named.class);
                            final String value = named.value();
                            if (!value.isEmpty()) {
                                addToFactory(value, returned);
                                continue;
                            }
                        }
                        addToFactory(returned.getClass(), returned);
                    } else {
                        throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + returnType + "] in [" + o.getClass().getSimpleName() + "] Configuration instance." +
                                "Grammar error - returned type don't contains Void or Primitive types");
                    }
                }
            }
        }
    }

    /**
     * Create a instance of the given type.
     *
     * @param type   type for instantiate
     * @param parent type-owner of {@param type}
     * @param result result of {@link org.di.context.analyze.impl.ClassAnalyzer#analyze(Class)}
     * @param <O>    the generic type of the interface.
     * @return new instance of type
     * @throws IoCInstantiateException when type is not instantiatable
     */
    private <O> O instantiateType(Class<O> type, Class<?> parent, ClassAnalyzeResult result) throws IoCInstantiateException {
        final ClassStateInjection state = result.getClassStateInjection();
        final PostConstructAnalyzer analyzer = getAnalyzer(PostConstructAnalyzer.class);
        final boolean postConstructCheck = analyzer.analyze(type);
        if (postConstructCheck && state != LAZY_INITIALIZATION) {
            postConstruction.add(type);
        }

        if (state == LAZY_INITIALIZATION) {
            if (isSingleton(type)) {
                if (parent != null) {
                    result = classAnalyze(type);
                    return instantiateType(type, parent, result);
                }
                markTypeIsLazyInit(type);
            } else {
                throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "]." +
                        "It is not possible to lazily initialize a prototype object");
            }
        } else if (state == INJECTED_CONSTRUCTOR) {
            return instantiateConstructorType(type);
        } else if (state == INJECTED_FIELDS) {
            return instantiateFieldsType(type);
        } else if (state == INJECTED_METHODS) {
            return instantiateMethodsType(type);
        } else if (state == INJECTED_NOTHING) {
            return instantiateNothingType(type);
        } else if (state == GRAMMAR_THROW_EXCEPTION) {
            throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "]."
                    + result.getThrowableMessage());
        }

        return null;
    }

    /**
     * Marking type if it have lazy initialization.
     *
     * @param type input type
     */
    private void markTypeIsLazyInit(Class<?> type) {
        addInstantiable(type);
        lazys.add(type);
    }

    /**
     * Initializing components by just instantiate type.
     *
     * @param type type for instantiation
     */
    @SuppressWarnings("unchecked")
    private <O> O instantiateNothingType(Class<O> type) {
        O instance = ReflectionUtils.instantiate(type);
        final String typeName = getComponentName(instance.getClass());
        instance = (O) processBeforeInitialization(typeName, instance);

        addInstantiable(type);
        addToFactory(type, instance);

        instance = (O) processAfterInitialization(typeName, instance);
        return instance;
    }

    /**
     * Functional method for initializing component dependencies.
     *
     * @param type type for instantiation
     */
    @SuppressWarnings("unchecked")
    private <O> O instantiateMethodsType(Class<O> type) {
        final List<Method> methodList = findMethodsFromType(type);
        final List<Object> argumentList = methodList.stream()
                .map(method -> mapMethodType(method, type))
                .collect(Collectors.toList());

        try {
            O instance = ReflectionUtils.instantiate(type);

            final String typeName = getComponentName(instance.getClass());
            instance = (O) processBeforeInitialization(typeName, instance);

            addInstantiable(type);

            for (Method method : methodList) {
                final Object toInstantiate = argumentList
                        .stream()
                        .filter(m -> m.getClass().getSimpleName().equals(method.getParameterTypes()[0].getSimpleName()))
                        .findFirst()
                        .get();

                method.invoke(instance, toInstantiate);
            }

            addToFactory(type, instance);

            instance = (O) processAfterInitialization(typeName, instance);

            return instance;
        } catch (Exception e) {
            throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "].", e);
        }
    }

    private Object mapMethodType(Method method, Class<?> type) {
        final Class<?> paramType = method.getParameterTypes()[0];
        if (paramType.getGenericSuperclass().equals(IoCProvider.class)) {
            return populateProviderMethod(method, type, classAnalyze(paramType));
        } else {
            return instantiate(paramType, type, classAnalyze(paramType));
        }
    }

    /**
     * Initializing components by analyzing member fields.
     *
     * @param type type for instantiation
     */
    @SuppressWarnings("unchecked")
    private <O> O instantiateFieldsType(Class<O> type) {
        final List<Field> fieldList = findFieldsFromType(type);
        final List<Object> argumentList = fieldList.stream()
                .map(field -> mapFieldType(field, type))
                .collect(Collectors.toList());

        try {
            O instance = ReflectionUtils.instantiate(type);
            final String typeName = getComponentName(instance.getClass());
            instance = (O) processBeforeInitialization(typeName, instance);

            addInstantiable(type);

            for (Field field : fieldList) {
                final Object toInstantiate = argumentList
                        .stream()
                        .filter(f -> f.getClass().getSimpleName().equals(field.getType().getSimpleName()))
                        .findFirst()
                        .get();

                final boolean access = field.isAccessible();
                field.setAccessible(true);
                field.set(instance, toInstantiate);
                field.setAccessible(access);
            }

            addToFactory(type, instance);

            instance = (O) processAfterInitialization(typeName, instance);

            return instance;
        } catch (Exception e) {
            throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "].", e);
        }
    }

    private Object mapFieldType(Field field, Class<?> type) {
        final Class<?> fieldType = field.getType();
        if (field.getType().equals(IoCProvider.class)) {
            return populateProviderField(field, type, classAnalyze(fieldType));
        } else {
            return instantiate(field.getType(), type, classAnalyze(fieldType));
        }
    }

    /**
     * Initializing a component by analyzing a parameters constructor.
     *
     * @param type type for instantiation
     */
    @SuppressWarnings("unchecked")
    private <O> O instantiateConstructorType(Class<O> type) {
        final Constructor<O> oConstructor = findConstructor(type);

        if (oConstructor != null) {
            final Parameter[] constructorParameters = oConstructor.getParameters();
            final List<Object> argumentList = Arrays.stream(constructorParameters)
                    .map(param -> mapConstType(param, type))
                    .collect(Collectors.toList());

            try {
                O instance = oConstructor.newInstance(argumentList.toArray());
                final String typeName = getComponentName(instance.getClass());
                instance = (O) processBeforeInitialization(typeName, instance);

                addInstantiable(type);
                addToFactory(type, instance);

                instance = (O) processAfterInitialization(typeName, instance);
                return instance;
            } catch (Exception e) {
                throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "].", e);
            }
        }

        return null;
    }

    private Object mapConstType(Parameter param, Class<?> type) {
        final Class<?> paramType = param.getType();
        if (param.getType().equals(IoCProvider.class)) {
            return populateProviderArgument(param, type, classAnalyze(paramType));
        } else {
            return instantiate(param.getType(), type, classAnalyze(paramType));
        }
    }

    /**
     * This method is used to define what implementing class should be used for a given interface.
     * <p>
     * This way you can use interface types as dependencies in your classes and doesn't have to
     * depend on specific implementations.
     * <p>
     * But IoC needs to know what implementing class should be used when an interface type is
     * defined as dependency.
     * <p>
     * ** The second parameter has to be an actual implementing class of the interface.
     * It may not be an abstract class!
     *
     * @param interfaceType      the class type of the interface.
     * @param implementationType the class type of the implementing class.
     * @param <O>                the generic type of the interface.
     * @throws IoCInstantiateException if the first parameter is <b>not</b> an interface or the second
     *                                 parameter <b>is</b> an interface or an abstract class.
     */
    public <O> void installInterface(Class<O> interfaceType, Class<? extends O> implementationType) throws IoCInstantiateException {
        if (interfaceType.isInterface()) {
            if (implementationType.isInterface()) {
                throw new IoCInstantiateException("Type is an interface. Expecting the second argument to not be an interface but an actual class");
            } else if (isAbstract(implementationType)) {
                throw new IoCInstantiateException("Type is an abstract class. Expecting the second argument to be an actual implementing class");
            } else {
                interfaces.put(interfaceType.getSimpleName(), implementationType);
            }
        } else {
            throw new IoCInstantiateException("Type is not an interface. Expecting the first argument to be an interface.");
        }
    }

    /**
     * This method is used to define a {@link IoCProvider} for a given type.
     * <p>
     * Providers can be combined with {@link LoadOpt.Opt#SINGLETON}'s.
     * When a type is marked as singleton (has the annotation {@link LoadOpt.Opt#SINGLETON} and there is a provider
     * defined for this type, then this provider will only executed exactly one time when the type is requested the
     * first time.
     *
     * @param classType the type of the class for which the provider is used.
     * @param provider  the provider that will be called to get an instance of the given type.
     * @param <O>       the generic type of the class/interface.
     */
    private <O> void installIoCProvider(Class<O> classType, IoCProvider<O> provider) {
        final String typeName = getComponentName(classType);
        providers.put(typeName, provider);
    }

    /**
     * Function used to an instance that is used every time the given class type is requested.
     * <p>
     * This way the given instance is effectively a singleton.
     * <p>
     * This method can also be used to define instances for interfaces or abstract classes
     * that otherwise couldn't be instantiated without further configuration.
     *
     * @param classType the class type for that the instance will be bound.
     * @param instance  the instance that will be bound.
     * @param <O>       the generic type of the class.
     */
    private <O> void installIoCInstance(Class<O> classType, O instance) {
        installIoCProvider(classType, () -> instance);
    }

    /**
     * Analyzes the resulting type and adds it to the collection according to the conditions of the sample.
     *
     * @param type     input type for analyze
     * @param instance instance of {@param type}
     */
    private void addToFactory(Class<?> type, Object instance) {
        final String typeName = getComponentName(type);
        addToFactory(typeName, instance);
    }

    /**
     * Analyzes the resulting type and adds it to the collection according to the conditions of the sample.
     *
     * @param name     input name type
     * @param instance instance of {@param type}
     */
    private void addToFactory(String name, Object instance) {
        if (isSingleton(instance.getClass())) {
            singletons.put(name, instance);
        } else if (isPrototype(instance.getClass())) {
            prototypes.put(name, instance);
        }
    }

    /**
     * Custom function to mark a class as singleton.
     * <p>
     * It is an alternative for situations when you can't use the {@link LoadOpt.Opt#SINGLETON} annotation.
     * For example when you want a class from a third-party library to be a singleton.
     *
     * @param type the type that will be marked as singleton.
     */
    public void addSingleton(Class<?> type) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("The given type is an interface. Expecting the param to be an actual class");
        }

        classSingletons.add(type);
    }

    /**
     * Custom function to mark a class as prototype.
     * <p>
     * It is an alternative for situations when you can't use the {@link LoadOpt.Opt#SINGLETON} annotation.
     * For example when you want a class from a third-party library to be a singleton.
     *
     * @param type the type that will be marked as singleton.
     */
    public void addPrototype(Class<?> type) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("Type is an interface. Expecting the param to be an actual class");
        }

        classPrototypes.add(type);
    }


    /**
     * Custom function to create a {@link IoCProvider} instance when such a provider is declared as constructor parameter.
     *
     * @param param         parameter declared by the constructor
     * @param requestedType type that requested by the user. This is used to generate a proper error messages.
     * @return created provider.
     */
    private IoCProvider populateProviderArgument(Parameter param, Class<?> requestedType, ClassAnalyzeResult result) {
        try {
            if (param.getParameterizedType() instanceof ParameterizedType) {
                final ParameterizedType typeParam = (ParameterizedType) param.getParameterizedType();
                final Type providerType = typeParam.getActualTypeArguments()[0];

                return () -> instantiate((Class<?>) providerType, result);
            } else {
                throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + requestedType + "]. There is a IoCProvider without a type parameter declared as dependency. "
                        + "When using IoCProvider dependency you need to define type parameter for this provider!");
            }
        } catch (Exception ignored) {

        }

        return null;
    }

    /**
     * Custom function to create a {@link IoCProvider} instance when such a provider is declared as field type.
     *
     * @param field         parameter declared by field
     * @param requestedType type that requested by the user. This is used to generate a proper error messages.
     * @return created provider.
     */
    private IoCProvider populateProviderField(Field field, Class<?> requestedType, ClassAnalyzeResult result) {
        try {
            if (field.getGenericType() instanceof ParameterizedType) {
                final ParameterizedType typeParam = (ParameterizedType) field.getGenericType();
                final Type providerType = typeParam.getActualTypeArguments()[0];

                return () -> instantiate((Class<?>) providerType, result);
            } else {
                throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + requestedType + "]. There is a IoCProvider without a type parameter declared as dependency. "
                        + "When using IoCProvider dependency you need to define type parameter for this provider!");
            }
        } catch (Exception ignored) {

        }

        return null;
    }

    /**
     * Custom function to create a {@link IoCProvider} instance when such a provider is declared as method type.
     *
     * @param method        parameter declared by field
     * @param requestedType type that requested by the user. This is used to generate a proper error messages.
     * @return created provider.
     */
    private IoCProvider populateProviderMethod(Method method, Class<?> requestedType, ClassAnalyzeResult result) {
        try {
            final Type type = method.getParameterTypes()[0].getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                final ParameterizedType typeParam = (ParameterizedType) type;
                final Type providerType = typeParam.getActualTypeArguments()[0];

                return () -> instantiate((Class<?>) providerType, result);
            } else {
                throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + requestedType + "]. There is a IoCProvider without a type parameter declared as dependency. "
                        + "When using IoCProvider dependency you need to define type parameter for this provider!");
            }
        } catch (Exception ignored) {

        }

        return null;
    }

    /**
     * Function of calling the user settings of the type before it is directly initialized.
     *
     * @param typeName type name
     * @param o        instantiated type
     * @return modified type
     */
    private Object processBeforeInitialization(String typeName, Object o) {
        Object result = o;
        for (ComponentProcessor processor : componentProcessors) {
            result = processor.beforeComponentInitialization(typeName, o);
            if (result != null) {
                return result;
            }
        }

        return result;
    }

    /**
     * Function of calling the user settings of the type after it is directly initialized.
     *
     * @param typeName type name
     * @param o        instantiated type
     * @return modified type
     */
    private Object processAfterInitialization(String typeName, Object o) {
        Object result = o;
        for (ComponentProcessor processor : componentProcessors) {
            result = processor.afterComponentInitialization(typeName, o);
            if (result != null) {
                return result;
            }
        }

        return result;
    }

    /**
     * Add type to instantiable collection.
     *
     * @param type type for adding in to collection
     */
    private void addInstantiable(Class<?> type) {
        classesInstantiatable.add(type);
    }

    /**
     * Add collection of analyzers to main collection.
     *
     * @param collection analyzers
     */
    public void addAnalyzers(Collection<Analyzer<?, ?>> collection) {
        analyzers.addAll(collection);
    }

    /**
     * Add collection of component processors to main collection.
     *
     * @param collection processors
     */
    public void addProcessors(Collection<ComponentProcessor> collection) {
        componentProcessors.addAll(collection);
    }

    /**
     * Custom function for initialization of environments.
     *
     * @param o environment instantiated object
     */
    public void addInstalledConfiguration(Object o) {
        addInstantiable(o.getClass());
        singletons.put(o.getClass().getSimpleName(), o);
    }

    /**
     * Check if type is singleton.
     *
     * @param type type for check
     * @return {@code true} if type have state injection of {@link LoadOpt.Opt#SINGLETON}
     */
    private boolean isSingleton(Class<?> type) {
        if (type.isAnnotationPresent(LoadOpt.class)) {
            return type.getAnnotation(LoadOpt.class).value() == SINGLETON;
        } else if (classSingletons.contains(type)) {
            return true;
        } else {
            return !classPrototypes.contains(type);
        }
    }

    /**
     * Check if type is prototype.
     *
     * @param type type for check
     * @return {@code true} if type have state injection of {@link LoadOpt.Opt#PROTOTYPE}
     */
    private boolean isPrototype(Class<?> type) {
        if (type.isAnnotationPresent(LoadOpt.class)) {
            return type.getAnnotation(LoadOpt.class).value() == PROTOTYPE;
        } else {
            return classPrototypes.contains(type);
        }
    }

    public Map<String, Object> getSingletons() {
        return singletons;
    }

    public Map<String, Object> getPrototypes() {
        return prototypes;
    }

    /**
     * Get instance type from a provider.
     *
     * @param type type for instantiate provider.
     * @param <O>  the generic type of the class.
     * @return instance provider of type
     * @throws IoCInstantiateException if provider don't instantiable
     */
    @SuppressWarnings("unchecked")
    private <O> O getInstanceFromProvider(Class<O> type) throws IoCInstantiateException {
        try {
            final String typeName = getComponentName(type);
            final IoCProvider<O> provider = providers.get(typeName);
            return provider.getInstance();
        } catch (Exception e) {
            throw new IoCInstantiateException("IoCError - Unavailable create instance of type [" + type + "].", e);
        }
    }

    private ClassAnalyzeResult classAnalyze(Class<?> type) {
        final ClassAnalyzer analyzer = getAnalyzer(ClassAnalyzer.class);
        if (!analyzer.supportFor(type)) {
            throw new IoCInstantiateException("It is impossible to test, check the class for type match!");
        }

        return analyzer.analyze(type, true);
    }

    /**
     * The analyzer search function by its class name.
     *
     * @param cls class analyzer
     * @return found analyzer
     */
    @SuppressWarnings("unchecked")
    public <O extends Analyzer<?, ?>> O getAnalyzer(Class<O> cls) {
        return (O) analyzers.stream().filter(a -> a.getClass().getSimpleName().equals(cls.getSimpleName())).findFirst().orElse(null);
    }
}
