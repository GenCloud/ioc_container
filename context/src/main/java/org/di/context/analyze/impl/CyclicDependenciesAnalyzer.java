//package org.di.context.analyze.impl;
//
//import org.di.annotations.Component;
//import org.di.annotations.Dependency;
//import org.di.context.analyze.Analyzer;
//import org.di.context.analyze.results.CyclicDependencyResult;
//import org.di.excepton.instantiate.IntroInstantiateException;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.util.*;
//import java.util.Map.Entry;
//
//import static org.di.context.analyze.enums.CyclicDependencyState.TRUE;
//import static org.di.utils.factory.ReflectionUtils.checkClass;
//import static org.di.utils.factory.ReflectionUtils.checkTypes;
//
///**
// * @author GenCloud
// * @date 06.09.2018
// */
//public class CyclicDependenciesAnalyzer implements Analyzer<CyclicDependencyResult, List<Class<?>>> {
//    private final Map<String, ClassDefinition> classDefinitions = new HashMap<>();
//
//    @Override
//    public CyclicDependencyResult analyze(List<Class<?>> tested) throws Exception {
//        for (Class<?> type : tested) {
//            checkType(type);
//        }
//
//        return checkCyclicDependencies();
//    }
//
//    @Override
//    public boolean supportFor(List<Class<?>> tested) {
//        return true;
//    }
//
//    private void checkType(Class<?> type) throws IntroInstantiateException {
//        final Constructor<?> constructor = type.getConstructors()[0];
//        final Class<?>[] parameterTypes = constructor.getParameterTypes();
//        final ClassDefinition definition = new ClassDefinition();
//        if (parameterTypes.length > 0) {
//            for (Class<?> parameter : parameterTypes) {
//                if (parameter.isAnnotationPresent(Component.class)) {
//                    if (!checkClass(parameter) && !checkTypes(parameter)) {
//                        continue;
//                    }
//
//                    addDependencies(parameter, definition);
//                    checkType(type);
//                }
//            }
//        }
//
//        final Field[] fields = type.getDeclaredFields();
//        for (Field field : fields) {
//            if (field.isAnnotationPresent(Dependency.class)) {
//                final Class<?> fieldType = field.getType();
//                if (!checkClass(fieldType) && !checkTypes(fieldType)) {
//                    continue;
//                }
//
//                addDependencies(fieldType, definition);
//                checkType(fieldType);
//            }
//        }
//
//        final Method[] methods = type.getDeclaredMethods();
//        if (methods.length > 1) {
//            for (Method method : methods) {
//                if (method.isAnnotationPresent(Dependency.class)) {
//                    if (method.getParameterCount() == 0) {
//                        throw new IntroInstantiateException("Impossibility of injection into a function with fewer parameters than one");
//                    }
//
//                    if (method.getParameterCount() > 1) {
//                        throw new IntroInstantiateException("Inability to inject a function with more than one parameter");
//                    }
//
//                    final Class<?> methodParameterType = method.getParameterTypes()[0];
//
//                    if (!checkClass(methodParameterType) && !checkTypes(methodParameterType)) {
//                        continue;
//                    }
//
//                    addDependencies(methodParameterType, definition);
//                    checkType(methodParameterType);
//                }
//            }
//        }
//
//        addToMap(type.getSimpleName(), definition);
//    }
//
//    private CyclicDependencyResult checkCyclicDependencies() {
//        for (Entry<String, ClassDefinition> entry : classDefinitions.entrySet()) {
//            final String key = entry.getKey();
//            final ClassDefinition value = entry.getValue();
//            final CyclicDependencyResult result = checkEntry(key, value);
//            if (result.getCyclicDependencyState() == TRUE) {
//                continue;
//            }
//
//            return result;
//        }
//
//        return new CyclicDependencyResult(TRUE);
//    }
//
//    private CyclicDependencyResult checkEntry(String cur, ClassDefinition value) {
//        final List<Class<?>> classes = value.getDependencies();
//        if (classes != null && !classes.isEmpty()) {
//            for (Class<?> type : classes) {
//                final ClassDefinition dep = classDefinitions.get(type.getSimpleName());
//                if (dep != null) {
//                    final List<Class<?>> classDeps = dep.getDependencies();
//                    if (classDeps != null && !classDeps.isEmpty()) {
//                        for (Class<?> type_dep : classDeps) {
//                            if (type_dep.getSimpleName().equals(cur)) {
//                                if (!type_dep.isAnnotationPresent(Lazy.class)) {
//                                    return new CyclicDependencyResult("Component: " + type.getSimpleName() + ". Requested component is currently in creation: Is there an unresolvable circular reference?");
//                                }
//                            }
//                        }
//                    }
//                }
//
//            }
//        }
//
//        return new CyclicDependencyResult(TRUE);
//    }
//
//    private void addDependencies(Class<?> type, ClassDefinition definition) {
//        if (definition.getDependencies() == null) {
//            definition.setDependencies(new ArrayList<>());
//        }
//
//        final Optional<Class<?>> optional = definition.getDependencies().stream().filter(c -> c.getSimpleName().equals(type.getSimpleName())).findFirst();
//        if (!optional.isPresent()) {
//            definition.getDependencies().add(type);
//        }
//    }
//
//    private void addToMap(String name, ClassDefinition definition) {
//        if (!classDefinitions.containsKey(name)) {
//            classDefinitions.put(name, definition);
//        } else {
//            final ClassDefinition old = classDefinitions.get(name);
//            classDefinitions.replace(name, old, definition);
//        }
//    }
//
//    private static class ClassDefinition {
//        private List<Class<?>> dependencies;
//        private ClassDefinition parent;
//
//        public List<Class<?>> getDependencies() {
//            return dependencies;
//        }
//
//        public void setDependencies(List<Class<?>> dependencies) {
//            this.dependencies = dependencies;
//        }
//
//        public ClassDefinition getParent() {
//            return parent;
//        }
//
//        public void setParent(ClassDefinition parent) {
//            this.parent = parent;
//        }
//    }
//
//}
