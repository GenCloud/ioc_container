package org.ioc.context.model.cache.expiring;

import org.ioc.context.model.cache.expiring.model.RemovalNotify;

@FunctionalInterface
public interface RemovalFact<K, V> {
	void onRemoval(RemovalNotify<K, V> notification);
}
