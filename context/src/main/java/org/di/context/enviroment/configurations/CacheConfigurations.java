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
import org.di.context.factories.config.Factory;

import static org.di.context.factories.config.Factory.defaultCacheFactory;

/**
 * @author GenCloud
 * @date 16.09.2018
 */
@Property(prefix = "cache.")
public class CacheConfigurations {
    private Class<? extends Factory> factoryClass = defaultCacheFactory();

    public Class<? extends Factory> getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class<? extends Factory> factoryClass) {
        this.factoryClass = factoryClass;
    }
}