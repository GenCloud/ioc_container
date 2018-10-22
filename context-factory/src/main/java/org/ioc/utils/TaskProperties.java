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
package org.ioc.utils;

import java.util.concurrent.TimeUnit;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class TaskProperties {
    private long startingDelay;
    private long fixedInterval;
    private TimeUnit timeUnit;

    public TaskProperties(long startingDelay, long fixedInterval, TimeUnit timeUnit) {
        this.startingDelay = startingDelay;
        this.fixedInterval = fixedInterval;
        this.timeUnit = timeUnit;
    }

    public long getStartingDelay() {
        return startingDelay;
    }

    public void setStartingDelay(long startingDelay) {
        this.startingDelay = startingDelay;
    }

    public long getFixedInterval() {
        return fixedInterval;
    }

    public void setFixedInterval(long fixedInterval) {
        this.fixedInterval = fixedInterval;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
