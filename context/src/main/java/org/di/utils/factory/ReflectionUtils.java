package org.di.utils.factory;

import org.di.excepton.instantiate.IntroInstantiateException;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

/**
 * @author GenCloud
 * @date 05.09.2018
 */
public class ReflectionUtils {
    public static Reflections configureReflection(Class<?>[] classes) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        for (Class<?> clazz : classes) {
            builder.setUrls(ClasspathHelper.forClass(clazz));
        }
        return new Reflections(builder);
    }

    public static Reflections configureReflection(String[] paths) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        for (String path : paths) {
            builder.setUrls(ClasspathHelper.forPackage(path));
        }
        return new Reflections(builder);
    }

    public static Reflections configureReflectionWithSystemClassLoaders(String[] paths) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setClassLoaders(new ClassLoader[]{Thread.currentThread().getContextClassLoader()});
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        for (String path : paths) {
            builder.setUrls(ClasspathHelper.forPackage(path));
        }
        return new Reflections(builder);
    }

    public static Reflections configureReflectionWithSystemClassLoaders(Class<?>[] classes) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setClassLoaders(new ClassLoader[]{Thread.currentThread().getContextClassLoader()});
        for (Class<?> clazz : classes) {
            builder.setUrls(ClasspathHelper.forClass(clazz));
        }
        return new Reflections(builder);
    }

    public static boolean checkTypes(Class<?> param) {
        return !param.isPrimitive() && !param.isArray() && !param.isEnum();
    }

    public static boolean checkClass(Class<?> component) {
        return !component.isInterface() && !component.isEnum() && !component.isArray();
    }

    public static <T> T instantiate(Class<T> clazz) throws IntroInstantiateException {
        if (clazz.isInterface()) {
            throw new IntroInstantiateException(clazz, "Specified class is an interface");
        }

        try {
            return clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new IntroInstantiateException(clazz, "Is it an abstract class?");
        } catch (IllegalAccessException ex) {
            throw new IntroInstantiateException(clazz, "Is the constructor accessible?");
        }
    }
}
