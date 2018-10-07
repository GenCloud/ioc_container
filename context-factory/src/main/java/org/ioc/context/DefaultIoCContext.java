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
package org.ioc.context;

import org.ioc.annotations.configuration.Property;
import org.ioc.annotations.configuration.PropertyFunction;
import org.ioc.annotations.context.*;
import org.ioc.annotations.modules.CacheModule;
import org.ioc.annotations.modules.DatabaseModule;
import org.ioc.annotations.modules.ThreadingModule;
import org.ioc.aop.DynamicProxy;
import org.ioc.context.factories.Factory;
import org.ioc.context.factories.*;
import org.ioc.context.listeners.IListener;
import org.ioc.context.listeners.facts.OnContextDestroyFact;
import org.ioc.context.listeners.facts.OnTypeInitFact;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.processors.DestroyProcessor;
import org.ioc.context.processors.TypeProcessor;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.context.sensible.Sensible;
import org.ioc.context.sensible.factories.CacheFactorySensible;
import org.ioc.context.sensible.factories.ThreadFactorySensible;
import org.ioc.context.type.IoCContext;
import org.ioc.enviroment.configurations.FactDispatcherAutoConfiguration;
import org.ioc.enviroment.loader.EnvironmentLoader;
import org.ioc.exceptions.IoCException;
import org.ioc.exceptions.IoCInstantiateException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.ioc.utils.ReflectionUtils.*;

/**
 * Central class to provide configuration for an application.
 *
 * @author GenCloud
 * @date 09/2018
 */
public class DefaultIoCContext extends AbstractIoCContext {
	private final InstanceFactory instanceFactory = new InstanceFactory(this);

	private final List<TypeProcessor> typeProcessors = new ArrayList<>();

	private final SingletonFactory singletonFactory = new SingletonFactory(instanceFactory);
	private final PrototypeFactory prototypeFactory = new PrototypeFactory(instanceFactory);

	private List<Class<?>> aspects;

	private FactDispatcherFactory dispatcherFactory;

	private String[] packages;

	@Override
	public String[] getPackages() {
		return packages;
	}

	public FactDispatcherFactory getDispatcherFactory() {
		if (dispatcherFactory == null) {
			final FactDispatcherFactory dispatcher = getType(FactDispatcherFactory.class);
			setDispatcherFactory(dispatcher);
		}
		return dispatcherFactory;
	}

	private void setDispatcherFactory(FactDispatcherFactory dispatcherFactory) {
		if (this.dispatcherFactory == null) {
			this.dispatcherFactory = dispatcherFactory;
		}
	}

	/**
	 * Main function of initialization {@link IoCContext}.
	 *
	 * @param mainSource class starter
	 * @param packages   packages to scan
	 */
	public void init(Class<?> mainSource, String... packages) {
		this.packages = packages;

		final List<TypeMetadata> types = findMetadataInClassPathByAnnotation(IoCComponent.class, packages)
				.stream()
				.filter(t -> !t.getType().isAnnotationPresent(Lazy.class))
				.collect(Collectors.toList());

		final List<TypeMetadata> lazyTypes = findMetadataInClassPath(packages)
				.stream()
				.filter(t -> t.getType().isAnnotationPresent(Lazy.class) && t.getMode() == Mode.SINGLETON)
				.collect(Collectors.toList());

		lazyTypes.forEach(singletonFactory::addType);

		final List<TypeProcessor> processors = findInstancesInClassPathByInstance(TypeProcessor.class, packages);

		List<Class<?>> configurations = findClassesByAnnotation(Property.class, packages)
				.stream()
				.filter(c -> !c.getAnnotation(Property.class).ignore())
				.collect(Collectors.toList());

		aspects = extractAspect(packages);

		configurations.add(FactDispatcherAutoConfiguration.class);

		if (mainSource.isAnnotationPresent(ThreadingModule.class)) {
			final ThreadingModule annotation = mainSource.getAnnotation(ThreadingModule.class);
			configurations.add(annotation.autoConfigurationClass());
			processors.add((TypeProcessor) instantiateClass(loadClass("org.ioc.threads.processors.ThreadConfigureProcessor")));
		}

		if (mainSource.isAnnotationPresent(CacheModule.class)) {
			final CacheModule annotation = mainSource.getAnnotation(CacheModule.class);
			configurations.add(annotation.autoConfigurationClass());
		}

		if (mainSource.isAnnotationPresent(DatabaseModule.class)) {
			final DatabaseModule annotation = mainSource.getAnnotation(DatabaseModule.class);
			configurations.add(annotation.autoConfigurationClass());
		}

		if (mainSource.isAnnotationPresent(Exclude.class)) {
			final Class<?>[] excluded = mainSource.getAnnotation(Exclude.class).excludedConfigurations();
			configurations = configurations.stream().filter(c -> !Arrays.asList(excluded).contains(c)).collect(Collectors.toList());
		}

		registerEnvironments(configurations);

		registerTypeProcessor(processors);

		initFactories();

		registerListeners(packages);

		registerTypes(types);

		initPostConstruction(singletonFactory.getTypes().values());
	}

	private void registerListeners(String... packages) {
		final List<IListener> listeners = findInstancesInClassPathByInstance(IListener.class, packages);
		listeners.forEach(l -> {
			instantiateSensibles(l);
			getDispatcherFactory().addListener(l);
		});
	}

	private void initFactories() {
		final List<TypeMetadata> metadatas = singletonFactory.getTypes()
				.values()
				.stream()
				.filter(t -> Factory.class.isAssignableFrom(t.getType()))
				.sorted((o1, o2) -> {
					final int order_1 = getOrder(o1.getType());
					final int order_2 = getOrder(o2.getType());
					return order_2 - order_1;
				}).collect(Collectors.toList());

		metadatas.forEach(t -> ((Factory) t.getInstance()).initialize());
	}

	private int getOrder(Class<?> type) {
		int order = 1;
		if (type.isAnnotationPresent(Order.class)) {
			order = type.getAnnotation(Order.class).value();
		}

		return order;
	}

	/**
	 * Function of reading default property path, init configuration and bag-functions in context.
	 *
	 * @param list collection of configurations
	 */
	@SuppressWarnings("deprecation")
	private void registerEnvironments(List<Class<?>> list) {
		final List<TypeMetadata> types = new LinkedList<>();
		for (Class<?> propertyClass : list) {
			final Property property = propertyClass.getAnnotation(Property.class);
			if (property.ignore()) {
				continue;
			}

			final Path path = Paths.get(property.path());

			try {
				final Object instance = propertyClass.newInstance();
				EnvironmentLoader.parse(instance, path.toFile());
				final TypeMetadata typeMetadata = new TypeMetadata(resolveTypeName(instance.getClass()), instance, Mode.SINGLETON);
				types.add(typeMetadata);

				for (Method method : propertyClass.getDeclaredMethods()) {
					if (method.isAnnotationPresent(PropertyFunction.class)) {
						final Object returned = method.invoke(instance);
						types.add(new TypeMetadata(resolveTypeName(returned.getClass()), returned, resolveLoadingMode(method)));
					}
				}

			} catch (Exception e) {
				throw new IoCException("IoCError - Failed to Load " + path + " Config File", e);
			}
		}

		registerTypes(types);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O> O getType(String name) {
		Object type = prototypeFactory.getType(name);
		if (type == null) {
			type = singletonFactory.getType(name);
		} else {
			initPostConstruction(type);
		}

		return (O) type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O> O getType(Class<O> type) {
		Object o = getType(type.getTypeName());
		if (o == null) {
			o = prototypeFactory.getType(type);
			if (o == null) {
				o = singletonFactory.getType(type);
			} else {
				initPostConstruction(o);
			}
		}

		return (O) o;
	}

	@Override
	public void setType(String name, Object instance) {
		final Class<?> type = instance.getClass();
		final Mode mode = resolveLoadingMode(type);
		final TypeMetadata metadata = new TypeMetadata(name, instance, mode);
		if (mode == Mode.SINGLETON) {
			singletonFactory.addType(metadata);
		} else {
			prototypeFactory.addType(metadata);
		}
	}

	@Override
	public List<TypeMetadata> registerTypes(List<TypeMetadata> types) {
		types.forEach(type -> {
			if (type.getInstance() != null) {
				instantiateSensibles(type.getInstance());
			} else {
				Object instance = instanceFactory.instantiate(type.getConstructor());

				instantiateSensibles(instance);

				instance = processBeforeInitialization(type.getName(), instance);

				instance = viewAspect(instance, aspects);

				type.setInstance(instance);

				instance = processAfterInitialization(type.getName(), instance);

				type.setInitialized(true);

				getDispatcherFactory().fireEvent(new OnTypeInitFact(type.getName(), instance));
			}

			if (type.getMode() == Mode.PROTOTYPE) {
				prototypeFactory.addType(type);
			} else {
				singletonFactory.addType(type);
			}
		});

		return types;
	}

	@Override
	public List<TypeMetadata> registerLazy(List<TypeMetadata> types) {
		types.forEach(type -> {
			Object instance = instanceFactory.instantiate(type.getConstructor());

			instantiateSensibles(instance);

			instance = processBeforeInitialization(type.getName(), instance);

			instance = viewAspect(instance, aspects);

			type.setInstance(instance);

			instance = processAfterInitialization(type.getName(), instance);

			type.setInitialized(true);

			getDispatcherFactory().fireEvent(new OnTypeInitFact(type.getName(), instance));

			singletonFactory.addType(type);
		});

		return types;
	}

	@Override
	public void destroy() {
		getDispatcherFactory().fireEvent(new OnContextDestroyFact(this));

		prototypeFactory.getTypes().values().forEach(this::destroy);
		singletonFactory.getTypes().values().forEach(this::destroy);
	}

	/**
	 * Function of call of destruction of a component.
	 *
	 * @param type bag for check
	 */
	private void destroy(TypeMetadata type) {
		final Class<?> c = type.getInstance().getClass();
		if (DestroyProcessor.class.isAssignableFrom(c)) {
			if (log.isDebugEnabled()) {
				log.info("Destroy bag of [{}]", c);
			}

			((DestroyProcessor) type.getInstance()).destroy();

			if (log.isDebugEnabled()) {
				log.info("Complete destroy bag [{}]", c);
			}
		}
	}

	/**
	 * Function of invoke class method annotated {@link PostConstruct}.
	 *
	 * @param values types for invoke postConstructions
	 */
	private void initPostConstruction(Collection<TypeMetadata> values) {
		for (TypeMetadata type : values) {
			final Class<?> toCheck = type.getType();
			final Method[] methods = toCheck.getDeclaredMethods();
			for (Method method : methods) {
				if (method.isAnnotationPresent(PostConstruct.class)) {
					try {
						method.invoke(type.getInstance());
					} catch (IllegalAccessException | InvocationTargetException e) {
						throw new IoCInstantiateException(e);
					}

					break;
				}
			}
		}
	}

	/**
	 * Function of invoke class method annotated {@link PostConstruct}.
	 *
	 * @param o bag for invoke postConstruction
	 */
	private void initPostConstruction(Object o) {
		final Class<?> toCheck = o.getClass();
		final Method[] methods = toCheck.getDeclaredMethods();
		for (Method method : methods) {
			if (method.isAnnotationPresent(PostConstruct.class)) {
				try {
					method.invoke(o);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new IoCInstantiateException();
				}

				break;
			}
		}
	}

	/**
	 * Function for found aspects.
	 *
	 * @param packages packages to scan
	 * @return aspect class
	 */
	private List<Class<?>> extractAspect(String... packages) {
		return new LinkedList<>(findAspects(packages));
	}

	/**
	 * Initializing processors in contexts.
	 *
	 * @param typeProcessors found processors in the classpath
	 */
	private void registerTypeProcessor(List<TypeProcessor> typeProcessors) {
		for (TypeProcessor typeProcessor : typeProcessors) {
			instantiateSensibles(typeProcessor);
			this.typeProcessors.add(typeProcessor);
		}
	}

	/**
	 * Inject some information to instance if find inheritance of {@link Sensible}.
	 *
	 * @param instance instance of component
	 */
	public void instantiateSensibles(Object instance) {
		final Class<?> cls = instance.getClass();
		if (ContextSensible.class.isAssignableFrom(cls)) {
			((ContextSensible) instance).contextInform(this);
		}

		if (ThreadFactorySensible.class.isAssignableFrom(cls)) {
			((ThreadFactorySensible) instance).factoryInform(getType(Factory.defaultThreadFactory()));
		}

		if (CacheFactorySensible.class.isAssignableFrom(cls)) {
			((CacheFactorySensible) instance).factoryInform(getType(Factory.defaultCacheFactory()));
		}

		if (EnvironmentSensible.class.isAssignableFrom(cls)) {
			final Class<?> type = instance.getClass();
			final Class<?> genericSuperclass = (Class<?>) type.getGenericSuperclass();
			if (genericSuperclass != Object.class) {
				final Type[] genericInterfaces = genericSuperclass.getGenericInterfaces();
				findParametrizedType((EnvironmentSensible) instance, genericInterfaces);
			} else {
				final Type[] genericInterfaces = type.getGenericInterfaces();
				findParametrizedType((EnvironmentSensible) instance, genericInterfaces);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void findParametrizedType(EnvironmentSensible instance, Type[] genericInterfaces) {
		for (Type t : genericInterfaces) {
			if (t instanceof ParameterizedType) {
				final ParameterizedType parameterizedType = (ParameterizedType) t;
				if (EnvironmentSensible.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
					final Class<?> param = (Class) parameterizedType.getActualTypeArguments()[0];
					final Object env = getType(param);
					if (env != null) {
						instance.environmentInform(env);
						break;
					}
				}
			}
		}
	}

	/**
	 * Function of find aspect agent for types.
	 *
	 * @param models registered aspect agents
	 */
	private Object viewAspect(Object instance, List<Class<?>> models) {
		final Class<?> type = instance.getClass();
		for (Method m : type.getDeclaredMethods()) {
			if (checkTypeForIntercept(m, models) && !type.isAnnotationPresent(Lazy.class)) {
				return DynamicProxy.newProxyInstance(this, instance, models);
			}
		}

		return instance;
	}

	/**
	 * Function of calling user settings of bag before it is directly initialized.
	 *
	 * @param typeName bag name
	 * @param o        instantiated bag
	 * @return modified bag
	 */
	private Object processBeforeInitialization(String typeName, Object o) {
		Object result = o;
		for (TypeProcessor processor : typeProcessors) {
			result = processor.beforeComponentInitialization(typeName, o);
		}

		if (result != null) {
			return result;
		}

		return o;
	}

	/**
	 * Function of calling user settings of bag after it is directly initialized.
	 *
	 * @param typeName bag name
	 * @param o        instantiated bag
	 * @return modified bag
	 */
	private Object processAfterInitialization(String typeName, Object o) {
		Object result = o;
		for (TypeProcessor processor : typeProcessors) {
			result = processor.afterComponentInitialization(typeName, o);
		}

		if (result != null) {
			return result;
		}

		return o;
	}

	public SingletonFactory getSingletonFactory() {
		return singletonFactory;
	}

	public PrototypeFactory getPrototypeFactory() {
		return prototypeFactory;
	}
}
