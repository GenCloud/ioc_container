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
package org.ioc.context.model.cache.expiring;

import org.ioc.context.model.cache.ExpiringFacade;
import org.ioc.utils.Assertion;

import java.util.function.ToLongBiFunction;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class ExpiringBuilder<K, V> {
	private long maximumWeight = -1;
	private long expireAfterAccessNanos = -1;
	private long expireAfterWriteNanos = -1;
	private ToLongBiFunction<K, V> weigher;
	private RemovalFact<K, V> removalFact;

	public ExpiringBuilder<K, V> setExpireAfterAccessNanos(long expireAfterAccessNanos) {
		Assertion.checkArgument(expireAfterAccessNanos >= 0, "expireAfterAccessNanos <= 0");
		this.expireAfterAccessNanos = expireAfterAccessNanos;
		return this;
	}

	public ExpiringBuilder<K, V> setExpireAfterWriteNanos(long expireAfterWriteNanos) {
		Assertion.checkArgument(expireAfterWriteNanos >= 0, "expireAfterWriteNanos <= 0");
		this.expireAfterWriteNanos = expireAfterWriteNanos;
		return this;
	}

	public ExpiringBuilder<K, V> setMaximumWeight(long maximumWeight) {
		Assertion.checkArgument(maximumWeight > 0, "maximumWeight < 0");
		this.maximumWeight = maximumWeight;
		return this;
	}

	public ExpiringBuilder<K, V> setWeigher(ToLongBiFunction<K, V> weigher) {
		Assertion.checkNotNull(weigher);
		this.weigher = weigher;
		return this;
	}

	public ExpiringBuilder<K, V> setRemovalFact(RemovalFact<K, V> removalFact) {
		Assertion.checkNotNull(removalFact);
		this.removalFact = removalFact;
		return this;
	}

	public ExpiringFacade<K, V> build() {
		final ExpiringFacade<K, V> cache = new ExpiringFacade<>();
		if (maximumWeight != -1) {
			cache.setMaximumWeight(maximumWeight);
		}

		if (expireAfterAccessNanos != -1) {
			cache.setExpireAfterAccessNanos(expireAfterAccessNanos);
		}

		if (expireAfterWriteNanos != -1) {
			cache.setExpireAfterWriteNanos(expireAfterWriteNanos);
		}

		if (weigher != null) {
			cache.setWeigher(weigher);
		}

		if (removalFact != null) {
			cache.setRemovalFact(removalFact);
		}
		return cache;
	}
}
