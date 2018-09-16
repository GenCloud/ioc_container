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
package org.di.context.enviroment.configurations;

import org.di.context.annotations.property.Property;

import java.util.concurrent.TimeUnit;

import static java.lang.Thread.*;

/**
 * @author GenCloud
 * @date 13.09.2018
 */
@Property(prefix = "ioc.threads.")
public class ThreadingConfiguration {
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


    public static enum ThreadPoolPriority {
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
