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
package org.di.context.factories.config;

/**
 * @author GenCloud
 * @date 12.09.2018
 */
public interface ComponentProcessor {
    /**
     * Processing not installed bean before initialization in factory.
     *
     * @param componentName type name
     * @param component     instantiated type
     * @return modified type
     */
    Object afterComponentInitialization(String componentName, Object component);

    /**
     * Processing installed bean after initialization in factory.
     *
     * @param componentName type name
     * @param component     instantiated type
     * @return modified type
     */
    Object beforeComponentInitialization(String componentName, Object component);
}
