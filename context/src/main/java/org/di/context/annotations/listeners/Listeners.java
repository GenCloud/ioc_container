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
package org.di.context.annotations.listeners;

import org.di.context.listeners.Listener;

import java.lang.annotation.*;

/**
 * <p>Indicates to the contexts which packages should be scanned to identify listeners
 * and initializeComponents them</p>
 * <p>If no package is specified, the package of the class marked with this annotation will be scanned</p>
 *
 * @author GenCloud
 * @date 04.09.2018
 * TODO (implement listener manager)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Listeners {
    /**
     * @return a string array of application packages, that will be scanned
     */
    String[] packages() default {};

    /**
     * @return an array of class-listeners
     */
    Class<? extends Listener>[] classes() default {};
}
