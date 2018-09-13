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
package org.di.test.components.lazy;

import org.di.context.annotations.IoCComponent;
import org.di.context.annotations.IoCDependency;
import org.di.context.annotations.Lazy;
import org.di.test.environments.ExampleEnvironment;

/**
 * @author GenCloud
 * @date 12.09.2018
 */
@Lazy
@IoCComponent
public class LazyComponentA {
    @IoCDependency
    private ExampleEnvironment exampleEnvironment;

    @Override
    public String toString() {
        return "LazyComponentA{hash: " + Integer.toHexString(hashCode()) +
                ", exampleEnvironment=" + exampleEnvironment +
                '}';
    }
}