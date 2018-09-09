package org.di.context.analyze.impl;

import org.di.annotations.Dependency;
import org.di.context.analyze.Analyzer;
import org.di.context.analyze.results.ClassAnalyzeResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.di.context.analyze.enums.ClassStateInjection.*;


/**
 * Analyzer for detecting the method of injection used.
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public class ClassAnalyzer implements Analyzer<ClassAnalyzeResult, Class<?>> {
    @Override
    public ClassAnalyzeResult analyze(Class<?> tested) {
        final Constructor<?>[] constructors = tested.getConstructors();
        if (constructors.length > 1) {
            return new ClassAnalyzeResult("Inability to inject a class with more than one constructor!");
        }

        final Constructor<?> constructor = constructors[0];
        if (constructor.getParameterCount() > 0) {
            if (!constructor.isAnnotationPresent(Dependency.class)) {
                return new ClassAnalyzeResult("Impossibility of injection into the standard class constructor. Use the Introduction annotation to introduce dependencies!");
            }

            return new ClassAnalyzeResult(INJECTED_CONSTRUCTOR);
        }

        final Field[] fields = tested.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Dependency.class)) {
                return new ClassAnalyzeResult(INJECTED_FIELDS);
            }
        }

        final Method[] methods = tested.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Dependency.class)) {
                if (method.getParameterCount() == 0) {
                    return new ClassAnalyzeResult("Impossibility of injection into a function with fewer parameters than one");
                }

                if (method.getParameterCount() > 1) {
                    return new ClassAnalyzeResult("Inability to inject a function with more than one parameter");
                }

                return new ClassAnalyzeResult(INJECTED_METHODS);
            }
        }
        return new ClassAnalyzeResult(INJECTED_NOTHING);
    }

    @Override
    public boolean supportFor(Class<?> tested) {
        return !tested.isAnnotation() & !tested.isArray() && !tested.isEnum() && !tested.isInterface();
    }
}