package org.ioc.context.model.cache.expiring.model;


public class RemovalNotify<K, V> {
	private final K key;
	private final V value;
	private final RemovalReason removalReason;

	public RemovalNotify(K key, V value, RemovalReason removalReason) {
		this.key = key;
		this.value = value;
		this.removalReason = removalReason;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public RemovalReason getRemovalReason() {
		return removalReason;
	}

	public enum RemovalReason {REPLACED, INVALIDATED, EVICTED}
}
