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
package org.ioc.enviroment.configurations;

import org.ioc.annotations.configuration.Property;
import org.ioc.annotations.configuration.PropertyFunction;
import org.ioc.context.factories.Factory;
import org.ioc.utils.ReflectionUtils;

import java.util.concurrent.TimeUnit;

import static java.lang.Thread.*;
import static org.ioc.context.factories.Factory.defaultThreadFactory;

/**
 * @author GenCloud
 * @date 09/2018
 */
@Property(prefix = "ioc.threads.")
public class ThreadingAutoConfiguration {
	private String poolName;

	private int availableProcessors;

	private long threadTimeout;

	private boolean threadAllowCoreTimeOut;

	private ThreadPoolPriority threadPoolPriority;

	@Property(ignore = true)
	private TimeUnit threadTimeoutUnit = TimeUnit.SECONDS;

	public String getPoolName() {
		return poolName;
	}

	public String getDefaultExecutor() {
		return "java.util.concurrent.ScheduledThreadPoolExecutor";
	}

	public int getAvailableProcessors() {
		return availableProcessors;
	}

	public long getThreadTimeout() {
		return threadTimeout;
	}

	public boolean isThreadAllowCoreTimeOut() {
		return threadAllowCoreTimeOut;
	}

	public ThreadPoolPriority getThreadPoolPriority() {
		return threadPoolPriority;
	}

	public TimeUnit getThreadTimeoutUnit() {
		return threadTimeoutUnit;
	}

	@PropertyFunction
	public Object threadingFactory() {
		final Class<? extends Factory> factory = defaultThreadFactory();
		return ReflectionUtils.instantiateClass(factory);
	}

	public enum ThreadPoolPriority {
		/**
		 * High priority.
		 * <p>
		 * Processor will block {@link ThreadPoolPriority#NORMAL} and {@link ThreadPoolPriority#LOW} priority
		 * threads in order to finish tasks in pools on this priority.
		 */
		HIGH(MAX_PRIORITY),
		/**
		 * Normal priority.
		 * <p>
		 * Processor will block {@link ThreadPoolPriority#LOW} priority threads in order to finish tasks
		 * in pools on this priority.
		 */
		NORMAL(NORM_PRIORITY),
		/**
		 * Low priority.
		 * <p>
		 * Processor will give very low priority for tasks in this level.
		 */
		LOW(MIN_PRIORITY);

		private final int threadPriority;

		ThreadPoolPriority(int threadPriority) {
			this.threadPriority = threadPriority;
		}

		public int get() {
			return threadPriority;
		}
	}
}
