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
package org.di.utils.factory;

import org.di.annotations.IoCComponent;
import org.di.annotations.IoCDependency;
import org.di.annotations.ScanPackage;
import org.di.excepton.instantiate.IoCInstantiateException;
import org.di.excepton.starter.IoCStartException;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static convenience methods for components
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public class ReflectionUtils {
    /**
     * Configuring a Reflections instance for scan classpath
     *
     * @param classes - source classes for scan him packages
     * @return - new Reflections instance
     */
    private static Reflections configureReflection(Class<?>[] classes) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.filterInputsBy(new FilterBuilder().include(".*\\.class"));
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        for (Class<?> clazz : classes) {
            builder.setUrls(ClasspathHelper.forClass(clazz));
        }
        return new Reflections(builder);
    }

    /**
     * Configuring a Reflections instance for scan classpath
     *
     * @param paths - source packages
     * @return - new Reflections instance
     */
    private static Reflections configureReflection(String[] paths) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.filterInputsBy(new FilterBuilder().include(".*\\.class"));
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        for (String path : paths) {
            builder.setUrls(ClasspathHelper.forPackage(path));
        }
        return new Reflections(builder);
    }

    /**
     * Configuring a Reflections instance for scan classpath
     *
     * @param mainSource - source class for scan him packages
     * @return - new Reflections instance
     */
    public static Reflections configureScanner(Class<?> mainSource) {
        Reflections reflections;

        final ScanPackage scanPackage = mainSource.getAnnotation(ScanPackage.class);
        if (scanPackage != null) {
            final String[] packages = scanPackage.packages();
            final Class<?>[] classes = scanPackage.classes();
            if (packages.length > 0) {
                reflections = configureReflection(packages);
            } else if (classes.length > 0) {
                reflections = configureReflection(classes);
            } else {
                throw new IoCStartException("IoCError - Unavailable create reflections [no find packages or classes to scan].");
            }
        } else {
            reflections = configureReflection(new Class[]{mainSource});
        }

        return reflections;
    }

    /**
     * @param type class to check
     * @return @return {@code true} if class for check is not an primitive, assigned Number or Boolean
     */
    public static boolean checkPropertyType(Class<?> type) {
        return !type.isPrimitive() && !Number.class.isAssignableFrom(type) && !Boolean.class.isAssignableFrom(type);
    }

    /**
     * @param type class to check
     * @return {@code true} if class for check is not an primitive, array
     */
    public static boolean checkTypes(Class<?> type) {
        return !type.isPrimitive() && !type.isArray();
    }

    /**
     * @param type - class to check
     * @return - {@code} true if class for check is not an annotation, interface, abstract, enum
     */
    public static boolean checkClass(Class<?> type) {
        return !type.isAnnotation() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers()) && !type.isEnum();
    }

    /**
     * Convenience method to instantiate a class using its no-arg constructor.
     *
     * @param clazz class to instantiate
     * @return the new instance
     * @throws IoCInstantiateException if the component cannot be instantiated
     * @see Class#newInstance()
     */
    public static <T> T instantiate(Class<T> clazz) throws IoCInstantiateException {
        if (clazz.isInterface()) {
            throw new IoCInstantiateException(clazz, "IoCError - Unavailable create instance of type [" + clazz + "]. " +
                    "Specified class is an interface");
        }

        try {
            return clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new IoCInstantiateException(clazz, "IoCError - Unavailable create instance of type [" + clazz + "]. " +
                    "Is it an abstract class?");
        } catch (IllegalAccessException ex) {
            throw new IoCInstantiateException(clazz, "IoCError - Unavailable create instance of type [" + clazz + "]. " +
                    "Is the constructor accessible?");
        }
    }

    /**
     * Find out the constructor that will be used for instantiation.
     * <p>
     * If there are more then one public constructors, the one with an {@link IoCDependency}
     * annotation is used.
     * <p>
     * In all other cases an {@link IoCInstantiateException} is thrown.
     *
     * @param type the class of which the constructor is searched for.
     * @param <O>  the generic type of the class.
     * @return the constructor to use
     * @throws IoCInstantiateException when any constructors not annotated with {@link IoCDependency}
     */
    @SuppressWarnings("unchecked")
    public static <O> Constructor<O> findConstructor(Class<O> type) throws IoCInstantiateException {
        final Constructor<?>[] constructors = type.getConstructors();

        if (constructors.length > 0) {
            final List<Constructor<?>> collect = Arrays
                    .stream(constructors)
                    .filter(c -> c.isAnnotationPresent(IoCDependency.class))
                    .collect(Collectors.toList());

            if (collect.size() == 0) {
                return null;
            }

            if (collect.size() > 1) {
                throw new IoCInstantiateException("IoC can't create an instance of the class [" + type + "]. " +
                        "There are more than one public constructors so I don't know which to use. " +
                        "Impossibility of injection into the standard class constructor. " +
                        "Use the IoCDependency annotation to introduce dependencies!");
            } else {
                return (Constructor<O>) collect.get(0);
            }
        }

        return null;
    }

    /**
     * Find out the specific fields annotated by {@link IoCDependency} that will be used for instantiation.
     *
     * @param type type for find declared fields
     * @return collection of type fields
     */
    public static List<Field> findFieldsFromType(Class<?> type) {
        final Field[] fields = type.getDeclaredFields();
        return Arrays
                .stream(fields)
                .filter(f -> f.isAnnotationPresent(IoCDependency.class))
                .collect(Collectors.toList());
    }

    /**
     * Find out the specific methods annotated by {@link IoCDependency} that will be used for instantiation.
     *
     * @param type type for find declared methods
     * @return collection of type methods
     */
    public static List<Method> findMethodsFromType(Class<?> type) {
        final Method[] methods = type.getDeclaredMethods();
        return Arrays
                .stream(methods)
                .filter(f -> f.isAnnotationPresent(IoCDependency.class))
                .collect(Collectors.toList());
    }

    /**
     * This helper method returns {@code true} only if the given
     * class type is an abstract class.
     *
     * @param type the class type to check
     * @return {@code true} if the given type is an abstract class, otherwise {@code false}
     */
    public static boolean isAbstract(Class<?> type) {
        return !type.isInterface() && Modifier.isAbstract(type.getModifiers());
    }

    /**
     * Function for get component qualified name.
     *
     * @param type type for nameable
     * @return type simple name
     */
    public static String getComponentName(Class<?> type) {
        final IoCComponent annotation = type.getAnnotation(IoCComponent.class);
        return annotation != null && !annotation.name().isEmpty() ? annotation.name() : type.getSimpleName();
    }
}
