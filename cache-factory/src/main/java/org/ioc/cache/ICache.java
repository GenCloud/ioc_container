package org.ioc.cache;

/**
 * Interface represents a Map structure for cache usage.
 *
 * @author GenCloud
 * @date 09/2018
 */
public interface ICache<K, V> extends Iterable<V> {
	/**
	 * Add pair <K, V> to cache.
	 * <p>
	 * Notice: if there is already a value with given id in map,
	 * {@link IllegalArgumentException} will be thrown.
	 *
	 * @param key   key name
	 * @param value cache content value
	 */
	void put(K key, V value);

	/**
	 * Returns cached value correlated to given key.
	 *
	 * @param key key
	 * @return cached value for this key
	 */
	V get(K key);

	/**
	 * Checks whether this map contains a value related to given key.
	 *
	 * @param key key
	 * @return true if key has an value
	 */
	boolean contains(K key);

	/**
	 * Removes an entry from map, that has given key.
	 *
	 * @param key key
	 */
	void remove(K key);

	/**
	 * Clears cache.
	 */
	void clear();

	/**
	 * @return size of cache map
	 */
	int size();

	default void forEach() {

	}
}
