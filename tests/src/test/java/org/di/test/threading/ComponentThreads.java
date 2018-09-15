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
package org.di.test.threading;

import org.di.context.annotations.IoCComponent;
import org.di.context.contexts.sensibles.ThreadFactorySensible;
import org.di.context.excepton.IoCException;
import org.di.context.factories.config.Factory;
import org.di.threads.annotation.SimpleTask;
import org.di.threads.factory.DefaultThreadingFactory;
import org.di.threads.factory.model.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author GenCloud
 * @date 14.09.2018
 */
@IoCComponent
public class ComponentThreads implements ThreadFactorySensible {
    private final Logger log = LoggerFactory.getLogger(AbstractTask.class);
    private final AtomicInteger atomicInteger = new AtomicInteger(0);
    private DefaultThreadingFactory defaultThreadingFactory;

    @PostConstruct
    public void init() {
        defaultThreadingFactory.async(new AbstractTask<Void>() {
            @Override
            public Void call() {
                log.info("Start test thread!");
                return null;
            }
        });
    }

    @Override
    public void factoryInform(Factory defaultThreadingFactory) throws IoCException {
        this.defaultThreadingFactory = (DefaultThreadingFactory) defaultThreadingFactory;
    }

    @SimpleTask(startingDelay = 1, fixedInterval = 5)
    public void schedule() {
        log.info("I'm Big Daddy, scheduling and incrementing param - [{}]", atomicInteger.incrementAndGet());
    }
}
