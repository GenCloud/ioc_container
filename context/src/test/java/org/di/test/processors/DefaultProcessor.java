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
package org.di.test.processors;

import org.di.factories.config.ComponentProcessor;
import org.di.test.components.ComponentD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 13.09.2018
 */
public class DefaultProcessor implements ComponentProcessor {
    private final Logger log = LoggerFactory.getLogger(DefaultProcessor.class);

    @Override
    public Object afterComponentInitialization(String componentName, Object component) {
        if (component instanceof ComponentD) {
            log.info("Sample changing ComponentD type after initialization");
        }
        return component;
    }

    @Override
    public Object beforeComponentInitialization(String componentName, Object component) {
        if (component instanceof ComponentD) {
            log.info("Sample changing ComponentD type before initialization");
        }
        return component;
    }
}
