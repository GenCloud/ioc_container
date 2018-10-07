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
package org.ioc.utils;

import org.ioc.annotations.configuration.PropertyFunction;
import org.ioc.annotations.context.IoCComponent;
import org.ioc.annotations.context.IoCRepository;
import org.ioc.annotations.context.Mode;
import org.ioc.aop.advice.AfterAdvice;
import org.ioc.aop.advice.AroundAdivice;
import org.ioc.aop.advice.BeforeAdvice;
import org.ioc.aop.advice.ThrowingAdvice;
import org.ioc.aop.annotation.*;
import org.ioc.aop.interceptor.Interceptor;
import org.ioc.aop.selector.StandardSelector;
import org.ioc.context.type.IoCContext;
import org.ioc.exceptions.IoCInstantiateException;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Static convenience methods for components.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class ReflectionUtils {
	private static final ClassLoader classLoader = ReflectionUtils.class.getClassLoader();

	private final static Map<Method, List<Interceptor>> INTERCEPTORS = new ConcurrentHashMap<>();
	private static final String TO_STRING_METHOD_NAME = "toString";
	private static final String EQUALS_METHOD_NAME = "equals";
	private static final String HASH_CODE_METHOD_NAME = "hashCode";

	/**
	 * Function of scanning a patch to find the types filtered by bag.
	 *
	 * @param instance type for filter
	 * @param packages scanning packages
	 * @return filtered classes
	 */
	public static List<Class<?>> findClassesByInstance(Class<?> instance, String... packages) {
		return findClassesByPackage(packages).stream()
				.filter(instance::isAssignableFrom)
				.collect(Collectors.toList());
	}

	/**
	 * Function of scanning a patch to find the types filtered by annotation.
	 *
	 * @param annotation annotation for filter
	 * @param packages   scanning packages
	 * @return filtered classes
	 */
	public static List<Class<?>> findClassesByAnnotation(Class<? extends Annotation> annotation, String... packages) {
		return findClassesByPackage(packages).stream()
				.filter(cls -> cls.isAnnotationPresent(annotation))
				.collect(Collectors.toList());
	}

	/**
	 * Function of scanning a patch to find types filtered by annotation's.
	 *
	 * @param annotations collection of annotations for filter
	 * @param packages    scanning packages
	 * @return filtered classes
	 */
	public static List<Class<?>> findClassesByAnnotation(List<Class<? extends Annotation>> annotations, String... packages) {
		return findClassesByPackage(packages).stream()
				.filter(cls -> annotations.stream().anyMatch(cls::isAnnotationPresent))
				.collect(Collectors.toList());
	}

	/**
	 * Function of scanning a patch to find all classes.
	 *
	 * @param packages scanning packages
	 * @return filtered classes
	 */
	private static List<Class<?>> findClassesByPackage(String... packages) {
		final List<Class<?>> list = new LinkedList<>();
		Arrays.stream(packages).forEach(packageName -> {
			final URL url = classLoader.getResource(packageName.replace(".", "/"));
			assert url != null;
			if (!url.getProtocol().equals("jar")) {
				list.addAll(loadClassesFromPackage(new File(url.getPath()), packageName));
			} else {
				list.addAll(loadClassesFromJarEntry(url));
			}
		});
		return list;
	}

	public static <T extends Annotation> T searchAnnotation(Class<?> entityClass, Class<T> clazz) {
		final List<T> list = searchAnnotations(entityClass, clazz);
		if (list.isEmpty()) {
			return null;
		}

		return list.get(0);
	}

	public static <T extends Annotation> List<T> searchAnnotations(Class<?> entityClass, Class<T> clazz) {
		final List<T> list = new ArrayList<>();
		toClassHierarchy(entityClass).forEach(type -> {
			final T classAnnotation = type.getAnnotation(clazz);
			if (classAnnotation != null) {
				list.add(classAnnotation);
			}

			getFields(type, Collections.singleton(clazz))
					.stream()
					.map(field -> field.getAnnotation(clazz))
					.filter(Objects::nonNull)
					.forEach(list::add);
		});
		return Collections.unmodifiableList(list);
	}

	public static List<Class<?>> toClassHierarchy(Class<?> entityClass) {
		if (entityClass == null || Object.class.equals(entityClass)) {
			return Collections.emptyList();
		}

		final List<Class<?>> list = new ArrayList<>();
		list.add(entityClass);
		Class<?> parent = entityClass.getSuperclass();
		while (parent != null && !Object.class.equals(parent)) {
			list.add(parent);
			parent = parent.getSuperclass();
		}

		return Collections.unmodifiableList(list);
	}

	public static List<Field> getFields(Class<?> clazz, Collection<Class<? extends Annotation>> annotations) {
		if (clazz == null) {
			return Collections.emptyList();
		}

		final Field[] fields = clazz.getDeclaredFields();
		if (fields == null || fields.length <= 0) {
			return Collections.emptyList();
		}

		final List<Field> list = new ArrayList<>(fields.length);
		Arrays.stream(fields)
				.filter(field -> !Modifier.isTransient(field.getModifiers()))
				.forEach(field -> annotations
						.stream()
						.filter(field::isAnnotationPresent)
						.map(annotation -> field)
						.forEach(list::add));
		return list;
	}

	/**
	 * Function of loading class in heap.
	 *
	 * @param name full class name
	 * @return class
	 */
	public static Class<?> loadClass(String name) {
		try {
			return classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Instantiate object associated with class or interface with the given string name.
	 *
	 * @param cls full class name
	 */
	public static void initClass(Class<?> cls) {
		try {
			Class.forName(cls.getName());
		} catch (ClassNotFoundException e) {
			throw new IoCInstantiateException("IoCError - Can't find bag " + cls, e);
		}
	}

	@SuppressWarnings("deprecation")
	public static Object instantiateClass(Class<?> cls) {
		try {
			return cls.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new IoCInstantiateException("IoCError - Unavailable create instance of bag [" + cls + "].", e);
		}
	}

	/**
	 * Function of scanning a patch to find all classes in current package.
	 *
	 * @param filePath    path to check
	 * @param packageName name of package in path
	 * @return found classes
	 */
	private static List<Class<?>> loadClassesFromPackage(File filePath, String packageName) {
		final List<Class<?>> list = new LinkedList<>();
		Arrays.stream(Objects.requireNonNull(filePath
				.listFiles((file) -> (file.isFile() && file.getName().endsWith(".class"))
						|| file.isDirectory())))
				.forEach(file -> {
					if (file.isDirectory()) {
						list.addAll(loadClassesFromPackage(file, packageName + "." + file.getName()));
					} else {
						list.add(loadClass(packageName + "." + file.getName().substring(0, file.getName().length() - 6)));
					}
				});
		return list;
	}

	/**
	 * Function of scanning and loading classes in jar-library.
	 *
	 * @param url url for open jar entries
	 * @return found classes
	 */
	private static List<Class<?>> loadClassesFromJarEntry(URL url) {
		final List<Class<?>> list = new LinkedList<>();
		try {
			final JarURLConnection connection = (JarURLConnection) url.openConnection();
			final JarFile jarFile = connection.getJarFile();
			final Enumeration<JarEntry> jarEntries = jarFile.entries();
			while (jarEntries.hasMoreElements()) {
				final String entryName = jarEntries.nextElement().getName();
				if (entryName.endsWith(".class")) {
					list.add(loadClass(entryName.substring(0, entryName.length() - 6).replace("/", ".")));
				}
			}
		} catch (IOException e) {
			throw new IoCInstantiateException();
		}
		return list;
	}

	/**
	 * Function of returning origin class.
	 *
	 * @param type type for processing
	 * @return origin class
	 */
	public static Class<?> getOrigin(Class<?> type) {
		final int indexOf = type.getName().indexOf("$$");
		if (indexOf != -1) {
			try {
				return Class.forName(type.getName().substring(0, indexOf));
			} catch (ClassNotFoundException e) {
				throw new IoCInstantiateException();
			}
		} else {
			return type;
		}
	}

	/**
	 * Function of checking type constructors length.
	 *
	 * @param cls for check
	 * @return boolean value
	 */
	public static boolean checkType(Class<?> cls) {
		if (cls.getConstructors().length != 1) {
			throw new IoCInstantiateException("IoC can't create an instance of the class [" + cls + "]. " +
					"There are more than one public constructors so I don't know which to use. " +
					"Impossibility of injection into the standard class constructor. " +
					"Use the IoCDependency annotation to introduce dependencies!");
		}

		return true;
	}

	//################################################# AOP #############################################

	/**
	 * Function for indicate loading type in context.
	 *
	 * @param type for check
	 * @return name of type
	 */
	public static String resolveTypeName(Class<?> type) {
		String id;
		if (type.isAnnotationPresent(IoCComponent.class)
				&& !(id = type.getAnnotation(IoCComponent.class).value()).isEmpty()) {
			return id;
		}
		if (type.isAnnotationPresent(IoCRepository.class)
				&& !(id = type.getAnnotation(IoCRepository.class).value()).isEmpty()) {
			return id;
		}
		return type.getSimpleName();
	}

	/**
	 * Function for indicate loading mode.
	 *
	 * @param type for check
	 * @return loading mode
	 */
	public static Mode resolveLoadingMode(Class<?> type) {
		if (type.isAnnotationPresent(IoCComponent.class)) {
			return type.getAnnotation(IoCComponent.class).scope();
		} else if (type.isAnnotationPresent(IoCRepository.class)) {
			return type.getAnnotation(IoCRepository.class).scope();
		}
		return Mode.SINGLETON;
	}

	/**
	 * Function for indicate loading mode.
	 *
	 * @param method for check
	 * @return loading mode
	 */
	public static Mode resolveLoadingMode(Method method) {
		final PropertyFunction propertyFunction = method.getAnnotation(PropertyFunction.class);
		return propertyFunction.scope();
	}

	public static boolean isEqualsMethod(Method method) {
		return EQUALS_METHOD_NAME.equals(method.getName()) && method.getParameterTypes().length == 1
				&& Object.class.equals(method.getParameterTypes()[0]);
	}

	public static boolean isHashCodeMethod(Method method) {
		return HASH_CODE_METHOD_NAME.equals(method.getName()) && method.getParameterTypes().length == 0;
	}

	public static boolean isToStringMethod(Method method) {
		return TO_STRING_METHOD_NAME.equals(method.getName()) && method.getParameterCount() == 0;
	}

	private static Method getPointCutMethod(Method[] methods) {
		for (Method mh : methods) {
			if (mh.isAnnotationPresent(PointCut.class)) {
				return mh;
			}
		}
		return null;
	}

	/**
	 * Function of analyzing method and instantiate IoCAspect agent.
	 *
	 * @param context application context
	 * @param method  method for check and invocation
	 * @return aspect agent
	 */
	public static List<Interceptor> installAspect(IoCContext context, Method method, List<Class<?>> aspectList) {
		List<Interceptor> result = INTERCEPTORS.get(method);
		if (result == null) {
			result = new ArrayList<>();
			for (Class<?> aspect : aspectList) {
				final Method[] methods = aspect.getDeclaredMethods();
				final Method pointCutMethod = getPointCutMethod(methods);
				StandardSelector pointcut = null;

				if (pointCutMethod != null) {
					final PointCut pointCut = pointCutMethod.getAnnotation(PointCut.class);
					pointcut = new StandardSelector(pointCut.value());
				}

				for (Method mh : methods) {
					if (mh.isAnnotationPresent(PointCut.class)) {
						continue;
					}

					if (mh.isAnnotationPresent(BeforeInvocation.class)) {
						final BeforeInvocation beforeInvocation = mh.getAnnotation(BeforeInvocation.class);
						if (pointcut != null && beforeInvocation.value().startsWith(pointCutMethod.getName())
								&& pointcut.isValidForAdvisor(method)) {
							result.add(create(context, aspect, mh, BeforeInvocation.class));
						} else {
							final StandardSelector selector = new StandardSelector(beforeInvocation.value());
							if (selector.isValidForAdvisor(method)) {
								result.add(create(context, aspect, mh, BeforeInvocation.class));
							}
						}
					} else if (mh.isAnnotationPresent(AfterInvocation.class)) {
						final AfterInvocation afterInvocation = mh.getAnnotation(AfterInvocation.class);
						if (pointcut != null && afterInvocation.value().startsWith(pointCutMethod.getName())
								&& pointcut.isValidForAdvisor(method)) {
							result.add(create(context, aspect, mh, AfterInvocation.class));
						} else {
							final StandardSelector selector = new StandardSelector(afterInvocation.value());
							if (selector.isValidForAdvisor(method)) {
								result.add(create(context, aspect, mh, AfterInvocation.class));
							}
						}
					} else if (mh.isAnnotationPresent(AroundExecution.class)) {
						final AroundExecution aroundExecution = mh.getAnnotation(AroundExecution.class);
						if (pointcut != null &&
								aroundExecution.value().startsWith(pointCutMethod.getName()) &&
								pointcut.isValidForAdvisor(method)) {
							result.add(create(context, aspect, mh, AroundExecution.class));
						} else {
							final StandardSelector selector = new StandardSelector(aroundExecution.value());
							if (selector.isValidForAdvisor(method)) {
								result.add(create(context, aspect, mh, AroundExecution.class));
							}
						}
					} else if (mh.isAnnotationPresent(Throwing.class)) {
						final Throwing throwing = mh.getAnnotation(Throwing.class);
						if (pointcut != null && throwing.value().startsWith(pointCutMethod.getName())
								&& pointcut.isValidForAdvisor(method)) {
							result.add(create(context, aspect, mh, Throwing.class));
						} else {
							final StandardSelector selector = new StandardSelector(throwing.value());
							if (selector.isValidForAdvisor(method)) {
								result.add(create(context, aspect, mh, Throwing.class));
							}
						}
					}
				}
			}

			if (!result.isEmpty()) {
				INTERCEPTORS.put(method, result);
			}
		}
		return result;
	}

	public static boolean checkTypeForIntercept(Method method, List<Class<?>> aspectList) {
		for (Class<?> aspect : aspectList) {
			final Method[] methods = aspect.getDeclaredMethods();
			final Method pointCutMethod = getPointCutMethod(methods);
			StandardSelector pointcut = null;

			if (pointCutMethod != null) {
				final PointCut pointCut = pointCutMethod.getAnnotation(PointCut.class);
				pointcut = new StandardSelector(pointCut.value());
			}

			for (Method mh : methods) {
				if (mh.isAnnotationPresent(PointCut.class)) {
					continue;
				}

				if (mh.isAnnotationPresent(BeforeInvocation.class)) {
					final BeforeInvocation beforeInvocation = mh.getAnnotation(BeforeInvocation.class);
					if (pointcut != null && beforeInvocation.value().startsWith(pointCutMethod.getName())
							&& pointcut.isValidForAdvisor(method)) {
						return true;
					} else {
						final StandardSelector selector = new StandardSelector(beforeInvocation.value());
						if (selector.isValidForAdvisor(method)) {
							return true;
						}
					}
				} else if (mh.isAnnotationPresent(AfterInvocation.class)) {
					final AfterInvocation afterInvocation = mh.getAnnotation(AfterInvocation.class);
					if (pointcut != null && afterInvocation.value().startsWith(pointCutMethod.getName())
							&& pointcut.isValidForAdvisor(method)) {
						return true;
					} else {
						final StandardSelector selector = new StandardSelector(afterInvocation.value());
						if (selector.isValidForAdvisor(method)) {
							return true;
						}
					}
				} else if (mh.isAnnotationPresent(AroundExecution.class)) {
					final AroundExecution aroundExecution = mh.getAnnotation(AroundExecution.class);
					if (pointcut != null &&
							aroundExecution.value().startsWith(pointCutMethod.getName()) &&
							pointcut.isValidForAdvisor(method)) {
						return true;
					} else {
						final StandardSelector selector = new StandardSelector(aroundExecution.value());
						if (selector.isValidForAdvisor(method)) {
							return true;
						}
					}
				} else if (mh.isAnnotationPresent(Throwing.class)) {
					final Throwing throwing = mh.getAnnotation(Throwing.class);
					if (pointcut != null && throwing.value().startsWith(pointCutMethod.getName())
							&& pointcut.isValidForAdvisor(method)) {
						return true;
					} else {
						final StandardSelector selector = new StandardSelector(throwing.value());
						if (selector.isValidForAdvisor(method)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	private static Interceptor create(IoCContext context, Class classType, Method method, Class annontationType) {
		if (annontationType == AroundExecution.class) {
			return new AroundAdivice(context, classType, method);
		} else if (annontationType == BeforeInvocation.class) {
			return new BeforeAdvice(context, classType, method);
		} else if (annontationType == AfterInvocation.class) {
			return new AfterAdvice(context, classType, method);
		} else if (annontationType == Throwing.class) {
			return new ThrowingAdvice(context, classType, method);
		}
		return null;
	}
}
