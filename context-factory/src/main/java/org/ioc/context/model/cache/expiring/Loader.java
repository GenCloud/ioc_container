package org.ioc.context.model.cache.expiring;

@FunctionalInterface
public interface Loader<K, V> {
	V load(K key) throws Exception;
}
