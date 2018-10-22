/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
 *
 * IoC Starter Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IoC Starter Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IoC Starter Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.context;

import org.ioc.annotations.configuration.Property;
import org.ioc.annotations.configuration.PropertyFunction;
import org.ioc.annotations.context.*;
import org.ioc.annotations.modules.DatabaseModule;
import org.ioc.annotations.modules.WebModule;
import org.ioc.aop.DynamicProxy;
import org.ioc.context.factories.Factory;
import org.ioc.context.factories.ICacheFactory;
import org.ioc.context.factories.core.InstanceFactory;
import org.ioc.context.factories.core.PrototypeFactory;
import org.ioc.context.factories.core.SingletonFactory;
import org.ioc.context.factories.facts.FactDispatcherFactory;
import org.ioc.context.factories.web.RequestFactory;
import org.ioc.context.listeners.IListener;
import org.ioc.context.listeners.facts.OnContextDestroyFact;
import org.ioc.context.listeners.facts.OnTypeInitFact;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.processors.DestroyProcessor;
import org.ioc.context.processors.TypeProcessor;
import org.ioc.context.processors.impl.ThreadConfigureProcessor;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.context.sensible.Sensible;
import org.ioc.context.sensible.factories.CacheFactorySensible;
import org.ioc.context.sensible.factories.DatabaseFactorySensible;
import org.ioc.context.sensible.factories.ThreadFactorySensible;
import org.ioc.context.type.IoCContext;
import org.ioc.enviroment.configurations.CacheAutoConfiguration;
import org.ioc.enviroment.configurations.FactDispatcherAutoConfiguration;
import org.ioc.enviroment.configurations.ThreadingAutoConfiguration;
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
import java.util.concurrent.atomic.AtomicReference;
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

	private final AtomicReference<SingletonFactory> singletonFactory = new AtomicReference<>();
	private final AtomicReference<PrototypeFactory> prototypeFactory = new AtomicReference<>();
	private final AtomicReference<RequestFactory> requestFactory = new AtomicReference<>();

	private List<Class<?>> aspects;

	private FactDispatcherFactory dispatcherFactory;

	private String[] packages;

	@Override
	public String[] getPackages() {
		return packages;
	}

	/**
	 * Main function of initialization {@link IoCContext}.
	 *
	 * @param mainSource class starter
	 * @param packages   packages to scan
	 */
	public void init(Class<?> mainSource, String... packages) {
		this.packages = packages;

		final List<TypeMetadata> types = findMetadataInClassPath(packages)
				.stream()
				.filter(t -> !t.getType().isAnnotationPresent(Lazy.class))
				.collect(Collectors.toList());

		final List<TypeMetadata> lazyTypes = findMetadataInClassPath(packages)
				.stream()
				.filter(t -> t.getType().isAnnotationPresent(Lazy.class) && t.getMode() == Mode.SINGLETON)
				.collect(Collectors.toList());

		lazyTypes.forEach(getSingletonFactory()::addType);

		final List<TypeProcessor> processors = findInstancesInClassPathByInstance(TypeProcessor.class, packages);

		List<Class<?>> configurations = findClassesByAnnotation(Property.class, packages)
				.stream()
				.filter(c -> !c.getAnnotation(Property.class).ignore())
				.collect(Collectors.toList());

		aspects = extractAspect(packages);

		configurations.add(CacheAutoConfiguration.class);
		configurations.add(ThreadingAutoConfiguration.class);
		configurations.add(FactDispatcherAutoConfiguration.class);

		processors.add((TypeProcessor) instantiateClass(ThreadConfigureProcessor.class));

		if (mainSource.isAnnotationPresent(DatabaseModule.class)) {
			final DatabaseModule annotation = mainSource.getAnnotation(DatabaseModule.class);
			configurations.add(annotation.autoConfigurationClass());
		}

		if (mainSource.isAnnotationPresent(WebModule.class)) {
			final WebModule annotation = mainSource.getAnnotation(WebModule.class);
			configurations.add(annotation.autoConfigurationClass());
		}

		if (mainSource.isAnnotationPresent(Exclude.class)) {
			final Class<?>[] excluded = mainSource.getAnnotation(Exclude.class).excludedConfigurations();
			configurations = configurations.stream().filter(c -> !Arrays.asList(excluded).contains(c)).collect(Collectors.toList());
		}

		configurations.sort((o1, o2) -> {
			final int order_1 = getOrder(o1);
			final int order_2 = getOrder(o2);
			return order_2 - order_1;
		});

		registerEnvironments(configurations);

		registerTypeProcessor(processors);

		initFactories();

		registerListeners(packages);

		registerTypes(types);

		initPostConstruction(getSingletonFactory().getTypes().values());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O> O getType(String name) {
		Object type = getPrototypeFactory().getType(name);
		if (type == null) {
			type = getRequestFactory().getType(name);
			if (type == null) {
				type = getSingletonFactory().getType(name);
			} else {
				instantiateSensibles(type);
			}
		}

		return (O) type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O> O getType(Class<O> type) {
		Object o = getType(type.getSimpleName());
		if (o == null) {
			o = getPrototypeFactory().getType(type);
			if (o == null) {
				o = getRequestFactory().getType(type);
				if (o == null) {
					o = getSingletonFactory().getType(type);
				} else {
					instantiateSensibles(o);
				}
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
			getSingletonFactory().addType(metadata);
		} else if (mode == Mode.REQUEST) {
			getRequestFactory().addType(metadata);
		} else {
			getPrototypeFactory().addType(metadata);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<TypeMetadata> getMetadatas(Mode mode) {
		if (mode == Mode.SINGLETON) {
			return getSingletonFactory().getTypes().values();
		} else if (mode == Mode.PROTOTYPE) {
			return getPrototypeFactory().getTypes().values();
		}

		return getRequestFactory().getTypes().values();
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
				getPrototypeFactory().addType(type);
			} else if (type.getMode() == Mode.REQUEST) {
				getRequestFactory().addType(type);
			} else {
				getSingletonFactory().addType(type);
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

			getSingletonFactory().addType(type);
		});

		return types;
	}

	@Override
	public void destroy() {
		getDispatcherFactory().fireEvent(new OnContextDestroyFact(this));

		getSingletonFactory().getTypes().values().forEach(this::destroy);
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

	private void registerListeners(String... packages) {
		final List<IListener> listeners = findInstancesInClassPathByInstance(IListener.class, packages);
		listeners.forEach(l -> {
			instantiateSensibles(l);
			getDispatcherFactory().addListener(l);
		});
	}

	private void initFactories() {
		final List<TypeMetadata> metadatas = getSingletonFactory().getTypes()
				.values()
				.stream()
				.filter(t -> Factory.class.isAssignableFrom(t.getType()))
				.filter(t -> !t.isInitialized())
				.sorted((o1, o2) -> {
					final int order_1 = getOrder(o1.getType());
					final int order_2 = getOrder(o2.getType());
					return order_2 - order_1;
				}).collect(Collectors.toList());

		metadatas.forEach(this::installFactory);
	}

	@SuppressWarnings("unchecked")
	private void installFactory(TypeMetadata metadata) {
		((Factory) metadata.getInstance()).initialize();
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

	/**
	 * Function of invoke class method annotated {@link PostConstruct}.
	 *
	 * @param values types for invoke postConstructions
	 */
	private void initPostConstruction(Collection<TypeMetadata> values) {
		List<TypeMetadata> toSort = new ArrayList<>(values);
		toSort.sort((o1, o2) -> {
			final int order_1 = getOrder(o1.getType());
			final int order_2 = getOrder(o2.getType());
			return order_2 - order_1;
		});

		for (TypeMetadata type : toSort) {
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
			getSingletonFactory().getTypes().values()
					.stream()
					.filter(t -> Factory.defaultThreadFactory().isAssignableFrom(t.getType()))
					.findFirst()
					.ifPresent(t -> {
						if (!t.isInitialized()) {
							t.setInitialized(true);
							installFactory(t);
							((ThreadFactorySensible) instance).factoryInform((Factory) t.getInstance());
						} else {
							((ThreadFactorySensible) instance).factoryInform((Factory) t.getInstance());
						}
					});
		}

		if (CacheFactorySensible.class.isAssignableFrom(cls)) {
			getSingletonFactory().getTypes().values()
					.stream()
					.filter(t -> ICacheFactory.class.isAssignableFrom(t.getType()))
					.findFirst()
					.ifPresent(t -> {
						if (!t.isInitialized()) {
							t.setInitialized(true);
							installFactory(t);
							((CacheFactorySensible) instance).factoryInform((Factory) t.getInstance());
						} else {
							((CacheFactorySensible) instance).factoryInform((Factory) t.getInstance());
						}
					});
		}

		if (DatabaseFactorySensible.class.isAssignableFrom(cls)) {
			getSingletonFactory().getTypes().values()
					.stream()
					.filter(t -> Factory.defaultDatabaseFactory().isAssignableFrom(t.getType()))
					.findFirst()
					.ifPresent(t -> {
						if (!t.isInitialized()) {
							t.setInitialized(true);
							installFactory(t);
							((DatabaseFactorySensible) instance).factoryInform((Factory) t.getInstance());
						} else {
							((DatabaseFactorySensible) instance).factoryInform((Factory) t.getInstance());
						}
					});
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

	@Override
	public RequestFactory getRequestFactory() {
		RequestFactory requestFactory = this.requestFactory.get();
		if (requestFactory == null) {
			requestFactory = new RequestFactory(instanceFactory);
			this.requestFactory.set(requestFactory);
		}
		return requestFactory;
	}

	@Override
	public SingletonFactory getSingletonFactory() {
		SingletonFactory singletonFactory = this.singletonFactory.get();
		if (singletonFactory == null) {
			singletonFactory = new SingletonFactory(instanceFactory);
			this.singletonFactory.set(singletonFactory);
		}
		return singletonFactory;
	}

	@Override
	public PrototypeFactory getPrototypeFactory() {
		PrototypeFactory prototypeFactory = this.prototypeFactory.get();
		if (prototypeFactory == null) {
			prototypeFactory = new PrototypeFactory(instanceFactory);
			this.prototypeFactory.set(prototypeFactory);
		}
		return prototypeFactory;
	}
}
