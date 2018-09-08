package org.di.context.analyze.impl;

import org.di.annotations.Component;
import org.di.annotations.Dependency;
import org.di.context.analyze.Analyzer;
import org.di.context.analyze.results.CyclicDependencyResult;
import org.di.excepton.instantiate.IntroInstantiateException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.di.context.analyze.enums.CyclicDependencyState.TRUE;
import static org.di.utils.factory.ReflectionUtils.checkClass;
import static org.di.utils.factory.ReflectionUtils.checkTypes;

/**
 * @author GenCloud
 * @date 06.09.2018
 */
public class CyclicDependenciesAnalyzer implements Analyzer<CyclicDependencyResult, List<Class<?>>> {
    private final Map<String, ClassDefinition> classDefinitions = new HashMap<>();

    @Override
    public CyclicDependencyResult analyze(List<Class<?>> tested) throws Exception {
        for (Class<?> type : tested) {
            final ClassDefinition main = new ClassDefinition();
            addToMap(type.getSimpleName(), main);

            final Constructor<?> constructor = type.getConstructors()[0];
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            final ClassDefinition parent = checkIncluded(type);
            if (parameterTypes.length > 0) {
                for (Class<?> parameter : parameterTypes) {
                    if (parameter.isAnnotationPresent(Component.class)) {
                        if (!checkClass(parameter) && !checkTypes(parameter)) {
                            continue;
                        }

                        ClassDefinition definition = checkIncluded(parameter);
                        if (definition == null) {
                            definition = new ClassDefinition();
                            addToMap(parameter.getSimpleName(), definition);
                            analyze(Collections.singletonList(parameter));
                            continue;
                        }

                        if (parent != null) {
                            definition.setParent(parent);
                        }

                        addDependencies(parameter, definition);
                    }
                }
            }

            final Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Dependency.class)) {
                    final Class<?> fieldType = field.getType();
                    if (!checkClass(fieldType) && !checkTypes(fieldType)) {
                        continue;
                    }

                    ClassDefinition definition = checkIncluded(fieldType);
                    if (definition == null) {
                        definition = new ClassDefinition();
                        addToMap(fieldType.getSimpleName(), definition);
                        analyze(Collections.singletonList(fieldType));
                        continue;
                    }

                    if (parent != null) {
                        definition.setParent(parent);
                    }

                    addDependencies(fieldType, definition);
                }
            }

            final Method[] methods = type.getDeclaredMethods();
            if (methods.length > 1) {
                for (Method method : methods) {
                    if (method.isAnnotationPresent(Dependency.class)) {
                        if (method.getParameterCount() == 0) {
                            throw new IntroInstantiateException("Impossibility of injection into a function with fewer parameters than one");
                        }

                        if (method.getParameterCount() > 1) {
                            throw new IntroInstantiateException("Inability to inject a function with more than one parameter");
                        }

                        final Class<?> methodParameterType = method.getParameterTypes()[0];

                        if (!checkClass(methodParameterType) && !checkTypes(methodParameterType)) {
                            continue;
                        }

                        ClassDefinition definition = checkIncluded(methodParameterType);
                        if (definition == null) {
                            definition = new ClassDefinition();
                            addToMap(methodParameterType.getSimpleName(), definition);
                            analyze(Collections.singletonList(methodParameterType));
                            continue;
                        }

                        if (parent != null) {
                            definition.setParent(parent);
                        }

                        addDependencies(methodParameterType, definition);
                    }
                }
            }
        }

        return checkCyclicDependencies();
    }

    @Override
    public boolean supportFor(List<Class<?>> tested) {
        return true;
    }

    private CyclicDependencyResult checkCyclicDependencies() {
        for (Map.Entry<String, ClassDefinition> entry : classDefinitions.entrySet()) {
            final ClassDefinition def = entry.getValue();
            final ClassDefinition parent = def.getParent();
            if (parent != null) {
                for (Class<?> type : parent.getDependencies()) {
                    if (type.getSimpleName().equals(entry.getKey())) {
                        for (Class<?> defType : entry.getValue().getDependencies()) {
                            if (defType.getSimpleName().equals(type.getSimpleName())) {
                                return new CyclicDependencyResult("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
                            }
                        }
                    }
                }
            }
        }

        return new CyclicDependencyResult(TRUE);
    }

    private ClassDefinition checkIncluded(Class<?> parameter) {
        final Optional<ClassDefinition> optional = classDefinitions.entrySet()
                .stream()
                .filter(e -> e.getKey().equals(parameter.getSimpleName()))
                .map(Map.Entry::getValue)
                .findFirst();
        return optional.orElse(null);
    }

    private void addDependencies(Class<?> type, ClassDefinition definition) {
        if (definition.getDependencies() == null) {
            definition.setDependencies(new ArrayList<>());
        }

        definition.getDependencies().add(type);
    }

    private void addToMap(String name, ClassDefinition definition) {
        if (!classDefinitions.containsKey(name)) {
            classDefinitions.put(name, definition);
        }
    }

    private static class ClassDefinition {
        private List<Class<?>> dependencies;
        private ClassDefinition parent;

        public List<Class<?>> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<Class<?>> dependencies) {
            this.dependencies = dependencies;
        }

        public ClassDefinition getParent() {
            return parent;
        }

        public void setParent(ClassDefinition parent) {
            this.parent = parent;
        }
    }

}
