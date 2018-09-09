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

import org.di.excepton.instantiate.IoCInstantiateException;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;

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
    public static Reflections configureReflection(Class<?>[] classes) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        for (Class<?> clazz : classes) {
            builder.setUrls(ClasspathHelper.forClass(clazz));
        }
        return new Reflections(builder);
    }

    /**
     * Configuring a Reflections instance for scan classpath
     * @param paths - source packages
     * @return - new Reflections instance
     */
    public static Reflections configureReflection(String[] paths) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        for (String path : paths) {
            builder.setUrls(ClasspathHelper.forPackage(path));
        }
        return new Reflections(builder);
    }

    /**
     * @param type - class to check
     * @return - {@code} true if class for check is not an primitive, array
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
            throw new IoCInstantiateException(clazz, "Specified class is an interface");
        }

        try {
            return clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new IoCInstantiateException(clazz, "Is it an abstract class?");
        } catch (IllegalAccessException ex) {
            throw new IoCInstantiateException(clazz, "Is the constructor accessible?");
        }
    }
}
