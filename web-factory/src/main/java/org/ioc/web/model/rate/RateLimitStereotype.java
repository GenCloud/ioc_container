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
package org.ioc.web.model.rate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class holding method calls information
 *
 * @author GenCloud
 * @date 10/2018
 */
public class RateLimitStereotype {
	private final int limit;
	private final long mills;

	private final List<Long> callTimestamps = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

	public RateLimitStereotype(TimeUnit timeUnit, int limit) {
		this.limit = limit;
		mills = timeUnit.toMillis(1);
	}

	public boolean check() {
		lock.readLock().lock();
		try {
			if (callTimestamps.size() >= limit) {
				return false;
			}
		} finally {
			lock.readLock().unlock();
		}

		lock.writeLock().lock();
		try {
			if (callTimestamps.size() < limit) {
				callTimestamps.add(System.currentTimeMillis());
				return true;
			} else {
				return false;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void removeOldest() {
		final long threshold = System.currentTimeMillis() - mills;
		lock.writeLock().lock();
		try {
			callTimestamps.removeIf(it -> it < threshold);
		} finally {
			lock.writeLock().unlock();
		}
	}
}
