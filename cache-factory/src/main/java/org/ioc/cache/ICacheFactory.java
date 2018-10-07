package org.ioc.cache;

import org.ioc.cache.annotations.CacheIgnore;
import org.ioc.cache.annotations.Cacheables;
import org.ioc.context.factories.Factory;
import org.ioc.context.processors.DestroyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

/**
 * This is an transparent Cache system. It proxies an interface implementing
 * {@link Cacheables}. Once first call is done through proxy, result
 * is cached in underlying cache engine. When second and sucedind calls
 * are made cache is looked up, if a match (method and arguments pair) is
 * found, this result is returned.
 * <p>
 * If you do not desire to cache an method, annotate it with {@link CacheIgnore}
 *
 * @author GenCloud
 * @date 09/2018
 */
public interface ICacheFactory extends Factory, DestroyProcessor {
	Logger log = LoggerFactory.getLogger(ICacheFactory.class);

	/**
	 * Invoke instance with cache. Note that interfaceType must be an interface!
	 *
	 * @param <T>           instance collection
	 * @param interfaceType interface collection. Remember, this must be an interface!
	 * @param instance      instance implementing interface
	 * @return cache-invoked object
	 */
	<T> T invoke(Class<T> interfaceType, T instance);

	/**
	 * Creates a new cache with default configurations. Eviction mode is LRU
	 * (Last Recently Used). The size is only a guarantee that you can store
	 * at least n items.
	 *
	 * @param <K>  cache key collection
	 * @param <V>  cache value collection
	 * @param name cache name
	 * @param size maximum cache size
	 * @return created cache
	 */
	<K, V> ICache<K, V> install(String name, int size);

	/**
	 * Creates a new eternal cache with default configurations. An eternal cache
	 * is guaranteed to never automatically expire items. The size is only a
	 * guarantee that you can store at least n items.
	 *
	 * @param <K>  cache key collection
	 * @param <V>  cache value collection
	 * @param name cache name
	 * @param size maximum cache size
	 * @return created cache
	 */
	<K, V> ICache<K, V> installEternal(String name, int size);

	/**
	 * Install a new cache with default configurations. The default cache size
	 * is 1000. The size is only a guarantee that you can store at least
	 * 1000 items.
	 *
	 * @param <K>  cache key collection
	 * @param <V>  cache value collection
	 * @param name cache name
	 * @return created cache
	 */
	<K, V> ICache<K, V> install(String name);

	/**
	 * Disposes cache. Once cache is disposed it cannot be used anymore.
	 *
	 * @param <K>    cache key collection
	 * @param <V>    cache value collection
	 * @param ICache cache
	 */
	<K, V> void invalidate(ICache<K, V> ICache);

	/**
	 * @param interfaceICache installed collection cache
	 * @param interfaceType   interface collection. Remember, this must be an interface!
	 * @param instance        instance implementing interface
	 * @param <T>             instance collection
	 * @return cache-invoked object
	 * @see ICacheFactory#invoke(Class, Object)
	 */
	@SuppressWarnings("unchecked")
	default <T> T invoke(ICache<MethodInvocation, Object> interfaceICache, Class<T> interfaceType, T instance) {
		if (log.isDebugEnabled()) {
			log.debug("Decorating {} with cache", interfaceType);
		}

		if (interfaceType.isAnnotationPresent(Cacheables.class)) {
			return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{interfaceType},
					((proxy, method, args) -> {
						if (method.isAnnotationPresent(CacheIgnore.class)) {
							return method.invoke(instance, args);
						}

						final MethodInvocation invocation = new MethodInvocation(method, args);
						Object result = interfaceICache.get(invocation);
						if (result == null) {
							result = method.invoke(instance, args);
							interfaceICache.put(invocation, result);
						}
						return result;
					}));
		}

		return null;
	}
}