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
package org.ioc.test.threading;

import org.ioc.annotations.context.IoCComponent;
import org.ioc.annotations.context.PostConstruct;
import org.ioc.context.factories.Factory;
import org.ioc.context.sensible.factories.ThreadFactorySensible;
import org.ioc.exceptions.IoCException;
import org.ioc.threads.annotation.SimpleTask;
import org.ioc.threads.factory.DefaultThreadPoolFactory;
import org.ioc.threads.factory.model.AbstractTask;
import org.ioc.threads.factory.model.interfaces.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author GenCloud
 * @date 09/2018
 */
@IoCComponent
public class ComponentThreads implements ThreadFactorySensible {
	private final Logger log = LoggerFactory.getLogger(AbstractTask.class);
	private final AtomicInteger atomicInteger = new AtomicInteger(0);

	private DefaultThreadPoolFactory threadPoolFactory;

	@PostConstruct
	public void init() {
		threadPoolFactory.async((Task<Void>) () -> {
			log.info("Start test thread!");
			return null;
		});
	}

	@Override
	public void factoryInform(Factory threadPoolFactory) throws IoCException {
		this.threadPoolFactory = (DefaultThreadPoolFactory) threadPoolFactory;
	}

	@SimpleTask(startingDelay = 1, fixedInterval = 5)
	public void schedule() {
		log.info("I'm Big Daddy, scheduling and incrementing param - [{}]", atomicInteger.incrementAndGet());
	}
}
